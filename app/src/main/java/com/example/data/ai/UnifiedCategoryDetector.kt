package com.example.data.ai

import com.example.data.model.BuiltInCategoriesCatalog
import com.example.data.model.ExpenseCategory
import com.example.data.model.ManagedCategoryItem
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import java.util.Locale

data class DetectedCategoryMatch(
    val managedCategory: ManagedCategoryItem,
    val legacyCategory: ExpenseCategory,
    val isExactCustomMatch: Boolean = false
)

/**
 * Unified, single-source-of-truth category & transaction-type detection engine shared across
 * Voice Entry, Receipt / Voucher Scan, and Plus (+) Quick / Daily Entry.
 *
 * - Supports all 5 expanded Income categories and all 12 expanded Expense categories
 * - Supports user-created custom categories instantaneously (matched before built-ins)
 * - Keeps Voice & Receipt fully decoupled from direct Firebase calls
 */
object UnifiedCategoryDetector {

    private val standaloneBanglaTeaRegex = Regex("(?:^|[\\s,।\\-+()\"'])চা(?:$|[\\s,।\\-+()\"'র])")

    private val diningAndMealKeywords = listOf(
        "food", "meal", "meals", "lunch", "dinner", "breakfast", "brunch", "supper",
        "dining", "dine", "ate", "eating", "eat", "restaurant", "cafe", "coffee", "tea",
        "snack", "snacks", "bakery", "biryani", "kacchi", "tehari", "khichuri", "burger", "pizza",
        "sandwich", "shawarma", "kebab", "kabab", "kitchen", "kfc", "mcdonald", "dominos",
        "foodpanda", "talabat", "deliveroo", "nasta", "khabar", "kheyechi", "khelam", "bhat",
        "paratha", "juice", "ice cream", "dessert", "fast food", "takeaway", "takeout",
        "fried chicken", "noodles", "pasta",
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
        "খেয়েছি", "খেয়েছি", "খেলাম", "খাইছি", "খাইলাম", "রেস্টুরেন্ট", "রেস্টুরেন্টে",
        "ক্যাফে", "নাস্তা", "লাঞ্চ", "ডিনার", "ব্রেকফাস্ট", "দুপুরের খাবার", "রাতের খাবার",
        "বিরিয়ানি", "বিরিয়ানি", "কাচ্চি", "বার্গার", "পিৎজা", "পিজ্জা", "কফি"
    )

    private val strongGroceryKeywords = listOf(
        "grocery", "groceries", "grocer", "supermarket", "super shop", "supershop", "hypermarket",
        "shwapno", "agora", "meena bazar", "meena", "unimart", "chaldal", "lulu", "carrefour",
        "sultan center", "sultan", "bazar", "bazaar", "kacha bazar", "raw market", "mart",
        "কাঁচাবাজার", "কাঁচা বাজার", "বাজার খরচ", "বাজার করেছি", "বাজার করলাম", "বাজার",
        "গ্রোসারি", "মুদি", "মুদিখানা", "সুপারশপ", "সুপার শপ", "হাইপারমার্কেট",
        "স্বপ্ন", "আগোরা", "মীনা বাজার", "চালডাল"
    )

    private val rawGroceryItemKeywords = listOf(
        "vegetables", "vegetable", "veggies", "fruits", "raw fish", "raw meat", "lentils", "spices",
        "flour", "rice", "milk", "eggs", "market",
        "সবজি", "শাকসবজি", "তরকারি", "মাছ", "মাংস", "চাল", "ডাল", "আটা", "ময়দা", "ময়দা",
        "চিনি", "লবণ", "পেঁয়াজ", "পেঁয়াজ", "রসুন", "আদা", "মসলা", "ডিম", "দুধ"
    )

    private val housingRentKeywords = listOf(
        "house rent", "room rent", "flat rent", "apartment rent", "home rent", "rent", "housing",
        "landlord", "hostel", "mess", "accommodation", "mortgage", "service charge", "basha bhara", "bara",
        "বাসা ভাড়া", "বাসা ভাড়া", "বাড়ি ভাড়া", "বাড়ি ভাড়া", "রুম ভাড়া", "রুম ভাড়া",
        "আবাসন", "ফ্ল্যাট ভাড়া", "ফ্ল্যাট ভাড়া", "মেস ভাড়া", "মেস ভাড়া", "সার্ভিস চার্জ"
    )

