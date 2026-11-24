package com.example.data.ai

import com.example.data.local.AccountEntity
import com.example.data.model.AppCurrency
import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.ExpenseCategory
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.TransactionType
import com.example.data.model.VoiceParsedTransaction
import com.example.localization.AppLanguage
import java.util.Locale

/**
 * Deterministic Bangla + English + Banglish natural-language transaction parser.
 * Serves as an instant on-device engine/fallback whenever cloud AI is unavailable,
 * accurately classifying Income vs Expense vs Transfer, Food & Dining vs Groceries,
 * multi-currency units (KD/KWD দিনার, BDT টাকা, SAR রিয়াল, AED দিরহাম, EUR, GBP, USD, INR),
 * and source (From) / destination (To) accounts across domestic and cross-border transfers.
 */
object LocalVoiceTransactionParser {

    private data class AccountMention(
        val label: String,
        val matchedAccount: AccountEntity?,
        val startIndex: Int,
        val endIndex: Int
    )

    private data class RegionCurrencyHint(
        val regionName: String,
        val currencyCode: String,
        val tokens: List<String>,
        val preferredBankTokens: List<String> = emptyList()
    )

    private val knownProviderTokens: List<Pair<String, List<String>>> = listOf(
        "Trust Bank" to listOf("trust bank", "trust", "ট্রাস্ট ব্যাংক", "ট্রাস্ট", "tap"),
        "Dutch-Bangla Bank" to listOf(
            "dutch-bangla", "dutch bangla", "dutchbangla", "dbbl", "nexuspay",
            "ডাচ-বাংলা", "ডাচ বাংলা", "ডাচবাংলা", "ডিবিবিএল"
        ),
        "City Bank" to listOf("city bank", "citytouch", "city", "সিটি ব্যাংক", "সিটি"),
        "BRAC Bank" to listOf("brac bank", "brac", "astha", "ব্র্যাক ব্যাংক", "ব্র্যাক"),
        "Islami Bank" to listOf("islami bank", "ibbl", "cellfin", "ইসলামী ব্যাংক", "ইসলামি ব্যাংক", "ইসলামী"),
        "Sonali Bank" to listOf("sonali bank", "sonali", "সোনালী ব্যাংক", "সোনালী"),
        "Eastern Bank" to listOf("eastern bank", "ebl", "ইস্টার্ন ব্যাংক", "ইবিএল"),
        "Prime Bank" to listOf("prime bank", "প্রাইম ব্যাংক"),
        "Pubali Bank" to listOf("pubali bank", "pubali", "পূবালী ব্যাংক", "পূবালী"),
        "UCB" to listOf("united commercial bank", "ucb", "ইউসিবি"),
        "Mutual Trust Bank" to listOf("mutual trust bank", "mutual trust", "mtb", "মিউচুয়াল ট্রাস্ট", "মিউচুয়াল ট্রাস্ট"),
        "Bank Asia" to listOf("bank asia", "ব্যাংক এশিয়া", "ব্যাংক এশিয়া"),
        "Dhaka Bank" to listOf("dhaka bank", "ঢাকা ব্যাংক"),
        "AB Bank" to listOf("ab bank", "এবি ব্যাংক"),
        "Al-Arafah Bank" to listOf("al-arafah", "al arafah", "আল আরাফাহ", "আল-আরাফাহ"),
        "bKash" to listOf("bkash", "b-kash", "bikash", "বিকাশ", "বিকাশে", "বিকাশের"),
        "Nagad" to listOf("nagad", "নগদ", "নগদে"),
        "Rocket" to listOf("rocket", "রকেট", "রকেটে"),
        "Upay" to listOf("upay", "উপায়", "উপায়", "উপায়ে", "উপায়ে"),
        "ABK" to listOf("al ahli bank of kuwait", "al ahli kuwait", "ahli bank", "abk", "এবিকে", "আল আহলি"),
        "KFH" to listOf("kuwait finance house", "kfh", "কেএফএইচ", "কুয়েত ফিন্যান্স", "কুয়েত ফিন্যান্স"),
        "NBK" to listOf("national bank of kuwait", "nbk", "এনবিকে", "ন্যাশনাল ব্যাংক অফ কুয়েত"),
        "Gulf Bank" to listOf("gulf bank", "গালফ ব্যাংক"),
        "Boubyan" to listOf("boubyan bank", "boubyan", "বুবিয়ান", "বুবিয়ান"),
        "Burgan Bank" to listOf("burgan bank", "burgan", "বুরগান"),
        "Al Tijari" to listOf("al tijari", "commercial bank of kuwait", "cbk", "আল তিজারি"),
        "Warba Bank" to listOf("warba bank", "warba", "ওয়ারবা", "ওয়ারবা"),
        "KIB" to listOf("kuwait international bank", "kib"),
        "Al Rajhi" to listOf("al rajhi", "alrajhi", "rajhi", "আল রাজি"),
        "SNB" to listOf("saudi national bank", "snb", "alahli", "সৌদি ন্যাশনাল"),
        "stc pay" to listOf("stc pay", "stcpay", "stc"),
        "ENBD" to listOf("emirates nbd", "enbd", "এমিরেটস"),
        "FAB" to listOf("first abu dhabi", "fab"),
        "QNB" to listOf("qatar national bank", "qnb"),
        "Maybank" to listOf("maybank", "মেব্যাংক"),
        "Wise" to listOf("transferwise", "wise", "ওয়াইজ", "ওয়াইজ"),
        "Revolut" to listOf("revolut", "রেভোলুট"),
        "Cash" to listOf("cash", "নগদ টাকা", "ক্যাশ", "পকেট")
    )

