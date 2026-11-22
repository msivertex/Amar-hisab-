package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.localization.AppLanguage
import com.example.ui.theme.BrandBkash
import com.example.ui.theme.BrandBracBank
import com.example.ui.theme.BrandCash
import com.example.ui.theme.BrandCityBank
import com.example.ui.theme.BrandDbbl
import com.example.ui.theme.BrandIslamiBank
import com.example.ui.theme.BrandNagad
import com.example.ui.theme.BrandRocket
import com.example.ui.theme.BrandSonaliBank
import com.example.ui.theme.BrandTrustBank
import com.example.ui.theme.BrandUpay
import com.example.ui.theme.CatEntertainment
import com.example.ui.theme.CatFood
import com.example.ui.theme.CatGroceries
import com.example.ui.theme.CatHealth
import com.example.ui.theme.CatIncome
import com.example.ui.theme.CatRent
import com.example.ui.theme.CatShopping
import com.example.ui.theme.CatTransfer
import com.example.ui.theme.CatTransport
import com.example.ui.theme.SlateOcean
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class AppTab {
    HOME,
    ACTIVITY,
    PLAN,
    INSIGHTS
}

enum class AuthMode {
    GUEST,
    GOOGLE
}

enum class AccountType {
    DEBIT_BANK,
    CREDIT_CARD,
    MFS,
    CASH
}