    private val utilitiesBillKeywords = listOf(
        "electricity", "electric", "power bill", "desco", "dpdc", "nesco", "wasa", "titas",
        "gas bill", "water bill", "wifi", "internet", "broadband", "utility", "utilities", "bill",
        "mobile recharge", "recharge", "telecom", "zain", "ooredoo", "stc", "grameenphone",
        "robi", "banglalink", "airtel", "teletalk", "postpaid", "prepaid", "current bill",
        "বিদ্যুৎ", "বিদ্যুৎ বিল", "কারেন্ট বিল", "গ্যাস", "গ্যাস বিল", "পানি", "পানির বিল",
        "ওয়াসা", "ওয়াসা", "ডেসকো", "ডিপিডিসি", "তিতাস", "ইন্টারনেট", "ওয়াইফাই", "ওয়াইফাই",
        "ব্রডব্যান্ড", "মোবাইল রিচার্জ", "রিচার্জ", "ফ্লেক্সিলোড", "ইউটিলিটি", "বিল"
    )

    private val transportKeywords = listOf(
        "uber", "pathao", "careem", "cng", "rickshaw", "bus", "metro", "train", "fuel", "petrol",
        "octane", "diesel", "gasoline", "taxi", "cab", "commute", "transport", "fare", "toll",
        "parking", "auto", "bike", "car wash", "station",
        "রিকশা", "উবার", "পাঠাও", "সিএনজি", "বাস", "বাস ভাড়া", "বাস ভাড়া", "মেট্রো", "মেট্রোরেল",
        "ট্রেন", "গাড়ি ভাড়া", "গাড়ি ভাড়া", "ভাড়া", "ভাড়া", "যাতায়াত", "যাতায়াত", "পরিবহন",
        "তেল", "ফুয়েল", "ফুয়েল", "পেট্রোল", "অকটেন", "পার্কিং", "টোল", "ট্যাক্সি"
    )

    private val familyRemittanceKeywords = listOf(
        "remittance", "send home", "sent home", "family support", "family expense", "parents",
        "mother", "father", "wife", "home support", "tuition", "school fee", "college fee",
        "family", "baba", "ma", "ammu", "abbu", "bashay pathiyechi", "deshe pathiyechi",
        "পরিবার", "পরিবারের", "রেমিট্যান্স", "দেশে পাঠিয়েছি", "দেশে পাঠিয়েছি", "বাসায় পাঠিয়েছি",
        "বাসায় পাঠিয়েছি", "বাড়িতে পাঠিয়েছি", "বাড়িতে পাঠিয়েছি", "মা-বাবা", "মা বাবা",
        "আব্বু", "আম্মু", "বউ", "সন্তান", "পড়াশোনা", "পড়াশোনা", "টিউশন", "স্কুল ফি"
    )

    private val healthKeywords = listOf(
        "medicine", "pharmacy", "doctor", "hospital", "clinic", "health", "medical", "med", "meds",
        "lab", "test", "diagnostic", "checkup", "dental", "dentist", "surgery", "prescription",
        "lazz pharma", "apollo", "evercare", "square hospital", "popular diagnostic",
        "ঔষধ", "ওষুধ", "ডাক্তার", "ডাক্তার ফি", "হাসপাতাল", "ক্লিনিক", "ফার্মেসি",
        "চিকিৎসা", "স্বাস্থ্য", "রক্ত পরীক্ষা", "টেস্ট", "ডায়াগনস্টিক", "ডায়াগনস্টিক", "প্রেসক্রিপশন"
    )