    private val knownRegions: List<RegionCurrencyHint> = listOf(
        RegionCurrencyHint(
            regionName = "Kuwait",
            currencyCode = "KWD",
            tokens = listOf("kuwait", "কুয়েত", "কুয়েত", "কুয়েতে", "কুয়েতে", "কুয়েতের", "কুয়েতের"),
            preferredBankTokens = listOf("kuwait", "abk", "nbk", "kfh", "gulf", "boubyan", "burgan", "tijari", "warba", "kib", "কুয়েত", "কুয়েত")
        ),
        RegionCurrencyHint(
            regionName = "Bangladesh",
            currencyCode = "BDT",
            tokens = listOf("bangladesh", "বাংলাদেশ", "বাংলাদেশে", "বাংলাদেশের", "দেশে", "বাড়িতে", "বাড়িতে"),
            preferredBankTokens = listOf("bangladesh", "dbbl", "dutch", "trust", "city", "brac", "islami", "sonali", "bkash", "nagad", "rocket", "upay", "বাংলাদেশ")
        ),
        RegionCurrencyHint(
            regionName = "Saudi Arabia",
            currencyCode = "SAR",
            tokens = listOf("saudi arabia", "saudi", "ksa", "riyadh", "jeddah", "সৌদি", "সৌদিতে", "সৌদির", "রিয়াদ", "রিয়াদ"),
            preferredBankTokens = listOf("saudi", "rajhi", "snb", "alahli", "riyad", "alinma", "stc")
        ),
        RegionCurrencyHint(
            regionName = "UAE",
            currencyCode = "AED",
            tokens = listOf("uae", "dubai", "abu dhabi", "emirates", "দুবাই", "দুবাইতে", "আমিরাত", "আবুধাবি"),
            preferredBankTokens = listOf("dubai", "enbd", "fab", "dib", "mashreq", "adcb", "uae")
        ),
        RegionCurrencyHint(
            regionName = "Qatar",
            currencyCode = "QAR",
            tokens = listOf("qatar", "doha", "কাতার", "কাতারে", "দোহা"),
            preferredBankTokens = listOf("qatar", "qnb", "cbq", "dukhan", "masraf")
        ),
        RegionCurrencyHint(
            regionName = "Oman",
            currencyCode = "OMR",
            tokens = listOf("oman", "muscat", "ওমান", "ওমানে"),
            preferredBankTokens = listOf("oman", "muscat", "dhofar")
        ),
        RegionCurrencyHint(
            regionName = "Bahrain",
            currencyCode = "BHD",
            tokens = listOf("bahrain", "বাহরাইন", "বাহরাইনে"),
            preferredBankTokens = listOf("bahrain", "nbb", "bbk")
        ),
        RegionCurrencyHint(
            regionName = "Malaysia",
            currencyCode = "MYR",
            tokens = listOf("malaysia", "মালয়েশিয়া", "মালয়েশিয়া", "মালয়েশিয়ায়"),
            preferredBankTokens = listOf("malaysia", "maybank", "cimb", "public bank", "rhb")
        ),
        RegionCurrencyHint(
            regionName = "Singapore",
            currencyCode = "SGD",
            tokens = listOf("singapore", "সিঙ্গাপুর", "সিঙ্গাপুরে"),
            preferredBankTokens = listOf("singapore", "dbs", "ocbc", "uob")
        ),
        RegionCurrencyHint(
            regionName = "India",
            currencyCode = "INR",
            tokens = listOf("india", "ভারত", "ভারতে", "ভারতের", "কলকাতা"),
            preferredBankTokens = listOf("india", "sbi", "hdfc", "icici", "axis")
        ),
        RegionCurrencyHint(
            regionName = "UK",
            currencyCode = "GBP",
            tokens = listOf("uk", "united kingdom", "england", "london", "যুক্তরাজ্য", "লন্ডন"),
            preferredBankTokens = listOf("barclays", "hsbc", "monzo", "lloyds", "natwest")
        ),
        RegionCurrencyHint(
            regionName = "USA",
            currencyCode = "USD",
            tokens = listOf("usa", "united states", "america", "আমেরিকা", "যুক্তরাষ্ট্র"),
            preferredBankTokens = listOf("chase", "bank of america", "citi", "wells fargo")
        ),
        RegionCurrencyHint(
            regionName = "Europe",
            currencyCode = "EUR",
            tokens = listOf("italy", "europe", "germany", "france", "spain", "portugal", "ইতালি", "ইতালিতে", "ইউরোপ"),
            preferredBankTokens = listOf("revolut", "wise", "intesa", "unicredit", "n26", "deutsche")
        )
    )

    private val explicitTransferVerbs = listOf(
        "transfer", "transferred", "remit", "remitted", "send to", "sent to", "move to", "moved to",
        "ট্রান্সফার", "স্থানান্তর", "পাঠিয়েছি", "পাঠিয়েছি", "পাঠালাম", "পাঠানো", "পাঠাইছি", "পাঠাইলাম",
        "সরালাম", "ট্রান্সফার করেছি", "পাঠানো হয়েছে", "পাঠানো হয়েছে",
        "pathiyechi", "pathalam", "pathaichi", "pathano", "->"
    )

