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

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

enum class CardNetwork(val label: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express")
}

enum class BankCategory(val en: String, val bn: String) {
    GOVERNMENT("Government Banks", "সরকারি ব্যাংক"),
    PRIVATE("Private Banks", "বেসরকারি ব্যাংক");

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

enum class BangladeshiBank(
    val en: String,
    val bn: String,
    val shortCode: String,
    val brandColor: Color,
    val category: BankCategory = BankCategory.PRIVATE
) {
    // Existing 6 Banks
    CITY_BANK("City Bank", "সিটি ব্যাংক", "CITY", BrandCityBank, BankCategory.PRIVATE),
    DBBL("Dutch-Bangla Bank", "ডাচ-বাংলা ব্যাংক", "DBBL", BrandDbbl, BankCategory.PRIVATE),
    TRUST_BANK("Trust Bank", "ট্রাস্ট ব্যাংক", "TRUST", BrandTrustBank, BankCategory.PRIVATE),
    ISLAMI_BANK("Islami Bank", "ইসলামী ব্যাংক", "IBBL", BrandIslamiBank, BankCategory.PRIVATE),
    SONALI_BANK("Sonali Bank", "সোনালী ব্যাংক", "SONALI", BrandSonaliBank, BankCategory.GOVERNMENT),
    BRAC_BANK("BRAC Bank", "ব্র্যাক ব্যাংক", "BRAC", BrandBracBank, BankCategory.PRIVATE),

    // Government Banks (Additional)
    JANATA_BANK("Janata Bank", "জনতা ব্যাংক", "JANATA", Color(0xFF0B5D3B), BankCategory.GOVERNMENT),
    AGRANI_BANK("Agrani Bank", "অগ্রণী ব্যাংক", "AGRANI", Color(0xFF15803D), BankCategory.GOVERNMENT),
    RUPALI_BANK("Rupali Bank", "রূপালী ব্যাংক", "RUPALI", Color(0xFF1D4ED8), BankCategory.GOVERNMENT),
    BKB("Bangladesh Krishi Bank", "বাংলাদেশ কৃষি ব্যাংক", "BKB", Color(0xFF166534), BankCategory.GOVERNMENT),

    // Private Banks (Additional)
    EBL("Eastern Bank", "ইস্টার্ন ব্যাংক", "EBL", Color(0xFF005DAA), BankCategory.PRIVATE),
    PRIME_BANK("Prime Bank", "প্রাইম ব্যাংক", "PRIME", Color(0xFF0F766E), BankCategory.PRIVATE),
    MTB("Mutual Trust Bank", "মিউচুয়াল ট্রাস্ট ব্যাংক", "MTB", Color(0xFF047857), BankCategory.PRIVATE),
    DHAKA_BANK("Dhaka Bank", "ঢাকা ব্যাংক", "DHAKA", Color(0xFF0369A1), BankCategory.PRIVATE),
    PUBALI_BANK("Pubali Bank", "পূবালী ব্যাংক", "PUBALI", Color(0xFF15803D), BankCategory.PRIVATE),
    BANK_ASIA("Bank Asia", "ব্যাংক এশিয়া", "BANKASIA", Color(0xFF1E40AF), BankCategory.PRIVATE),
    SEBL("Southeast Bank", "সাউথইস্ট ব্যাংক", "SEBL", Color(0xFF0E7490), BankCategory.PRIVATE),
    ONE_BANK("One Bank", "ওয়ান ব্যাংক", "ONE", Color(0xFFBE123C), BankCategory.PRIVATE),
    UTTARA_BANK("Uttara Bank", "উত্তরা ব্যাংক", "UTTARA", Color(0xFF0F4C81), BankCategory.PRIVATE),
    JAMUNA_BANK("Jamuna Bank", "যমুনা ব্যাংক", "JAMUNA", Color(0xFF1D4ED8), BankCategory.PRIVATE),
    UCB("United Commercial Bank", "ইউনাইটেড কমার্শিয়াল ব্যাংক", "UCB", Color(0xFFB91C1C), BankCategory.PRIVATE),
    MBL("Mercantile Bank", "মার্কেন্টাইল ব্যাংক", "MBL", Color(0xFF0D9488), BankCategory.PRIVATE);

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

enum class MfsProvider(
    val en: String,
    val bn: String,
    val brandColor: Color,
    val secondaryColor: Color,
    val parentBankEn: String = "",
    val parentBankBn: String = ""
) {
    BKASH("bKash", "বিকাশ", BrandBkash, Color(0xFFF43F5E)),
    NAGAD("Nagad", "নগদ", BrandNagad, Color(0xFFDC2626)),
    ROCKET("Rocket", "রকেট", BrandRocket, Color(0xFF6B21A8), "DBBL", "ডাচ-বাংলা ব্যাংক"),
    UPAY("Upay", "উপায়", BrandUpay, Color(0xFFEAB308), "UCB", "ইউসিবি"),
    TAP("Tap", "ট্যাপ", Color(0xFF006A4E), Color(0xFF10B981), "Trust Axiata Pay", "ট্রাস্ট আজিয়াটা পে"),
    OK_WALLET("OK Wallet", "ওকে ওয়ালেট", Color(0xFFDC2626), Color(0xFFF97316), "One Bank", "ওয়ান ব্যাংক"),
    MCASH("mCash", "এমক্যাশ", Color(0xFF047857), Color(0xFF10B981), "Islami Bank", "ইসলামী ব্যাংক"),
    SURECASH("SureCash", "শিওরক্যাশ", Color(0xFF0284C7), Color(0xFF38BDF8)),
    MYCASH("MyCash", "মাইক্যাশ", Color(0xFF0F766E), Color(0xFF14B8A6), "Mercantile Bank", "মার্কেন্টাইল ব্যাংক"),
    ISLAMIC_WALLET("Islamic Wallet", "ইসলামিক ওয়ালেট", Color(0xFF166534), Color(0xFF22C55E), "Islami Bank", "ইসলামী ব্যাংক");

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en

    fun directoryLabel(lang: AppLanguage): String {
        val base = label(lang)
        val parent = if (lang.isBangla) parentBankBn else parentBankEn
        return if (parent.isNotBlank()) "$base ($parent)" else base
    }
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

data class SubcategoryOption(val key: String, val en: String, val bn: String) {
    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
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

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en

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
    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
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
        // Expense Categories (12)
        ManagedCategoryItem(
            id = "exp_food_dining",
            type = TransactionType.EXPENSE,
            en = "Food & Dining",
            bn = "খাবার ও ডাইনিং",
            accentColor = CatFood,
            iconKey = "restaurant",
            subcategories = listOf(
                SubcategoryOption("Restaurant", "Restaurant", "রেস্টুরেন্ট"),
                SubcategoryOption("Coffee & Tea", "Coffee & Tea", "চা ও কফি"),
                SubcategoryOption("Snacks", "Meals & Snacks", "নাস্তা ও খাবার")
            ),
            legacyCategory = ExpenseCategory.FOOD_DINING
        ),
        ManagedCategoryItem(
            id = "exp_groceries",
            type = TransactionType.EXPENSE,
            en = "Groceries",
            bn = "বাজার ও গ্রোসারি",
            accentColor = CatGroceries,
            iconKey = "groceries",
            subcategories = listOf(
                SubcategoryOption("Super Shop", "Super Shop", "সুপার শপ"),
                SubcategoryOption("Kacha Bazar", "Kacha Bazar", "কাঁচাবাজার"),
                SubcategoryOption("Fish & Meat", "Fish & Meat", "মাছ ও মাংস")
            ),
            legacyCategory = ExpenseCategory.GROCERIES
        ),
        ManagedCategoryItem(
            id = "exp_housing_rent",
            type = TransactionType.EXPENSE,
            en = "Housing & Rent",
            bn = "বাসা ভাড়া ও আবাসন",
            accentColor = CatRent,
            iconKey = "home",
            subcategories = listOf(
                SubcategoryOption("House Rent", "House Rent", "বাসা ভাড়া"),
                SubcategoryOption("Service Charge", "Service Charge", "সার্ভিস চার্জ"),
                SubcategoryOption("Maintenance", "Home Maintenance", "বাসা মেরামত")
            ),
            legacyCategory = ExpenseCategory.RENT_UTILITIES
        ),
        ManagedCategoryItem(
            id = "exp_utilities_bills",
            type = TransactionType.EXPENSE,
            en = "Utilities & Bills",
            bn = "ইউটিলিটি ও বিল",
            accentColor = Color(0xFF0284C7),
            iconKey = "bills",
            subcategories = listOf(
                SubcategoryOption("Electricity", "Electricity Bill", "বিদ্যুৎ বিল"),
                SubcategoryOption("Gas & Water", "Gas & Water", "গ্যাস ও পানি"),
                SubcategoryOption("Internet & Mobile", "Internet & Recharge", "ইন্টারনেট ও রিচার্জ")
            ),
            legacyCategory = ExpenseCategory.RENT_UTILITIES
        ),
        ManagedCategoryItem(
            id = "exp_transport",
            type = TransactionType.EXPENSE,
            en = "Transport & Commute",
            bn = "যাতায়াত ও পরিবহন",
            accentColor = CatTransport,
            iconKey = "transport",
            subcategories = listOf(
                SubcategoryOption("Ride Share", "Ride Share", "রাইড শেয়ার"),
                SubcategoryOption("Metro & Bus", "Metro & Bus", "মেট্রো ও বাস"),
                SubcategoryOption("Fuel", "Fuel / CNG", "জ্বালানি")
            ),
            legacyCategory = ExpenseCategory.TRANSPORT
        ),
        ManagedCategoryItem(
            id = "exp_family_remittance",
            type = TransactionType.EXPENSE,
            en = "Family & Remittance",
            bn = "পরিবার ও রেমিট্যান্স",
            accentColor = Color(0xFF0D9488),
            iconKey = "payments",
            subcategories = listOf(
                SubcategoryOption("Home Remittance", "Home Remittance", "দেশে টাকা পাঠানো"),
                SubcategoryOption("Parents & Family", "Parents & Family", "পরিবার খরচ"),
                SubcategoryOption("Education", "Family Education", "পড়াশোনা")
            ),
            legacyCategory = ExpenseCategory.OTHER
        ),
        ManagedCategoryItem(
            id = "exp_health",
            type = TransactionType.EXPENSE,
            en = "Health & Medical",
            bn = "স্বাস্থ্য ও চিকিৎসা",
            accentColor = CatHealth,
            iconKey = "health",
            subcategories = listOf(
                SubcategoryOption("Medicine", "Medicine", "ঔষধ"),
                SubcategoryOption("Doctor", "Doctor Visit", "ডাক্তার ফি"),
                SubcategoryOption("Lab & Test", "Hospital & Lab", "হাসপাতাল ও টেস্ট")
            ),
            legacyCategory = ExpenseCategory.HEALTH
        ),
        ManagedCategoryItem(
            id = "exp_shopping",
            type = TransactionType.EXPENSE,
            en = "Shopping & Apparel",
            bn = "কেনাকাটা ও পোশাক",
            accentColor = CatShopping,
            iconKey = "shopping",
            subcategories = listOf(
                SubcategoryOption("Clothing", "Clothing", "পোশাক"),
                SubcategoryOption("Electronics", "Electronics", "ইলেকট্রনিক্স"),
                SubcategoryOption("Home Care", "Home Care", "ঘরের সামগ্রী")
            ),
            legacyCategory = ExpenseCategory.SHOPPING
        ),
        ManagedCategoryItem(
            id = "exp_subscriptions",
            type = TransactionType.EXPENSE,
            en = "Subscriptions & Digital",
            bn = "সাবস্ক্রিপশন ও ডিজিটাল সেবা",
            accentColor = CatEntertainment,
            iconKey = "entertainment",
            subcategories = listOf(
                SubcategoryOption("Streaming", "Streaming & Apps", "স্ট্রিমিং ও অ্যাপ"),
                SubcategoryOption("Cloud & Software", "Cloud & Software", "ক্লাউড ও সফটওয়্যার"),
                SubcategoryOption("Gaming", "Games & Media", "গেমস ও মিডিয়া")
            ),
            legacyCategory = ExpenseCategory.ENTERTAINMENT
        ),
        ManagedCategoryItem(
            id = "exp_travel",
            type = TransactionType.EXPENSE,
            en = "Travel & Vacation",
            bn = "ভ্রমণ ও ছুটি",
            accentColor = Color(0xFF4F46E5),
            iconKey = "travel",
            subcategories = listOf(
                SubcategoryOption("Flights & Tickets", "Flights & Tickets", "ফ্লাইট ও টিকেট"),
                SubcategoryOption("Hotel & Stay", "Hotel & Resort", "হোটেল ও রিসোর্ট"),
                SubcategoryOption("Tour", "Tour & Outing", "ট্যুর ও ভ্রমণ")
            ),
            legacyCategory = ExpenseCategory.ENTERTAINMENT
        ),
        ManagedCategoryItem(
            id = "exp_charity_gifts",
            type = TransactionType.EXPENSE,
            en = "Charity & Gifts",
            bn = "দান ও উপহার",
            accentColor = Color(0xFFDB2777),
            iconKey = "gift",
            subcategories = listOf(
                SubcategoryOption("Charity & Zakat", "Charity & Zakat", "দান ও যাকাত"),
                SubcategoryOption("Gift", "Gifts & Events", "উপহার ও অনুষ্ঠান")
            ),
            legacyCategory = ExpenseCategory.OTHER
        ),
        ManagedCategoryItem(
            id = "exp_other",
            type = TransactionType.EXPENSE,
            en = "Other Expense",
            bn = "অন্যান্য খরচ",
            accentColor = SlateOcean,
            iconKey = "other",
            subcategories = listOf(
                SubcategoryOption("Debt Repayment", "Debt Repayment", "দেনা পরিশোধ"),
                SubcategoryOption("Miscellaneous", "Miscellaneous", "বিবিধ খরচ")
            ),
            legacyCategory = ExpenseCategory.OTHER
        ),

        // Income Categories (5)
        ManagedCategoryItem(
            id = "inc_salary",
            type = TransactionType.INCOME,
            en = "Salary & Allowance",
            bn = "বেতন ও ভাতা",
            accentColor = CatIncome,
            iconKey = "payments",
            subcategories = listOf(
                SubcategoryOption("Monthly Salary", "Monthly Salary", "মাসিক বেতন"),
                SubcategoryOption("Allowance", "Allowance & Overtime", "ভাতা ও ওভারটাইম")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_bonus",
            type = TransactionType.INCOME,
            en = "Bonus & Incentives",
            bn = "বোনাস ও ইনসেনটিভ",
            accentColor = Color(0xFFD97706),
            iconKey = "gift",
            subcategories = listOf(
                SubcategoryOption("Festival Bonus", "Festival Bonus", "উৎসব বোনাস"),
                SubcategoryOption("Incentive", "Performance Incentive", "ইনসেনটিভ ও কমিশন")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_business",
            type = TransactionType.INCOME,
            en = "Business & Side Income",
            bn = "ব্যবসা ও সাইড ইনকাম",
            accentColor = Color(0xFF0D9488),
            iconKey = "business",
            subcategories = listOf(
                SubcategoryOption("Sales Profit", "Business Profit", "ব্যবসায়িক মুনাফা"),
                SubcategoryOption("Freelance", "Freelance & Side Gig", "ফ্রিল্যান্সিং ও সাইড ইনকাম")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_refund",
            type = TransactionType.INCOME,
            en = "Refund & Cashback",
            bn = "রিফান্ড ও ক্যাশব্যাক",
            accentColor = Color(0xFF0284C7),
            iconKey = "trending_up",
            subcategories = listOf(
                SubcategoryOption("Refund", "Order Refund", "রিফান্ড"),
                SubcategoryOption("Cashback", "Cashback & Reward", "ক্যাশব্যাক ও রিওয়ার্ড")
            ),
            legacyCategory = ExpenseCategory.SALARY_INCOME
        ),
        ManagedCategoryItem(
            id = "inc_other",
            type = TransactionType.INCOME,
            en = "Other Income",
            bn = "অন্যান্য আয়",
            accentColor = Color(0xFF7C3AED),
            iconKey = "other",
            subcategories = listOf(
                SubcategoryOption("Gift Received", "Cash Gift", "নগদ উপহার"),
                SubcategoryOption("Miscellaneous Income", "Other Income", "বিবিধ আয়")
            ),
            legacyCategory = ExpenseCategory.OTHER
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

    fun mergedAll(
        overrides: List<ManagedCategoryItem>,
        deletedIds: Set<String>
    ): List<ManagedCategoryItem> =
        mergedFor(TransactionType.EXPENSE, overrides, deletedIds) +
            mergedFor(TransactionType.INCOME, overrides, deletedIds) +
            mergedFor(TransactionType.TRANSFER, overrides, deletedIds)

    fun resolveForLegacy(
        type: TransactionType,
        legacyCategory: ExpenseCategory,
        mergedItems: List<ManagedCategoryItem>
    ): ManagedCategoryItem? {
        val typed = mergedItems.filter { it.type == type && it.isActive }
        if (typed.isEmpty()) return null
        return typed.firstOrNull { it.legacyCategory == legacyCategory }
            ?: typed.firstOrNull { it.id == if (type == TransactionType.INCOME) "inc_other" else "exp_other" }
            ?: typed.first()
    }

    fun createCustomCategory(
        type: TransactionType,
        name: String,
        accentColor: Color = CategoryEditorCatalog.colorSwatches.first(),
        iconKey: String = if (type == TransactionType.INCOME) "payments" else "other"
    ): ManagedCategoryItem {
        val clean = name.trim()
        val slug = clean
            .lowercase()
            .replace(Regex("[^a-z0-9\\u0980-\\u09FF]+"), "_")
            .trim('_')
            .take(24)
            .ifBlank { System.currentTimeMillis().toString() }
        val id = "custom_${type.name.lowercase()}_${slug}_${System.currentTimeMillis() % 10000}"
        val fallbackLegacy = when (type) {
            TransactionType.INCOME -> ExpenseCategory.SALARY_INCOME
            TransactionType.TRANSFER -> ExpenseCategory.TRANSFER
            TransactionType.EXPENSE -> ExpenseCategory.OTHER
        }
        return ManagedCategoryItem(
            id = id,
            type = type,
            en = clean,
            bn = clean,
            accentColor = accentColor,
            iconKey = iconKey,
            subcategories = emptyList(),
            isBuiltIn = false,
            isActive = true,
            legacyCategory = fallbackLegacy
        )
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

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

enum class TransactionSortOption(val en: String, val bn: String) {
    NEWEST("Newest", "নতুন আগে"),
    OLDEST("Oldest", "পুরাতন আগে"),
    HIGHEST("Highest ৳", "বেশি টাকা"),
    LOWEST("Lowest ৳", "কম টাকা");

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

enum class StartOfWeek(val calendarDay: Int, val en: String, val bn: String) {
    SATURDAY(Calendar.SATURDAY, "Saturday", "শনিবার"),
    SUNDAY(Calendar.SUNDAY, "Sunday", "রবিবার"),
    MONDAY(Calendar.MONDAY, "Monday", "সোমবার"),
    TUESDAY(Calendar.TUESDAY, "Tuesday", "মঙ্গলবার"),
    WEDNESDAY(Calendar.WEDNESDAY, "Wednesday", "বুধবার"),
    THURSDAY(Calendar.THURSDAY, "Thursday", "বৃহস্পতিবার"),
    FRIDAY(Calendar.FRIDAY, "Friday", "শুক্রবার");

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

enum class AppThemeMode(val en: String, val bn: String) {
    AUTO("Auto", "অটো"),
    LIGHT("Light", "লাইট"),
    DARK("Dark", "ডার্ক");

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
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

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
    fun subtitle(lang: AppLanguage): String = if (lang.isBangla) subtitleBn else subtitleEn
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

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
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
    val isLocalFallback: Boolean = false,
    val managedCategoryId: String = "",
    val managedCategoryLabel: String = ""
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
    val isLocalFallback: Boolean = false,
    val managedCategoryId: String = "",
    val managedCategoryLabel: String = ""
)

fun defaultColorForCash(): Color = BrandCash