    private val shoppingKeywords = listOf(
        "shopping", "clothes", "clothing", "apparel", "shirt", "pant", "pants", "shoe", "shoes",
        "dress", "saree", "panjabi", "electronics", "daraz", "amazon", "aarong", "gadget",
        "phone", "laptop", "mall", "store", "shop", "fashion", "watch", "bag", "cosmetics", "perfume",
        "কেনাকাটা", "পোশাক", "জামা", "কাপড়", "কাপড়", "শার্ট", "প্যান্ট", "জুতা", "শপিং",
        "শাড়ি", "শাড়ি", "পাঞ্জাবি", "ইলেকট্রনিক্স", "গ্যাজেট", "মোবাইল ফোন", "কসমেটিকস", "আড়ং", "আড়ং"
    )

    private val subscriptionsKeywords = listOf(
        "subscription", "subscriptions", "netflix", "spotify", "youtube", "icloud", "google one",
        "chatgpt", "openai", "prime video", "hoichoi", "chorki", "bioscope", "apple music",
        "canva", "adobe", "microsoft 365", "domain", "hosting", "vpn", "saas", "digital",
        "streaming", "app store", "play store", "game", "gaming", "cinema", "movie", "ott",
        "সাবস্ক্রিপশন", "ডিজিটাল", "ডিজিটাল সেবা", "নেটফ্লিক্স", "স্পটিফাই", "ইউটিউব",
        "হইচই", "চরকি", "স্ট্রিমিং", "সফটওয়্যার", "সফটওয়্যার", "অ্যাপ", "সিনেমা", "বিনোদন", "গেম"
    )

    private val travelKeywords = listOf(
        "travel", "vacation", "holiday", "tour", "trip", "flight", "airline", "air ticket",
        "boarding", "hotel booking", "resort", "airbnb", "cox's bazar", "coxs bazar", "sylhet",
        "sajek", "visa", "passport", "luggage", "outing", "picnic", "sightseeing",
        "ভ্রমণ", "ভ্রমন", "ছুটি", "ট্যুর", "ট্রিপ", "ফ্লাইট", "এয়ার টিকেট", "এয়ার টিকেট",
        "বিমান টিকেট", "রিসোর্ট", "হোটেল বুকিং", "ভিসা", "পাসপোর্ট", "পিকনিক", "ঘুরতে", "কক্সবাজার"
    )

    private val charityGiftsKeywords = listOf(
        "charity", "donation", "donate", "zakat", "sadaqah", "sadaka", "fitra", "mosque", "masjid",
        "gift", "gifts", "present", "wedding gift", "birthday gift", "salami", "eidi", "help", "tips",
        "দান", "সদকা", "সদকাহ", "যাকাত", "জাকাত", "ফিতরা", "মসজিদ", "মাদ্রাসা", "এতিমখানা",
        "উপহার", "গিফট", "সালামি", "ঈদি", "বকশিশ", "অনুদান", "সহযোগিতা"
    )

    // --- Income Category Keywords (5 Categories) ---
    private val incomeSalaryKeywords = listOf(
        "salary", "wages", "payroll", "allowance", "stipend", "paycheck", "monthly pay",
        "overtime", "house rent allowance", "medical allowance", "beton", "bhata",
        "বেতন", "মাসিক বেতন", "ভাতা", "ওভারটাইম", "স্যালারি", "মাইনে"
    )

    private val incomeBonusKeywords = listOf(
        "bonus", "incentive", "incentives", "festival bonus", "eid bonus", "commission",
        "performance bonus", "reward", "appraisal", "increment", "award", "baksheesh",
        "বোনাস", "ঈদ বোনাস", "উৎসব বোনাস", "ইনসেনটিভ", "কমিশন", "পুরস্কার", "ইনক্রিমেন্ট"
    )

    private val incomeBusinessKeywords = listOf(
        "business", "side income", "side hustle", "freelance", "freelancing", "client", "project",
        "consulting", "sales", "shop income", "profit", "revenue", "dividend", "dps", "fdr",
        "investment", "upwork", "fiverr", "tuition income", "rent income", "rental income",
        "ব্যবসা", "ব্যবসায়িক", "ব্যবসায়িক", "সাইড ইনকাম", "ফ্রিল্যান্স", "ফ্রিল্যান্সিং",
        "ক্লায়েন্ট", "ক্লায়েন্ট", "প্রজেক্ট", "বিক্রি", "বিক্রয়", "মুনাফা", "লাভ", "লভ্যাংশ",
        "দোকান", "টিউশনি", "ভাড়া আয়", "ভাড়া আয়", "বিনিয়োগ", "বিনিয়োগ"
    )