    private val strongIncomeKeywords = listOf(
        // English & Banglish incoming money indicators
        "income", "salary", "received", "receive", "credited", "cash in", "cash-in", "cashin",
        "add money", "deposit", "deposited", "bonus", "remittance", "refund", "cashback",
        "freelance", "got paid", "paid me", "earned", "dividend", "profit", "incoming",
        "beton", "peyechi", "pelam", "dhukeche", "dhukse", "dhuklo", "joma hoyeche", "joma holo", "joma", "eseche",
        // Bangla incoming money indicators
        "ঢুকেছে", "ঢুকছে", "ঢুকলো", "ঢুকল", "টাকা ঢুকেছে",
        "পেয়েছি", "পেয়েছি", "পেলাম", "পাওয়া গেছে", "পাওয়া গেছে", "টাকা পেয়েছি", "টাকা পেয়েছি",
        "জমা হয়েছে", "জমা হয়েছে", "জমা হলো", "জমা হল", "জমা পড়েছে", "জমা পড়েছে", "জমা",
        "এসেছে", "আসছে", "টাকা এসেছে", "আয়", "আয়", "আয় হয়েছে", "আয় হয়েছে",
        "বেতন", "বোনাস", "রেমিট্যান্স", "ক্যাশ ইন", "ক্যাশ-ইন", "অ্যাড মানি", "এড মানি",
        "রিফান্ড", "ক্যাশব্যাক", "ডিপোজিট", "লাভ", "ক্রেডিট", "রিসিভ"
    )

    private val strongExpenseActionKeywords = listOf(
        "spent", "spend", "paid", "bought", "buy", "expense", "cost", "purchase", "purchased",
        "cash out", "cash-out", "cashout", "debited", "withdraw", "withdrew", "ate", "eating",
        "pay bill", "paid bill", "payment",
        "খরচ", "কিনেছি", "কিনলাম", "দিয়েছি", "দিয়েছি", "দিলাম", "কেটেছে", "কেটে নিয়েছে", "কেটে নিয়েছে",
        "ক্যাশ আউট", "ক্যাশ-আউট", "পেমেন্ট করেছি", "বিল দিয়েছি", "বিল দিয়েছি",
        "খেয়েছি", "খেয়েছি", "খেলাম", "খাইছি", "খাইলাম", "উত্তোলন",
        "khoroch", "kinechi", "kinlam", "kheyechi", "khelam", "dilam", "diyechi"
    )

    private val diningAndMealKeywords = listOf(
        // English & Banglish meals / eating out / food items
        "food", "meal", "meals", "lunch", "dinner", "breakfast", "brunch", "supper",
        "dining", "dine", "ate", "eating", "eat", "restaurant", "cafe", "coffee", "tea",
        "snack", "snacks", "biryani", "kacchi", "tehari", "khichuri", "burger", "pizza",
        "sandwich", "shawarma", "kebab", "kfc", "mcdonald", "dominos", "foodpanda", "talabat", "deliveroo",
        "nasta", "khabar", "kheyechi", "khelam", "bhat", "paratha", "juice", "ice cream", "dessert",
        "fast food", "takeaway", "takeout", "fried chicken", "noodles", "pasta",
        // Bangla meals / eating out / food items
        "খাবার", "খাওয়া", "খাওয়া", "খেয়েছি", "খেয়েছি", "খেলাম", "খাইছি", "খাইলাম",
        "নাস্তা", "সকালের নাস্তা", "দুপুরের খাবার", "রাতের খাবার", "দুপুরে", "ভাত", "রুটি", "পরোটা",
        "খিচুড়ি", "খিচুড়ি", "তেহারি", "কাচ্চি", "বিরিয়ানি", "বিরিয়ানি", "পোলাও", "কাবাব",
        "শর্মা", "শাওয়ারমা", "শাওয়ারমা", "বার্গার", "পিৎজা", "পিজ্জা", "স্যান্ডউইচ", "ফাস্টফুড",
        "হোটেল", "হোটেলে", "রেস্টুরেন্ট", "রেস্টুরেন্টে", "রেস্তোরাঁ", "ক্যাফে", "কফি",
        "চা-নাস্তা", "চা নাস্তা", "এক কাপ চা", "সিঙ্গারা", "সমুচা", "মিষ্টি", "আইসক্রিম", "জুস",
        "লাঞ্চ", "ডিনার", "ব্রেকফাস্ট", "স্ন্যাকস", "ফুডপান্ডা", "ফুড", "ডাইনিং"
    )

    private val mealConsumptionKeywords = listOf(
        "lunch", "dinner", "breakfast", "brunch", "supper", "restaurant", "cafe", "ate", "eating", "dining",
        "biryani", "kacchi", "burger", "pizza", "shawarma", "coffee", "foodpanda", "talabat",
        "খেয়েছি", "খেয়েছি", "খেলাম", "খাইছি", "খাইলাম", "হোটেল", "হোটেলে", "রেস্টুরেন্ট", "রেস্টুরেন্টে",
        "ক্যাফে", "নাস্তা", "লাঞ্চ", "ডিনার", "ব্রেকফাস্ট", "দুপুরের খাবার", "রাতের খাবার",
        "বিরিয়ানি", "বিরিয়ানি", "কাচ্চি", "বার্গার", "পিৎজা", "পিজ্জা", "কফি"
    )

    private val strongGroceryKeywords = listOf(
        "grocery", "groceries", "grocer", "supermarket", "super shop", "supershop", "hypermarket",
        "shwapno", "agora", "meena bazar", "unimart", "chaldal", "lulu", "carrefour", "sultan center",
        "bazar", "bazaar", "kacha bazar", "raw market",
        "কাঁচাবাজার", "কাঁচা বাজার", "বাজার খরচ", "বাজার করেছি", "বাজার করলাম", "বাজার",
        "গ্রোসারি", "মুদি", "মুদিখানা", "সুপারশপ", "হাইপারমার্কেট",
        "স্বপ্ন", "আগোরা", "মীনা বাজার", "চালডাল"
    )