enum class BankSubtype(val en: String, val bn: String) {
    SALARY("Salary Account", "স্যালারি অ্যাকাউন্ট"),
    SAVINGS("Savings Account", "সেভিংস অ্যাকাউন্ট");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class CardNetwork(val label: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express")
}

enum class BangladeshiBank(
    val en: String,
    val bn: String,
    val shortCode: String,
    val brandColor: Color
) {
    CITY_BANK("City Bank", "সিটি ব্যাংক", "CITY", BrandCityBank),
    DBBL("Dutch-Bangla Bank", "ডাচ-বাংলা ব্যাংক", "DBBL", BrandDbbl),
    TRUST_BANK("Trust Bank", "ট্রাস্ট ব্যাংক", "TRUST", BrandTrustBank),
    ISLAMI_BANK("Islami Bank", "ইসলামী ব্যাংক", "IBBL", BrandIslamiBank),
    SONALI_BANK("Sonali Bank", "সোনালী ব্যাংক", "SONALI", BrandSonaliBank),
    BRAC_BANK("BRAC Bank", "ব্র্যাক ব্যাংক", "BRAC", BrandBracBank);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class MfsProvider(
    val en: String,
    val bn: String,
    val brandColor: Color,
    val secondaryColor: Color
) {
    BKASH("bKash", "বিকাশ", BrandBkash, Color(0xFFF43F5E)),
    NAGAD("Nagad", "নগদ", BrandNagad, Color(0xFFDC2626)),
    ROCKET("Rocket", "রকেট", BrandRocket, Color(0xFF6B21A8)),
    UPAY("Upay", "উপায়", BrandUpay, Color(0xFFEAB308));

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

data class SubcategoryOption(val key: String, val en: String, val bn: String) {
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class ExpenseCategory(
    val en: String,
    val bn: String,
    val color: Color
) {
    FOOD_DINING("Food & Dining", "খাবার ও ডাইনিং", CatFood),
    GROCERIES("Groceries", "বাজার ও গ্রোসারি", CatGroceries),
    RENT_UTILITIES("Rent & Utilities", "বাসা ভাড়া ও বিল", CatRent),
    TRANSPORT("Transport", "যাতায়াত", CatTransport),
    SHOPPING("Shopping", "কেনাকাটা", CatShopping),
    HEALTH("Health & Meds", "স্বাস্থ্য ও ঔষধ", CatHealth),
    ENTERTAINMENT("Entertainment", "বিনোদন", CatEntertainment),
    SALARY_INCOME("Salary & Income", "বেতন ও আয়", CatIncome),
    TRANSFER("Wallet Transfer", "অ্যাকাউন্ট স্থানান্তর", CatTransfer),
    OTHER("Other", "অন্যান্য", SlateOcean);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en

    fun subcategories(): List<SubcategoryOption> = when (this) {
        FOOD_DINING -> listOf(
            SubcategoryOption("Restaurant", "Restaurant", "রেস্টুরেন্ট"),
            SubcategoryOption("Coffee & Tea", "Coffee & Tea", "চা ও কফি"),
            SubcategoryOption("Snacks", "Street Snacks", "নাস্তা")
        )
        GROCERIES -> listOf(
            SubcategoryOption("Super Shop", "Super Shop", "সুপার শপ"),
            SubcategoryOption("Kacha Bazar", "Kacha Bazar", "কাঁচাবাজার"),
            SubcategoryOption("Fish & Meat", "Fish & Meat", "মাছ ও মাংস")
        )
        RENT_UTILITIES -> listOf(
            SubcategoryOption("House Rent", "House Rent", "বাসা ভাড়া"),
            SubcategoryOption("Electricity", "Electricity Bill", "বিদ্যুৎ বিল"),
            SubcategoryOption("Internet & Gas", "Internet & Gas", "ইন্টারনেট ও গ্যাস")
        )
        TRANSPORT -> listOf(
            SubcategoryOption("Ride Share", "Ride Share", "রাইড শেয়ার"),
            SubcategoryOption("Metro & Bus", "Metro & Bus", "মেট্রো ও বাস"),
            SubcategoryOption("Fuel", "Fuel / CNG", "জ্বালানি")
        )
        SHOPPING -> listOf(
            SubcategoryOption("Clothing", "Clothing", "পোশাক"),
            SubcategoryOption("Electronics", "Electronics", "ইলেকট্রনিক্স"),
            SubcategoryOption("Home Care", "Home Care", "ঘরের সামগ্রী")
        )
        HEALTH -> listOf(
            SubcategoryOption("Medicine", "Medicine", "ঔষধ"),
            SubcategoryOption("Doctor", "Doctor Visit", "ডাক্তার ফি")
        )
        ENTERTAINMENT -> listOf(
            SubcategoryOption("Streaming", "Streaming & Apps", "সাবস্ক্রিপশন"),
            SubcategoryOption("Outing", "Outing & Cinema", "ভ্রমণ ও সিনেমা")
        )
        SALARY_INCOME -> listOf(
            SubcategoryOption("Monthly Salary", "Monthly Salary", "মাসিক বেতন"),
            SubcategoryOption("Freelance", "Freelance", "ফ্রিল্যান্সিং"),
            SubcategoryOption("Bonus", "Bonus / Gift", "বোনাস ও উপহার")
        )
        TRANSFER -> listOf(
            SubcategoryOption("Wallet Transfer", "Wallet Transfer", "ওয়ালেট স্থানান্তর")
        )
        OTHER -> listOf(
            SubcategoryOption("Debt Repayment", "Debt Repayment", "দেনা-পাওনা পরিশোধ"),
            SubcategoryOption("Miscellaneous", "Miscellaneous", "বিবিধ")
        )
    }
}

enum class CategorySortMode {
    DEFAULT,
    CUSTOM,
    MOST_USED
}

data class ManagedCategoryItem(
    val id: String,
    val type: TransactionType,
    val en: String,
    val bn: String,
    val accentColor: Color,
    val iconKey: String,
    val subcategories: List<SubcategoryOption>,
    val isBuiltIn: Boolean = true,
    val isActive: Boolean = true,
    val legacyCategory: ExpenseCategory? = null
) {
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

object CategoryEditorCatalog {
    val colorSwatches: List<Color> = listOf(
        Color(0xFFEA580C), // Tangerine Orange
        Color(0xFF16A34A), // Emerald Green
        Color(0xFF0284C7), // Sky Cyan Blue
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF7C3AED), // Violet Purple
        Color(0xFFDB2777), // Vibrant Pink
        Color(0xFFE11D48), // Crimson Red
        Color(0xFF0D9488), // Teal
        Color(0xFFD97706), // Amber Gold
        Color(0xFF4F46E5), // Indigo
        Color(0xFF059669), // Jade
        Color(0xFF475569)  // Slate Ocean
    )

    val iconKeys: List<String> = listOf(
        "restaurant",
        "groceries",
        "home",
        "transport",
        "shopping",
        "health",
        "entertainment",
        "payments",
        "work",
        "business",
        "trending_up",
        "gift",
        "transfer",
        "bank",
        "mfs",
        "education",
        "travel",
        "bills",
        "savings",
        "other"
    )
}

object BuiltInCategoriesCatalog {
    val all: List<ManagedCategoryItem> = listOf(
        // Expense Categories (8)
        ManagedCategoryItem(
            id = "exp_food_dining",
            type = TransactionType.EXPENSE,
            en = "Food & Dining",
            bn = "খাবার ও ডাইনিং",
            accentColor = CatFood,
            iconKey = "restaurant",
            subcategories = ExpenseCategory.FOOD_DINING.subcategories(),
            legacyCategory = ExpenseCategory.FOOD_DINING
        ),
        ManagedCategoryItem(
            id = "exp_groceries",
            type = TransactionType.EXPENSE,
            en = "Groceries",
            bn = "বাজার ও গ্রোসারি",
            accentColor = CatGroceries,
            iconKey = "groceries",
            subcategories = ExpenseCategory.GROCERIES.subcategories(),
            legacyCategory = ExpenseCategory.GROCERIES
        ),
        ManagedCategoryItem(
            id = "exp_rent_utilities",
            type = TransactionType.EXPENSE,
            en = "Rent & Utilities",
            bn = "বাসা ভাড়া ও বিল",
            accentColor = CatRent,
            iconKey = "home",
            subcategories = ExpenseCategory.RENT_UTILITIES.subcategories(),
            legacyCategory = ExpenseCategory.RENT_UTILITIES
        ),
        ManagedCategoryItem(
            id = "exp_transport",
            type = TransactionType.EXPENSE,
            en = "Transport",
            bn = "যাতায়াত",
            accentColor = CatTransport,
            iconKey = "transport",
            subcategories = ExpenseCategory.TRANSPORT.subcategories(),
            legacyCategory = ExpenseCategory.TRANSPORT
        ),
        ManagedCategoryItem(
            id = "exp_shopping",
            type = TransactionType.EXPENSE,
            en = "Shopping",
            bn = "কেনাকাটা",
            accentColor = CatShopping,
            iconKey = "shopping",
            subcategories = ExpenseCategory.SHOPPING.subcategories(),
            legacyCategory = ExpenseCategory.SHOPPING
        ),
        ManagedCategoryItem(
            id = "exp_health",
            type = TransactionType.EXPENSE,
            en = "Health & Meds",
            bn = "স্বাস্থ্য ও ঔষধ",
            accentColor = CatHealth,
            iconKey = "health",
            subcategories = ExpenseCategory.HEALTH.subcategories(),
            legacyCategory = ExpenseCategory.HEALTH
        ),
        ManagedCategoryItem(
            id = "exp_entertainment",
            type = TransactionType.EXPENSE,
            en = "Entertainment",
            bn = "বিনোদন",
            accentColor = CatEntertainment,
            iconKey = "entertainment",
            subcategories = ExpenseCategory.ENTERTAINMENT.subcategories(),
            legacyCategory = ExpenseCategory.ENTERTAINMENT
        ),
        ManagedCategoryItem(
            id = "exp_other",
            type = TransactionType.EXPENSE,
            en = "Other",
            bn = "অন্যান্য",
            accentColor = SlateOcean,
            iconKey = "other",
            subcategories = ExpenseCategory.OTHER.subcategories(),
            legacyCategory = ExpenseCategory.OTHER
        ),

        // Income Categories (5)
        ManagedCategoryItem(
            id = "inc_salary",
            type = TransactionType.INCOME,
            en = "Salary & Wages",
            bn = "বেতন ও আয়",
            accentColor = CatIncome,
            iconKey = "payments",
            subcategories = listOf(
                SubcategoryOption("Monthly Salary", "Monthly Salary", "মাসিক বেতন"),
                SubcategoryOption("Overtime", "Overtime", "ওভারটাইম")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_freelance",
            type = TransactionType.INCOME,
            en = "Freelancing",
            bn = "ফ্রিল্যান্সিং",
            accentColor = Color(0xFF0284C7),
            iconKey = "work",
            subcategories = listOf(
                SubcategoryOption("Client Project", "Client Project", "ক্লায়েন্ট প্রজেক্ট"),
                SubcategoryOption("Consulting", "Consulting", "কনসাল্টিং")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_business",
            type = TransactionType.INCOME,
            en = "Business",
            bn = "ব্যবসা",
            accentColor = Color(0xFF0D9488),
            iconKey = "business",
            subcategories = listOf(
                SubcategoryOption("Sales Profit", "Sales Profit", "বিক্রয় মুনাফা"),
                SubcategoryOption("Commission", "Commission", "কমিশন")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_investment",
            type = TransactionType.INCOME,
            en = "Investments & Profit",
            bn = "বিনিয়োগ ও লভ্যাংশ",
            accentColor = Color(0xFF7C3AED),
            iconKey = "trending_up",
            subcategories = listOf(
                SubcategoryOption("DPS / FDR", "DPS / FDR Profit", "ডিপিএস / এফডিআর"),
                SubcategoryOption("Dividend", "Dividend", "লভ্যাংশ")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_bonus_gift",
            type = TransactionType.INCOME,
            en = "Bonus & Gift",
            bn = "বোনাস ও উপহার",
            accentColor = Color(0xFFD97706),
            iconKey = "gift",
            subcategories = listOf(
                SubcategoryOption("Festival Bonus", "Festival Bonus", "উৎসব বোনাস"),
                SubcategoryOption("Cash Gift", "Cash Gift", "নগদ উপহার")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),

        // Transfer Categories (3)
        ManagedCategoryItem(
            id = "trf_wallet",
            type = TransactionType.TRANSFER,
            en = "Wallet Transfer",
            bn = "ওয়ালেট ট্রান্সফার",
            accentColor = CatTransfer,
            iconKey = "transfer",
            subcategories = listOf(
                SubcategoryOption("Own Account", "Own Account", "নিজের অ্যাকাউন্ট")
            ),
            legacyCategory = ExpenseCategory.TRANSFER
        ),
        ManagedCategoryItem(
            id = "trf_bank",
            type = TransactionType.TRANSFER,
            en = "Bank Transfer",
            bn = "ব্যাংক ট্রান্সফার",
            accentColor = Color(0xFF1D4ED8),
            iconKey = "bank",
            subcategories = listOf(
                SubcategoryOption("BEFTN / NPSB", "BEFTN / NPSB", "বিইএফটিএন / এনপিএসবি"),
                SubcategoryOption("Card Payment", "Credit Card Bill", "ক্রেডিট কার্ড বিল")
            ),
            legacyCategory = ExpenseCategory.TRANSFER
        ),
        ManagedCategoryItem(
            id = "trf_mfs",
            type = TransactionType.TRANSFER,
            en = "MFS Cash In / Out",
            bn = "এমএফএস ক্যাশ ইন/আউট",
            accentColor = Color(0xFFE11D48),
            iconKey = "mfs",
            subcategories = listOf(
                SubcategoryOption("bKash / Nagad", "bKash / Nagad", "বিকাশ / নগদ"),
                SubcategoryOption("Agent Cash Out", "Agent Cash Out", "এজেন্ট ক্যাশ আউট")
            ),
            legacyCategory = ExpenseCategory.TRANSFER
        )
    )

    fun defaultsFor(type: TransactionType): List<ManagedCategoryItem> =
        all.filter { it.type == type }

    /**
     * Merges built-in defaults with user overrides (edited built-ins + new custom categories),
     * filters out deleted built-ins, and returns all items belonging to [type].
     */
    fun mergedFor(
        type: TransactionType,
        overrides: List<ManagedCategoryItem>,
        deletedIds: Set<String>
    ): List<ManagedCategoryItem> {
        val overrideMap = overrides.associateBy { it.id }
        val builtInMerged = all
            .filterNot { it.id in deletedIds }
            .map { builtIn -> overrideMap[builtIn.id] ?: builtIn }
            .filter { it.type == type }
        val builtInIds = all.map { it.id }.toSet()
        val customAdded = overrides.filter { it.id !in builtInIds && it.id !in deletedIds && it.type == type }
        return builtInMerged + customAdded
    }

    fun orderedFor(type: TransactionType, savedOrder: List<String>): List<ManagedCategoryItem> =
        applyCustomOrder(defaultsFor(type), savedOrder)

    fun applyCustomOrder(
        items: List<ManagedCategoryItem>,
        savedOrder: List<String>
    ): List<ManagedCategoryItem> {
        if (savedOrder.isEmpty()) return items
        val byId = items.associateBy { it.id }
        val ordered = savedOrder.mapNotNull { byId[it] }
        val remaining = items.filterNot { item -> ordered.any { it.id == item.id } }
        return ordered + remaining
    }

    fun encodeOverridesToJson(items: List<ManagedCategoryItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type.name)
                put("en", item.en)
                put("bn", item.bn)
                put("colorArgb", item.accentColor.value.toLong())
                put("iconKey", item.iconKey)
                put("isBuiltIn", item.isBuiltIn)
                put("isActive", item.isActive)
                if (item.legacyCategory != null) {
                    put("legacyCategory", item.legacyCategory.name)
                }
                val subs = JSONArray()
                item.subcategories.forEach { sub ->
                    subs.put(
                        JSONObject().apply {
                            put("key", sub.key)
                            put("en", sub.en)
                            put("bn", sub.bn)
                        }
                    )
                }
                put("subcategories", subs)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeOverridesFromJson(raw: String): List<ManagedCategoryItem> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val type = runCatching { TransactionType.valueOf(obj.optString("type")) }
                        .getOrNull() ?: TransactionType.EXPENSE
                    val colorLong = obj.optLong("colorArgb", 0L)
                    val color = if (colorLong != 0L) Color(colorLong.toULong()) else CatFood
                    val legacy = obj.optString("legacyCategory").takeIf { it.isNotBlank() }?.let {
                        runCatching { ExpenseCategory.valueOf(it) }.getOrNull()
                    }
                    val subsArr = obj.optJSONArray("subcategories")
                    val subs = buildList {
                        if (subsArr != null) {
                            for (j in 0 until subsArr.length()) {
                                val s = subsArr.optJSONObject(j) ?: continue
                                val k = s.optString("key")
                                val enVal = s.optString("en", k)
                                val bnVal = s.optString("bn", enVal)
                                if (enVal.isNotBlank() || bnVal.isNotBlank()) {
                                    add(SubcategoryOption(k.ifBlank { enVal }, enVal, bnVal))
                                }
                            }
                        }
                    }
                    add(
                        ManagedCategoryItem(
                            id = id,
                            type = type,
                            en = obj.optString("en"),
                            bn = obj.optString("bn"),
                            accentColor = color,
                            iconKey = obj.optString("iconKey", "other"),
                            subcategories = subs,
                            isBuiltIn = obj.optBoolean("isBuiltIn", false),
                            isActive = obj.optBoolean("isActive", true),
                            legacyCategory = legacy
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

enum class LoanType {
    I_OWE,      // দেনা (Money I Owe)
    OWED_TO_ME  // পাওনা (Money Owed to Me)
}

enum class InsightTimeFilter(val en: String, val bn: String) {
    DAILY("Daily", "দৈনিক"),
    THIS_WEEK("This Week", "এই সপ্তাহ"),
    THIS_MONTH("This Month", "এই মাস"),
    LAST_MONTH("Last Month", "গত মাস");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class TransactionSortOption(val en: String, val bn: String) {
    NEWEST("Newest", "নতুন আগে"),
    OLDEST("Oldest", "পুরাতন আগে"),
    HIGHEST("Highest ৳", "বেশি টাকা"),
    LOWEST("Lowest ৳", "কম টাকা");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class StartOfWeek(val calendarDay: Int, val en: String, val bn: String) {
    SATURDAY(Calendar.SATURDAY, "Saturday", "শনিবার"),
    SUNDAY(Calendar.SUNDAY, "Sunday", "রবিবার"),
    MONDAY(Calendar.MONDAY, "Monday", "সোমবার"),
    TUESDAY(Calendar.TUESDAY, "Tuesday", "মঙ্গলবার"),
    WEDNESDAY(Calendar.WEDNESDAY, "Wednesday", "বুধবার"),
    THURSDAY(Calendar.THURSDAY, "Thursday", "বৃহস্পতিবার"),
    FRIDAY(Calendar.FRIDAY, "Friday", "শুক্রবার");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class AppThemeMode(val en: String, val bn: String) {
    AUTO("Auto", "অটো"),
    LIGHT("Light", "লাইট"),
    DARK("Dark", "ডার্ক");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

enum class AppColourTheme(
    val en: String,
    val bn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightCanvasTint: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val previewGradient: List<Color>,
    val accentRedDot: Color? = null
) {
    SKY_CYAN(
        en = "Sky Cyan",
        bn = "Sky Cyan",
        subtitleEn = "Crystal Liquid signature",
        subtitleBn = "ক্রিস্টাল লিকুইড ডিফল্ট",
        lightPrimary = Color(0xFF0284C7),
        lightSecondary = Color(0xFF38BDF8),
        lightCanvasTint = Color(0xFFE0F4FF),
        darkPrimary = Color(0xFF06B6D4),
        darkSecondary = Color(0xFF22D3EE),
        previewGradient = listOf(Color(0xFF0284C7), Color(0xFF06B6D4), Color(0xFF38BDF8))
    ),
    INDIGO(
        en = "Indigo",
        bn = "Indigo",
        subtitleEn = "The Hisab Pati brand",
        subtitleBn = "দ্য হিসাব পাতি ব্র্যান্ড",
        lightPrimary = Color(0xFF4F46E5),
        lightSecondary = Color(0xFF818CF8),
        lightCanvasTint = Color(0xFFEEF2FF),
        darkPrimary = Color(0xFF6366F1),
        darkSecondary = Color(0xFFA5B4FC),
        previewGradient = listOf(Color(0xFF3730A3), Color(0xFF4F46E5), Color(0xFF818CF8))
    ),
    BANGLA_GREEN(
        en = "Bangla Green",
        bn = "Bangla Green",
        subtitleEn = "Deep green & crimson accent",
        subtitleBn = "গাঢ় সবুজ ও লাল আভা",
        lightPrimary = Color(0xFF006A4E),
        lightSecondary = Color(0xFFF42A41),
        lightCanvasTint = Color(0xFFECFDF5),
        darkPrimary = Color(0xFF10B981),
        darkSecondary = Color(0xFFFB7185),
        previewGradient = listOf(Color(0xFF064E3B), Color(0xFF006A4E), Color(0xFF10B981)),
        accentRedDot = Color(0xFFF42A41)
    ),
    ROYAL_VIOLET(
        en = "Royal Violet",
        bn = "Royal Violet",
        subtitleEn = "Amethyst frost vault",
        subtitleBn = "উজ্জ্বল অ্যামেথিস্ট থিম",
        lightPrimary = Color(0xFF7C3AED),
        lightSecondary = Color(0xFFC084FC),
        lightCanvasTint = Color(0xFFF5F3FF),
        darkPrimary = Color(0xFFA855F7),
        darkSecondary = Color(0xFFD8B4FE),
        previewGradient = listOf(Color(0xFF5B21B6), Color(0xFF7C3AED), Color(0xFFC084FC))
    ),
    CRIMSON_CORAL(
        en = "Crimson Coral",
        bn = "Crimson Coral",
        subtitleEn = "Warm ruby & sunset glow",
        subtitleBn = "উষ্ণ রুবি ও সানসেট আভা",
        lightPrimary = Color(0xFFE11D48),
        lightSecondary = Color(0xFFF97316),
        lightCanvasTint = Color(0xFFFFF1F2),
        darkPrimary = Color(0xFFFB7185),
        darkSecondary = Color(0xFFFB923C),
        previewGradient = listOf(Color(0xFF9F1239), Color(0xFFE11D48), Color(0xFFF97316))
    );

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
    fun subtitle(lang: AppLanguage): String = if (lang == AppLanguage.BN) subtitleBn else subtitleEn
}

enum class HomeLayoutStyle {
    BALANCE_CARD,
    WALLET_STACK
}

enum class HomeSectionId(val en: String, val bn: String) {
    SUMMARY("Balance Card", "ব্যালেন্স কার্ড"),
    WALLETS("Wallet Carousel", "ওয়ালেট ক্যারোসেল"),
    BUDGET("Monthly Budget", "মাসিক বাজেট"),
    STREAK("Spending Streak", "ব্যয় নিয়ন্ত্রণ স্ট্রিক"),
    SHORTCUTS("Quick Shortcuts", "দ্রুত শর্টকাট"),
    RECENT("Recent Activity", "সাম্প্রতিক কার্যকলাপ"),
    UPCOMING_BILLS("Upcoming Bills", "আসন্ন বিলসমূহ"),
    GOALS("Savings Goals", "সঞ্চয় লক্ষ্য"),
    FINANCIAL_HEALTH("Financial Health", "আর্থিক স্বাস্থ্য");

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

data class DailyTrendPoint(
    val dayLabel: String,
    val amount: Double,
    val isPeak: Boolean = false
)

data class NetWorthSummary(
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0
) {
    val netWorth: Double get() = totalAssets - totalLiabilities
    val assetRatio: Float
        get() {
            val total = totalAssets + totalLiabilities
            return if (total <= 0.0) 1f else (totalAssets / total).toFloat().coerceIn(0f, 1f)
        }
}

data class CategorySpend(
    val category: ExpenseCategory,
    val amount: Double,
    val share: Float
)

data class SmartAlert(
    val id: String,
    val title: String,
    val subtitle: String,
    val isWarning: Boolean,
    val actionRoute: com.example.notifications.NotificationActionRoute = com.example.notifications.NotificationActionRoute.OPEN_HOME,
    val category: com.example.notifications.NotificationCategory = com.example.notifications.NotificationCategory.BUDGET_THRESHOLD,
    val timestampMillis: Long? = null
)

data class ReceiptScanResult(
    val merchantName: String,
    val amountBdt: Double,
    val category: ExpenseCategory,
    val dateLabel: String,
    val itemsSummary: String,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val feeAmount: Double = 0.0,
    val currencyCode: String = "BDT",
    val providerHint: String = "",
    val accountLast4Hint: String = "",
    val referenceId: String = "",
    val timestampMillis: Long? = null,
    val detectedDirectionKeyword: String = "",
    val isLocalFallback: Boolean = false
)

data class GroundingSource(
    val title: String,
    val uri: String
)

data class SearchGroundingResult(
    val answer: String,
    val searchQueries: List<String>,
    val sources: List<GroundingSource>
)

data class VoiceParsedTransaction(
    val title: String,
    val amountBdt: Double,
    val type: TransactionType,
    val category: ExpenseCategory,
    val accountHint: String,
    val currencyCode: String = "BDT",
    val toAccountHint: String = "",
    val note: String = "",
    val isLocalFallback: Boolean = false
)

fun defaultColorForCash(): Color = BrandCash