    private val incomeRefundKeywords = listOf(
        "refund", "refunded", "cashback", "cash back", "rebate", "reimbursement", "reimbursed",
        "reversal", "reversed", "return money", "money back", "chargeback", "discount back",
        "রিফান্ড", "ক্যাশব্যাক", "ক্যাশ ব্যাক", "ফেরত", "টাকা ফেরত", "রিভার্সাল", "রিইম্বার্সমেন্ট"
    )

    // --- Unified Transaction Type Signals ---
    private val typeTransferKeywords = listOf(
        "transfer", "transferred", "remit", "remitted", "send to", "sent to", "move to", "moved to",
        "bank transfer", "wallet transfer", "beftn", "npsb", "->",
        "ট্রান্সফার", "স্থানান্তর", "পাঠিয়েছি", "পাঠিয়েছি", "পাঠালাম", "পাঠাইছি", "সরালাম",
        "pathiyechi", "pathalam", "pathaichi"
    )

    private val typeIncomeKeywords = listOf(
        "income", "salary", "received", "receive", "credited", "cash in", "cash-in", "cashin",
        "add money", "deposit", "deposited", "bonus", "incentive", "refund", "cashback",
        "freelance", "got paid", "paid me", "earned", "dividend", "profit", "side income",
        "beton", "peyechi", "pelam", "dhukeche", "dhukse", "dhuklo", "joma hoyeche", "joma", "eseche",
        "ঢুকেছে", "ঢুকছে", "ঢুকলো", "ঢুকল", "পেয়েছি", "পেয়েছি", "পেলাম", "পাওয়া গেছে", "পাওয়া গেছে",
        "জমা হয়েছে", "জমা হয়েছে", "জমা হলো", "জমা হল", "জমা", "এসেছে", "আসছে",
        "আয়", "আয়", "বেতন", "ভাতা", "বোনাস", "ইনসেনটিভ", "ক্যাশ ইন", "অ্যাড মানি", "রিফান্ড", "ক্যাশব্যাক", "মুনাফা", "লাভ"
    )

    private val typeExpenseKeywords = listOf(
        "spent", "spend", "paid", "bought", "buy", "expense", "cost", "purchase", "purchased",
        "cash out", "cash-out", "debited", "withdraw", "ate", "eating", "pay bill", "payment",
        "খরচ", "কিনেছি", "কিনলাম", "দিয়েছি", "দিয়েছি", "দিলাম", "কেটেছে", "ক্যাশ আউট",
        "পেমেন্ট", "বিল দিয়েছি", "বিল দিয়েছি", "খেয়েছি", "খেয়েছি", "খেলাম", "খাইছি",
        "khoroch", "kinechi", "kinlam", "kheyechi", "khelam", "dilam", "diyechi"
    )

    fun detectTransactionType(
        text: String,
        fallbackType: TransactionType = TransactionType.EXPENSE
    ): TransactionType {
        val raw = text.trim()
        if (raw.isBlank()) return fallbackType
        val lower = ReceiptVoucherMatcher.toAsciiDigits(raw).lowercase(Locale.US)

        val hasTransferVerb = typeTransferKeywords.any { matchesKeyword(lower, it) }
        val hasDirectionalTransfer = Regex("(?:from|theke|থেকে|হতে).+(?:to|te|তে|একাউন্টে|অ্যাকাউন্টে|ব্যাংকে|ওয়ালেটে|ওয়ালেটে|->)")
            .containsMatchIn(lower)
        val lowerForExpense = lower.replace("got paid", " ").replace("paid me", " ")
        val hasIncome = typeIncomeKeywords.any { matchesKeyword(lower, it) } ||
            (matchesKeyword(lower, "credit") && !lower.contains("credit card") && !lower.contains("credit limit"))
        val hasExpense = typeExpenseKeywords.any { matchesKeyword(lowerForExpense, it) }

        return when {
            hasTransferVerb && (hasDirectionalTransfer || !hasIncome) -> TransactionType.TRANSFER
            hasIncome && !hasExpense && !hasTransferVerb -> TransactionType.INCOME
            hasExpense && !hasIncome -> TransactionType.EXPENSE
            hasIncome && !lower.contains("খরচ") && !lower.contains("ক্যাশ আউট") && !lower.contains("cash out") -> TransactionType.INCOME
            else -> fallbackType
        }
    }