    private val rawGroceryItemKeywords = listOf(
        "vegetables", "vegetable", "veggies", "fruits", "raw fish", "raw meat", "lentils", "spices", "flour", "rice", "milk", "eggs", "market",
        "সবজি", "শাকসবজি", "তরকারি", "মাছ", "মাংস", "চাল", "ডাল", "আটা", "ময়দা", "ময়দা",
        "চিনি", "লবণ", "পেঁয়াজ", "পেঁয়াজ", "রসুন", "আদা", "মসলা", "ডিম", "দুধ"
    )

    private val standaloneBanglaTeaRegex = Regex("(?:^|[\\s,।\\-+()\"'])চা(?:$|[\\s,।\\-+()\"'র])")

    fun parse(
        utterance: String,
        accounts: List<AccountEntity>,
        language: AppLanguage,
        defaultCurrencyCode: String = "BDT",
        managedCategories: List<ManagedCategoryItem> = BuiltInCategoriesCatalog.all
    ): VoiceParsedTransaction {
        val raw = utterance.trim()
        val ascii = ReceiptVoucherMatcher.toAsciiDigits(raw)
        val lower = ascii.lowercase(Locale.US)

        // 1. Extract amount (supports decimals like 12.500, commas like 5,000, and k/hajar shorthand)
        val amount = extractAmount(ascii)

        // 2. Detect explicit currency first (e.g., দিনার / dinar / KD / KWD -> KWD)
        val explicitCurrency = detectExplicitCurrency(raw, lower)

        // 3. Detect all account/bank/wallet mentions in order of appearance (strict matching—never unrelated fallback)
        val mentions = findAccountMentions(lower, accounts)

        // 4. Detect directional transfer clauses & regional hints (e.g. "কুয়েত থেকে বাংলাদেশের ডাচ বাংলা একাউন্টে")
        val directionalSplit = splitDirectionalClauses(lower)
        val allRegions = findRegionMentions(lower)

        // 5. Classify transaction type: TRANSFER vs INCOME vs EXPENSE
        val hasTransferVerb = explicitTransferVerbs.any { matchesKeyword(lower, it) }
        val hasFromToPattern = directionalSplit != null ||
            Regex("(?:from|theke|থেকে|হতে).+(?:to|te|তে|একাউন্টে|অ্যাকাউন্টে|ব্যাংকে|ওয়ালেটে|ওয়ালেটে|->)").containsMatchIn(lower)

        val hasTwoDistinctAccounts = mentions.size >= 2 &&
            mentions[0].label != mentions[1].label &&
            (mentions[0].matchedAccount == null || mentions[1].matchedAccount == null ||
                mentions[0].matchedAccount?.id != mentions[1].matchedAccount?.id)

        // Strip "got paid" / "paid me" before evaluating expense "paid" so income isn't misread as expense
        val lowerForExpenseCheck = lower
            .replace("got paid", " ")
            .replace("paid me", " ")

        val hasStandaloneCredit = matchesKeyword(lower, "credit") &&
            !lower.contains("credit card") &&
            !lower.contains("credit limit")

        val hasStrongIncome = hasStandaloneCredit ||
            strongIncomeKeywords.any { matchesKeyword(lower, it) }
        val hasStrongExpenseAction = strongExpenseActionKeywords.any { matchesKeyword(lowerForExpenseCheck, it) }
        val hasThirdPersonIncomingToMe = (lower.contains("আমাকে") || lower.contains("আমার")) &&
            (lower.contains("পাঠিয়েছে") || lower.contains("পাঠিয়েছে") || lower.contains("দিয়েছে") || lower.contains("দিয়েছে"))

        val isCrossRegionOrAccountTransfer = hasTransferVerb &&
            (mentions.isNotEmpty() || allRegions.isNotEmpty() || hasFromToPattern)

        val type = when {
            (hasStrongIncome || hasThirdPersonIncomingToMe) && !hasTransferVerb && !hasTwoDistinctAccounts && !hasStrongExpenseAction ->
                TransactionType.INCOME
            hasTwoDistinctAccounts && (hasTransferVerb || hasFromToPattern || !hasStrongExpenseAction) ->
                TransactionType.TRANSFER
            isCrossRegionOrAccountTransfer ->
                TransactionType.TRANSFER
            hasStrongIncome && !hasStrongExpenseAction ->
                TransactionType.INCOME
            hasStrongIncome && !hasTransferVerb && !lower.contains("খরচ") && !lower.contains("ক্যাশ আউট") && !lower.contains("cash out") ->
                TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 6. Resolve source (From) & destination (To) accounts and hints accurately
        var fromHint = ""
        var toHint = ""
        var matchedSourceAccount: AccountEntity? = null
        var inferredSourceRegionCurrency: String? = null

        if (type == TransactionType.TRANSFER) {
            val resolved = resolveTransferEndpoints(
                lower = lower,
                mentions = mentions,
                accounts = accounts,
                directionalSplit = directionalSplit,
                explicitCurrency = explicitCurrency
            )
            fromHint = resolved.fromHint
            toHint = resolved.toHint
            matchedSourceAccount = resolved.fromAccount
            inferredSourceRegionCurrency = resolved.sourceRegionCurrency
        } else if (mentions.isNotEmpty()) {
            val primaryMention = mentions.first()
            matchedSourceAccount = primaryMention.matchedAccount
                ?: matchAccountByHint(
                    accounts = accounts,
                    hint = primaryMention.label,
                    preferredCurrency = explicitCurrency
                )
            fromHint = matchedSourceAccount?.name ?: primaryMention.label
        } else if (allRegions.isNotEmpty()) {
            val region = allRegions.first()
            inferredSourceRegionCurrency = region.currencyCode
            matchedSourceAccount = matchAccountByRegion(accounts, region)
            fromHint = matchedSourceAccount?.name ?: region.regionName
        }

        // 7. Resolve currency: explicit currency unit (দিনার/KD/KWD) > source region currency > matched source account > default
        val resolvedCurrency = explicitCurrency
            ?: inferredSourceRegionCurrency
            ?: matchedSourceAccount?.currencyCode?.takeIf { it.isNotBlank() }
            ?: AppCurrency.fromCode(defaultCurrencyCode).code

        // If TRANSFER still has blank fromHint, try resolving source account from resolvedCurrency excluding destination
        if (type == TransactionType.TRANSFER && matchedSourceAccount == null && fromHint.isBlank()) {
            val excludedToId = matchAccountByHint(accounts, toHint)?.id
            val currencySource = accounts.firstOrNull {
                it.id != excludedToId && it.currencyCode.equals(resolvedCurrency, ignoreCase = true)
            }
            if (currencySource != null) {
                matchedSourceAccount = currencySource
                fromHint = currencySource.name
            }
        }

        // 8. Infer category accurately using unified detector (12 Expense, 5 Income, 3 Transfer + custom user categories)
        val detectedCategory = UnifiedCategoryDetector.detectCategory(
            text = raw,
            type = type,
            managedCategories = managedCategories,
            language = language
        )
        val category = detectedCategory.legacyCategory
        val managedCategory = detectedCategory.managedCategory
        val managedLabel = managedCategory.label(language)

        // 9. Build concise title
        val title = buildTitle(
            raw = raw,
            type = type,
            category = category,
            managedCategoryId = managedCategory.id,
            managedCategoryLabel = managedLabel,
            fromHint = fromHint,
            toHint = toHint,
            language = language
        )

        return VoiceParsedTransaction(
            title = title,
            amountBdt = amount,
            type = type,
            category = category,
            accountHint = fromHint,
            currencyCode = resolvedCurrency,
            toAccountHint = toHint,
            note = raw.take(160),
            isLocalFallback = true,
            managedCategoryId = managedCategory.id,
            managedCategoryLabel = managedLabel
        )
    }

    fun matchAccountByHint(
        accounts: List<AccountEntity>,
        hint: String,
        preferredCurrency: String? = null,
        excludeAccountId: Long? = null
    ): AccountEntity? {
        val candidates = accounts.filter { excludeAccountId == null || it.id != excludeAccountId }
        if (candidates.isEmpty()) return null

        val cleanHint = ReceiptVoucherMatcher.toAsciiDigits(hint).trim().lowercase(Locale.US)
        if (cleanHint.isBlank()) return null

        // 1. Direct name or bank/provider substring match
        val directMatches = candidates.filter { acc ->
            val name = ReceiptVoucherMatcher.toAsciiDigits(acc.name).lowercase(Locale.US).trim()
            val bank = ReceiptVoucherMatcher.toAsciiDigits(acc.bankOrProvider).lowercase(Locale.US).trim()
            (name.length >= 2 && (name.contains(cleanHint) || cleanHint.contains(name))) ||
                (bank.length >= 2 && (bank.contains(cleanHint) || cleanHint.contains(bank)))
        }
        if (directMatches.isNotEmpty()) {
            return preferredCurrency?.takeIf { it.isNotBlank() }?.let { cur ->
                directMatches.firstOrNull { it.currencyCode.equals(cur, ignoreCase = true) }
            } ?: directMatches.first()
        }

        // 2. Canonical bank/MFS alias match via ReceiptVoucherMatcher (strict provider match — NO unrelated currency fallback)
        val canonical = knownProviderTokens.firstOrNull { (name, aliases) ->
            cleanHint.contains(name.lowercase(Locale.US)) ||
                aliases.any { matchesKeyword(cleanHint, it) }
        }?.first ?: hint

        val matched = ReceiptVoucherMatcher.matchAccount(
            accounts = candidates,
            providerHint = canonical,
            currencyCode = preferredCurrency.orEmpty(),
            allowCurrencyFallback = false
        )
        if (matched.isAutoMatched && matched.account != null) {
            return matched.account
        }

        // 3. Country/region hint match (e.g. "Kuwait" / "কুয়েত" -> user's Kuwait / KWD account)
        val matchedRegion = knownRegions.firstOrNull { region ->
            region.regionName.equals(cleanHint, ignoreCase = true) ||
                region.currencyCode.equals(cleanHint, ignoreCase = true) ||
                region.tokens.any { matchesKeyword(cleanHint, it) }
        }
        if (matchedRegion != null) {
            return matchAccountByRegion(candidates, matchedRegion)
        }

        return null
    }

    private data class DirectionalSplit(
        val sourceClause: String,
        val destClause: String
    )

    private data class ResolvedTransferEndpoints(
        val fromAccount: AccountEntity?,
        val toAccount: AccountEntity?,
        val fromHint: String,
        val toHint: String,
        val sourceRegionCurrency: String?
    )

    private fun splitDirectionalClauses(lower: String): DirectionalSplit? {
        // 1. Bangla "থেকে" / "হতে" / "theke": text before is source, text after is destination
        val banglaFromMarkers = listOf(" থেকে ", " থেকে", "থেকে ", " হতে ", " theke ")
        for (marker in banglaFromMarkers) {
            val idx = lower.indexOf(marker)
            if (idx > 0) {
                val before = lower.substring(0, idx).trim()
                val after = lower.substring(idx + marker.length).trim()
                if (before.isNotBlank() && after.isNotBlank()) {
                    return DirectionalSplit(sourceClause = before, destClause = after)
                }
            }
        }

        // 2. Arrow "->"
        val arrowIdx = lower.indexOf("->")
        if (arrowIdx > 0) {
            val before = lower.substring(0, arrowIdx).trim()
            val after = lower.substring(arrowIdx + 2).trim()
            if (before.isNotBlank() && after.isNotBlank()) {
                return DirectionalSplit(sourceClause = before, destClause = after)
            }
        }

        // 3. English "from X to Y" or "to Y from X"
        val fromMatch = Regex("(?<![a-z])from(?![a-z])").find(lower)
        val toMatch = Regex("(?<![a-z])to(?![a-z])").find(lower)
        if (fromMatch != null && toMatch != null) {
            return if (fromMatch.range.first < toMatch.range.first) {
                DirectionalSplit(
                    sourceClause = lower.substring(fromMatch.range.last + 1, toMatch.range.first).trim(),
                    destClause = lower.substring(toMatch.range.last + 1).trim()
                )
            } else {
                DirectionalSplit(
                    sourceClause = lower.substring(fromMatch.range.last + 1).trim(),
                    destClause = lower.substring(toMatch.range.last + 1, fromMatch.range.first).trim()
                )
            }
        }
        if (toMatch != null && toMatch.range.first > 0) {
            val before = lower.substring(0, toMatch.range.first).trim()
            val after = lower.substring(toMatch.range.last + 1).trim()
            if (before.isNotBlank() && after.isNotBlank()) {
                return DirectionalSplit(sourceClause = before, destClause = after)
            }
        }

        return null
    }

    private fun resolveTransferEndpoints(
        lower: String,
        mentions: List<AccountMention>,
        accounts: List<AccountEntity>,
        directionalSplit: DirectionalSplit?,
        explicitCurrency: String?
    ): ResolvedTransferEndpoints {
        var fromAccount: AccountEntity? = null
        var toAccount: AccountEntity? = null
        var fromHint = ""
        var toHint = ""
        var sourceRegionCurrency: String? = null

        if (directionalSplit != null) {
            val srcMentions = findAccountMentions(directionalSplit.sourceClause, accounts)
            val dstMentions = findAccountMentions(directionalSplit.destClause, accounts)
            val srcRegions = findRegionMentions(directionalSplit.sourceClause)
            val dstRegions = findRegionMentions(directionalSplit.destClause)

            // Resolve destination first if explicitly named in destClause (e.g. "ডাচ বাংলা একাউন্টে")
            if (dstMentions.isNotEmpty()) {
                val m = dstMentions.first()
                toAccount = m.matchedAccount
                toHint = m.matchedAccount?.name ?: m.label
            }

            // Resolve source from sourceClause: explicit bank/wallet > source region/country > explicit currency
            if (srcMentions.isNotEmpty()) {
                val m = srcMentions.firstOrNull { it.matchedAccount?.id != toAccount?.id } ?: srcMentions.first()
                fromAccount = m.matchedAccount?.takeIf { it.id != toAccount?.id }
                fromHint = fromAccount?.name ?: m.label
            } else if (srcRegions.isNotEmpty()) {
                val region = srcRegions.first()
                sourceRegionCurrency = region.currencyCode
                fromAccount = matchAccountByRegion(
                    accounts = accounts.filter { it.id != toAccount?.id },
                    region = region
                )
                fromHint = fromAccount?.name ?: region.regionName
            }

            // If destination was not named by bank, resolve by destination region (e.g., "বাংলাদেশে")
            if (toAccount == null && toHint.isBlank() && dstRegions.isNotEmpty()) {
                val region = dstRegions.first()
                toAccount = matchAccountByRegion(
                    accounts = accounts.filter { it.id != fromAccount?.id },
                    region = region
                )
                toHint = toAccount?.name ?: region.regionName
            }

            // If source is still empty but explicit currency exists (e.g., KD / দিনার), match matching-currency wallet
            if (fromAccount == null && fromHint.isBlank() && explicitCurrency != null) {
                fromAccount = accounts.firstOrNull {
                    it.id != toAccount?.id && it.currencyCode.equals(explicitCurrency, ignoreCase = true)
                }
                if (fromAccount != null) {
                    fromHint = fromAccount.name
                }
            }

            return ResolvedTransferEndpoints(
                fromAccount = fromAccount,
                toAccount = toAccount,
                fromHint = fromHint,
                toHint = toHint,
                sourceRegionCurrency = sourceRegionCurrency
            )
        }

        // No explicit splitter — check 2+ mentions or single destination-marked mention
        if (mentions.size >= 2) {
            val ordered = orderTransferMentions(lower, mentions)
            return ResolvedTransferEndpoints(
                fromAccount = ordered.first.matchedAccount,
                toAccount = ordered.second.matchedAccount,
                fromHint = ordered.first.matchedAccount?.name ?: ordered.first.label,
                toHint = ordered.second.matchedAccount?.name ?: ordered.second.label,
                sourceRegionCurrency = null
            )
        }

        if (mentions.size == 1) {
            val only = mentions.first()
            val afterMention = lower.substring(only.endIndex.coerceAtMost(lower.length)).trimStart()
            val isMarkedDestination = Regex(
                "^(?:একাউন্টে|অ্যাকাউন্টে|ব্যাংকে|ওয়ালেটে|ওয়ালেটে|তে|এ|য়|য়|কে)(?:$|\\s|[.,।])"
            ).containsMatchIn(afterMention) ||
                lower.substring(0, only.startIndex).trimEnd().endsWith("to")

            if (isMarkedDestination) {
                toAccount = only.matchedAccount
                toHint = only.matchedAccount?.name ?: only.label
                val regionsBefore = findRegionMentions(lower.substring(0, only.startIndex))
                if (regionsBefore.isNotEmpty()) {
                    val region = regionsBefore.first()
                    sourceRegionCurrency = region.currencyCode
                    fromAccount = matchAccountByRegion(accounts.filter { it.id != toAccount?.id }, region)
                    fromHint = fromAccount?.name ?: region.regionName
                } else if (explicitCurrency != null) {
                    fromAccount = accounts.firstOrNull {
                        it.id != toAccount?.id && it.currencyCode.equals(explicitCurrency, ignoreCase = true)
                    }
                    fromHint = fromAccount?.name.orEmpty()
                }
            } else {
                fromAccount = only.matchedAccount
                fromHint = only.matchedAccount?.name ?: only.label
            }
        }

        return ResolvedTransferEndpoints(
            fromAccount = fromAccount,
            toAccount = toAccount,
            fromHint = fromHint,
            toHint = toHint,
            sourceRegionCurrency = sourceRegionCurrency
        )
    }

    private fun findRegionMentions(text: String): List<RegionCurrencyHint> {
        val hits = mutableListOf<Pair<Int, RegionCurrencyHint>>()
        for (region in knownRegions) {
            var bestIdx = Int.MAX_VALUE
            for (tok in region.tokens) {
                val idx = indexOfToken(text, tok)
                if (idx >= 0 && idx < bestIdx) bestIdx = idx
            }
            if (bestIdx != Int.MAX_VALUE) {
                hits += bestIdx to region
            }
        }
        return hits.sortedBy { it.first }.map { it.second }
    }

    private fun matchAccountByRegion(
        accounts: List<AccountEntity>,
        region: RegionCurrencyHint
    ): AccountEntity? {
        if (accounts.isEmpty()) return null
        val currencyAccounts = accounts.filter { it.currencyCode.equals(region.currencyCode, ignoreCase = true) }

        // 1. Prefer account whose currency matches AND name/bank mentions the region or one of its major banks
        val bankAndCurrencyMatch = currencyAccounts.firstOrNull { acc ->
            val searchable = "${acc.name} ${acc.bankOrProvider}".lowercase(Locale.US)
            region.preferredBankTokens.any { tok -> matchesKeyword(searchable, tok) }
        }
        if (bankAndCurrencyMatch != null) return bankAndCurrencyMatch

        // 2. Any account in the region's currency (e.g., any KWD card for Kuwait)
        if (currencyAccounts.isNotEmpty()) return currencyAccounts.first()

        // 3. Account whose name/bank explicitly mentions the region name
        return accounts.firstOrNull { acc ->
            val searchable = "${acc.name} ${acc.bankOrProvider}".lowercase(Locale.US)
            region.tokens.any { tok -> matchesKeyword(searchable, tok) }
        }
    }

    private fun extractAmount(ascii: String): Double {
        // Check shorthand like "5k" or "2.5k"
        val kMatch = Regex("(?<![a-z0-9])(\\d+(?:\\.\\d+)?)\\s*k(?![a-z0-9])", RegexOption.IGNORE_CASE)
            .find(ascii)
        if (kMatch != null) {
            val base = kMatch.groupValues[1].toDoubleOrNull()
            if (base != null && base > 0.0) return base * 1000.0
        }

        // Check Bangla "হাজার" (thousand) e.g. "৫ হাজার" or "5 hajar"
        val hajarMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:হাজার|hajar|hazar|thousand)", RegexOption.IGNORE_CASE)
            .find(ascii)
        if (hajarMatch != null) {
            val base = hajarMatch.groupValues[1].toDoubleOrNull()
            if (base != null && base > 0.0) return base * 1000.0
        }

        // General numbers (ignoring 4-digit card last4 preceded by **** or xxxx)
        val cleaned = ascii.replace(Regex("[*xX•]{2,}\\s*\\d{4}"), " ")
        val numberRegex = Regex("(\\d{1,3}(?:,\\d{2,3})+(?:\\.\\d{1,3})?|\\d+(?:\\.\\d{1,3})?)")
        val numbers = numberRegex.findAll(cleaned)
            .mapNotNull { m ->
                val rawNum = m.value.replace(",", "")
                rawNum.toDoubleOrNull()?.takeIf { it > 0.0 && it < 1_000_000_000.0 }
            }
            .toList()

        return numbers.firstOrNull() ?: 0.0
    }