    /**
     * Detects the most accurate [ManagedCategoryItem] and its mapped [ExpenseCategory] for [text]
     * under [type]. Always checks active user-added custom categories first so newly added custom
     * categories synchronize immediately into smart detection across Voice, Receipt, and `+` Entry.
     */
    fun detectCategory(
        text: String,
        type: TransactionType,
        managedCategories: List<ManagedCategoryItem> = BuiltInCategoriesCatalog.all,
        language: AppLanguage = AppLanguage.BN
    ): DetectedCategoryMatch {
        val activeForType = managedCategories
            .filter { it.type == type && it.isActive }
            .ifEmpty { BuiltInCategoriesCatalog.defaultsFor(type) }

        val raw = text.trim()
        val lower = ReceiptVoucherMatcher.toAsciiDigits(raw).lowercase(Locale.US)

        // 1. Check user-created custom categories FIRST (exact or phrase match on en / bn / subcategories)
        if (lower.isNotBlank()) {
            val customMatch = activeForType
                .filter { !it.isBuiltIn }
                .firstOrNull { item -> matchesCustomCategory(lower, item) }
            if (customMatch != null) {
                return DetectedCategoryMatch(
                    managedCategory = customMatch,
                    legacyCategory = customMatch.legacyCategory ?: fallbackLegacy(type),
                    isExactCustomMatch = true
                )
            }
        }

        // 2. Built-in / expanded category detection by transaction type
        val targetId = when (type) {
            TransactionType.TRANSFER -> detectTransferCategoryId(lower)
            TransactionType.INCOME -> detectIncomeCategoryId(lower)
            TransactionType.EXPENSE -> detectExpenseCategoryId(lower)
        }

        // 3. Also check if user typed the exact label of any built-in/renamed category
        val matchedItem = activeForType.firstOrNull { it.id == targetId }
            ?: activeForType.firstOrNull { item -> matchesCustomCategory(lower, item) }
            ?: BuiltInCategoriesCatalog.defaultsFor(type).firstOrNull { it.id == targetId }
            ?: activeForType.first()

        return DetectedCategoryMatch(
            managedCategory = matchedItem,
            legacyCategory = matchedItem.legacyCategory ?: fallbackLegacy(type),
            isExactCustomMatch = false
        )
    }

    private fun detectTransferCategoryId(lower: String): String = when {
        listOf("bkash", "nagad", "rocket", "upay", "mfs", "বিকাশ", "নগদ", "রকেট", "উপায়", "উপায়", "ক্যাশ ইন", "ক্যাশ আউট")
            .any { matchesKeyword(lower, it) } -> "trf_mfs"
        listOf("bank", "dbbl", "dutch", "city", "brac", "trust", "islami", "sonali", "beftn", "npsb", "ব্যাংক", "ডাচ")
            .any { matchesKeyword(lower, it) } -> "trf_bank"
        else -> "trf_wallet"
    }

    private fun detectIncomeCategoryId(lower: String): String {
        if (lower.isBlank()) return "inc_salary"
        return when {
            incomeRefundKeywords.any { matchesKeyword(lower, it) } -> "inc_refund"
            incomeBonusKeywords.any { matchesKeyword(lower, it) } -> "inc_bonus"
            incomeBusinessKeywords.any { matchesKeyword(lower, it) } -> "inc_business"
            incomeSalaryKeywords.any { matchesKeyword(lower, it) } -> "inc_salary"
            else -> "inc_other"
        }
    }

    private fun detectExpenseCategoryId(lower: String): String {
        if (lower.isBlank()) return "exp_other"

        // Strip travel place names that contain "বাজার"/"bazar" (e.g., কক্সবাজার / Cox's Bazar)
        // before running grocery matching so travel entries never misclassify as Groceries.
        val lowerForGroceryCheck = lower
            .replace("কক্সবাজার", " ")
            .replace("cox's bazar", " ")
            .replace("coxs bazar", " ")

        val hasDining = diningAndMealKeywords.any { matchesKeyword(lower, it) } ||
            standaloneBanglaTeaRegex.containsMatchIn(lower)
        val hasMealConsumption = mealConsumptionKeywords.any { matchesKeyword(lower, it) }
        val hasStrongGrocery = strongGroceryKeywords.any { matchesKeyword(lowerForGroceryCheck, it) }
        val hasRawGroceryItem = rawGroceryItemKeywords.any { matchesKeyword(lowerForGroceryCheck, it) }

        return when {
            // Reserve Groceries for explicit market/grocery shopping (not eating out / meals)
            hasStrongGrocery && !hasMealConsumption -> "exp_groceries"

            // Meals, dining out, lunch, dinner, breakfast, snacks, biryani, restaurant, food items
            hasDining -> "exp_food_dining"

            // Raw grocery items when no dining/eating context is present
            hasRawGroceryItem -> "exp_groceries"

            // Family & Remittance (check before generic transport/other)
            familyRemittanceKeywords.any { matchesKeyword(lower, it) } -> "exp_family_remittance"

            // Housing & Rent (distinct from Utilities & Bills)
            housingRentKeywords.any { matchesKeyword(lower, it) } -> "exp_housing_rent"

            // Utilities & Bills (electricity, gas, water, Wi-Fi, mobile recharge, telecom)
            utilitiesBillKeywords.any { matchesKeyword(lower, it) } -> "exp_utilities_bills"

            // Health & Medical
            healthKeywords.any { matchesKeyword(lower, it) } -> "exp_health"

            // Travel & Vacation (flights, tours, resorts, visa — check before generic transport)
            travelKeywords.any { matchesKeyword(lower, it) } -> "exp_travel"

            // Transport & Commute
            transportKeywords.any { matchesKeyword(lower, it) } -> "exp_transport"

            // Charity & Gifts (check before shopping so "gift/উপহার/দান" maps to Charity & Gifts)
            charityGiftsKeywords.any { matchesKeyword(lower, it) } -> "exp_charity_gifts"

            // Subscriptions & Digital Services
            subscriptionsKeywords.any { matchesKeyword(lower, it) } -> "exp_subscriptions"

            // Shopping & Apparel
            shoppingKeywords.any { matchesKeyword(lower, it) } -> "exp_shopping"

            else -> "exp_other"
        }
    }

    private fun matchesCustomCategory(lower: String, item: ManagedCategoryItem): Boolean {
        val candidates = buildList {
            if (item.en.isNotBlank()) add(item.en)
            if (item.bn.isNotBlank()) add(item.bn)
            item.subcategories.forEach { sub ->
                if (sub.en.isNotBlank()) add(sub.en)
                if (sub.bn.isNotBlank()) add(sub.bn)
            }
        }
            .map { ReceiptVoucherMatcher.toAsciiDigits(it).lowercase(Locale.US).trim() }
            .filter { it.length >= 2 }
            .distinct()

        return candidates.any { candidate -> matchesKeyword(lower, candidate) }
    }

    private fun fallbackLegacy(type: TransactionType): ExpenseCategory = when (type) {
        TransactionType.INCOME -> ExpenseCategory.SALARY_INCOME
        TransactionType.TRANSFER -> ExpenseCategory.TRANSFER
        TransactionType.EXPENSE -> ExpenseCategory.OTHER
    }

    internal fun matchesKeyword(text: String, token: String): Boolean {
        if (token.isBlank()) return false
        val isPureAscii = token.all { it.code < 128 }
        return if (isPureAscii) {
            Regex("(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9])").containsMatchIn(text)
        } else {
            text.contains(token)
        }
    }
}