    private fun detectExplicitCurrency(raw: String, lower: String): String? {
        return when {
            Regex("(?<![a-z])(kwd|kd)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("dinar") || raw.contains("দিনার") || raw.contains("কেডি") || raw.contains("কে.ডি") -> "KWD"
            Regex("(?<![a-z])(sar|sr)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("riyal") || raw.contains("রিয়াল") || raw.contains("রিয়াল") -> "SAR"
            Regex("(?<![a-z])(aed|dhs|dh)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("dirham") || raw.contains("দিরহাম") -> "AED"
            Regex("(?<![a-z])(qar|qr)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("qatari") -> "QAR"
            Regex("(?<![a-z])omr(?![a-z])").containsMatchIn(lower) ||
                lower.contains("omani") -> "OMR"
            Regex("(?<![a-z])bhd(?![a-z])").containsMatchIn(lower) ||
                lower.contains("bahraini") -> "BHD"
            Regex("(?<![a-z])(myr|rm)(?![a-z])").containsMatchIn(lower) ||
                lower.contains("ringgit") || raw.contains("রিংগিত") -> "MYR"
            Regex("(?<![a-z])sgd(?![a-z])").containsMatchIn(lower) -> "SGD"
            raw.contains("€") || Regex("(?<![a-z])eur(?![a-z])").containsMatchIn(lower) ||
                lower.contains("euro") || raw.contains("ইউরো") -> "EUR"
            raw.contains("£") || Regex("(?<![a-z])gbp(?![a-z])").containsMatchIn(lower) ||
                lower.contains("pound") || raw.contains("পাউন্ড") -> "GBP"
            raw.contains("$") || Regex("(?<![a-z])usd(?![a-z])").containsMatchIn(lower) ||
                lower.contains("dollar") || raw.contains("ডলার") -> "USD"
            raw.contains("₹") || Regex("(?<![a-z])inr(?![a-z])").containsMatchIn(lower) ||
                lower.contains("rupee") || raw.contains("রুপি") -> "INR"
            raw.contains("৳") || Regex("(?<![a-z])(bdt|tk|taka)(?![a-z])").containsMatchIn(lower) ||
                raw.contains("টাকা") -> "BDT"
            else -> null
        }
    }

    private fun findAccountMentions(lower: String, accounts: List<AccountEntity>): List<AccountMention> {
        val found = mutableListOf<AccountMention>()

        // 1. Direct matches against the user's own account names & banks/providers
        for (acc in accounts) {
            val candidates = listOf(acc.name, acc.bankOrProvider)
                .map { ReceiptVoucherMatcher.toAsciiDigits(it).lowercase(Locale.US).trim() }
                .filter { it.length >= 3 }
            var bestIdx = Int.MAX_VALUE
            var bestEnd = 0
            for (cand in candidates) {
                val idx = indexOfToken(lower, cand)
                if (idx >= 0 && idx < bestIdx) {
                    bestIdx = idx
                    bestEnd = idx + cand.length
                }
            }
            if (bestIdx != Int.MAX_VALUE) {
                found += AccountMention(
                    label = acc.name,
                    matchedAccount = acc,
                    startIndex = bestIdx,
                    endIndex = bestEnd
                )
            }
        }

        // 2. Canonical bank/MFS alias matches mapped to user's accounts strictly (NO unrelated BDT fallback)
        for ((canonical, aliases) in knownProviderTokens) {
            var bestIdx = Int.MAX_VALUE
            var bestEnd = 0
            for (alias in aliases) {
                val idx = indexOfToken(lower, alias)
                if (idx >= 0 && idx < bestIdx) {
                    bestIdx = idx
                    bestEnd = idx + alias.length
                }
            }
            if (bestIdx != Int.MAX_VALUE) {
                val alreadyMatchedAtPos = found.any { kotlin.math.abs(it.startIndex - bestIdx) <= 3 }
                if (!alreadyMatchedAtPos) {
                    val matched = ReceiptVoucherMatcher.matchAccount(
                        accounts = accounts,
                        providerHint = canonical,
                        currencyCode = "",
                        allowCurrencyFallback = false
                    ).takeIf { it.isAutoMatched }?.account
                    if (matched == null || found.none { it.matchedAccount?.id == matched.id }) {
                        found += AccountMention(
                            label = matched?.name ?: canonical,
                            matchedAccount = matched,
                            startIndex = bestIdx,
                            endIndex = bestEnd
                        )
                    }
                }
            }
        }

        return found.sortedBy { it.startIndex }
    }

    private fun orderTransferMentions(
        lower: String,
        mentions: List<AccountMention>
    ): Pair<AccountMention, AccountMention> {
        val first = mentions[0]
        val second = mentions[1]
        // Handle reverse phrasing like "to DBBL from Trust"
        val toIdx = listOf(" to ", "->").map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull()
        val fromIdx = listOf(" from ").map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull()
        if (toIdx != null && fromIdx != null && toIdx < fromIdx) {
            return second to first
        }
        return first to second
    }

    private fun matchesKeyword(text: String, token: String): Boolean = indexOfToken(text, token) >= 0

    private fun indexOfToken(text: String, token: String): Int {
        val isPureAscii = token.all { it.code < 128 }
        return if (isPureAscii) {
            val match = Regex("(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9])").find(text)
            match?.range?.first ?: -1
        } else {
            text.indexOf(token)
        }
    }

    private fun buildTitle(
        raw: String,
        type: TransactionType,
        category: ExpenseCategory,
        managedCategoryId: String,
        managedCategoryLabel: String,
        fromHint: String,
        toHint: String,
        language: AppLanguage
    ): String {
        val isBn = language.isBangla
        if (type == TransactionType.TRANSFER) {
            return when {
                fromHint.isNotBlank() && toHint.isNotBlank() -> "$fromHint → $toHint"
                toHint.isNotBlank() -> if (isBn) "$toHint-এ ট্রান্সফার" else "Transfer to $toHint"
                else -> if (isBn) "অ্যাকাউন্ট ট্রান্সফার" else "Account Transfer"
            }
        }
        val isGenericOther = managedCategoryId == "exp_other" || managedCategoryId == "inc_other"
        if (!isGenericOther && managedCategoryLabel.isNotBlank()) {
            return if (fromHint.isNotBlank() && (raw.lowercase(Locale.US).contains("cash out") || raw.contains("ক্যাশ আউট"))) {
                if (isBn) "$fromHint ক্যাশ আউট ($managedCategoryLabel)" else "$fromHint Cash Out ($managedCategoryLabel)"
            } else {
                managedCategoryLabel
            }
        }
        if (category != ExpenseCategory.OTHER) {
            val baseLabel = category.label(language)
            return if (fromHint.isNotBlank() && (raw.lowercase(Locale.US).contains("cash out") || raw.contains("ক্যাশ আউট"))) {
                if (isBn) "$fromHint ক্যাশ আউট ($baseLabel)" else "$fromHint Cash Out ($baseLabel)"
            } else {
                baseLabel
            }
        }
        val cleaned = raw
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(36)
        return cleaned.ifBlank { if (isBn) "দ্রুত এন্ট্রি" else "Quick Entry" }
    }
}
