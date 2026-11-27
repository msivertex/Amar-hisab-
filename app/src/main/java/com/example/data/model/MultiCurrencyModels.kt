package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.market.MarketCurrency
import com.example.localization.AppLanguage
import com.example.ui.theme.BrandBracBank
import com.example.ui.theme.BrandCityBank
import com.example.ui.theme.BrandDbbl
import com.example.ui.theme.BrandIslamiBank
import com.example.ui.theme.BrandSonaliBank
import com.example.ui.theme.BrandTrustBank

/**
 * Supported primary, secondary, and card currencies synchronized 1:1 with [WorldCountryCatalog]
 * and [AppCountry] across all 195 sovereign countries for live & offline exchange conversions.
 */
data class AppCurrency(
    val code: String,
    val symbol: String,
    val flag: String,
    val countryEn: String,
    val countryBn: String,
    val nameEn: String,
    val nameBn: String,
    val usesIndianGrouping: Boolean = false,
    val decimalPlaces: Int = 2,
    val defaultPerUsd: Double = 1.0
) {
    /** Backward-compatible alias matching enum `.name`. */
    val name: String get() = code

    fun countryName(lang: AppLanguage): String = if (lang.isBangla) countryBn else countryEn
    fun currencyName(lang: AppLanguage): String = if (lang.isBangla) nameBn else nameEn

    /** Non-blank currency symbol when distinct from the ISO code (e.g., "د.ك", "৳", "₹", "Rs"). */
    fun distinctSymbolOrNull(): String? =
        symbol.trim().takeIf { it.isNotEmpty() && !it.equals(code, ignoreCase = true) }

    /** Uniform code-only currency label (e.g., "KWD", "BDT", "INR", "LKR"). */
    fun codeAndSymbolLabel(): String = code

    fun shortBadge(): String = "$flag $code"

    /** Clean uniform ISO badge label for cards & headers (always fixed ISO code, e.g., "KWD", "BDT", "INR", "LKR"). */
    fun cleanBadgeLabel(): String = code

    /** Uniform Country Name + Currency Code display across pickers & selectors (symbol-free). */
    fun pickerDisplayLabel(lang: AppLanguage, overrideCountryName: String? = null): String {
        val country = overrideCountryName?.takeIf { it.isNotBlank() } ?: countryName(lang)
        return "$country • $code"
    }

    fun fullDisplay(lang: AppLanguage): String = "$flag ${pickerDisplayLabel(lang)}"

    fun toMarketCurrency(): MarketCurrency =
        MarketCurrency.entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MarketCurrency.USD

    companion object {
        val entries: List<AppCurrency> = WorldCountryCatalog.currencySpecs.map { spec ->
            AppCurrency(
                code = spec.code,
                symbol = spec.symbol,
                flag = spec.flag,
                countryEn = spec.countryEn,
                countryBn = spec.countryBn,
                nameEn = spec.nameEn,
                nameBn = spec.nameBn,
                usesIndianGrouping = spec.usesIndianGrouping,
                decimalPlaces = spec.decimalPlaces,
                defaultPerUsd = spec.defaultPerUsd
            )
        }

        private val byCode: Map<String, AppCurrency> = entries.associateBy { it.code.uppercase() }

        // Named constants preserving 100% compatibility with all existing callers and tests
        val BDT: AppCurrency get() = byCode.getValue("BDT")
        val INR: AppCurrency get() = byCode.getValue("INR")
        val NPR: AppCurrency get() = byCode.getValue("NPR")
        val LKR: AppCurrency get() = byCode.getValue("LKR")
        val BTN: AppCurrency get() = byCode.getValue("BTN")
        val MMK: AppCurrency get() = byCode.getValue("MMK")
        val MVR: AppCurrency get() = byCode.getValue("MVR")
        val SAR: AppCurrency get() = byCode.getValue("SAR")
        val AED: AppCurrency get() = byCode.getValue("AED")
        val KWD: AppCurrency get() = byCode.getValue("KWD")
        val QAR: AppCurrency get() = byCode.getValue("QAR")
        val OMR: AppCurrency get() = byCode.getValue("OMR")
        val BHD: AppCurrency get() = byCode.getValue("BHD")
        val MYR: AppCurrency get() = byCode.getValue("MYR")
        val SGD: AppCurrency get() = byCode.getValue("SGD")
        val KRW: AppCurrency get() = byCode.getValue("KRW")
        val JPY: AppCurrency get() = byCode.getValue("JPY")
        val AUD: AppCurrency get() = byCode.getValue("AUD")
        val USD: AppCurrency get() = byCode.getValue("USD")
        val GBP: AppCurrency get() = byCode.getValue("GBP")
        val CAD: AppCurrency get() = byCode.getValue("CAD")
        val EUR: AppCurrency get() = byCode.getValue("EUR")

        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return BDT
            return byCode[code.trim().uppercase()] ?: BDT
        }

        fun fromCodeOrNull(code: String?): AppCurrency? {
            if (code.isNullOrBlank()) return null
            return byCode[code.trim().uppercase()]
        }
    }
}

/**
 * Regional groupings shared by Profile Details > Country, Main Currency, Second Currency,
 * and the Card / New Account Wizard across all 195 UN-recognized countries.
 */
enum class CountryRegionGroup(val en: String, val bn: String) {
    SOUTH_ASIA("South Asia & Neighboring Countries", "দক্ষিণ এশিয়া ও প্রতিবেশী দেশসমূহ"),
    MIDDLE_EAST("Middle East", "মধ্যপ্রাচ্য"),
    ASIA_PACIFIC_OCEANIA("Asia-Pacific & Oceania", "এশিয়া-প্যাসিফিক ও ওশেনিয়া"),
    NORTH_AMERICA_UK("North America & UK", "উত্তর আমেরিকা ও যুক্তরাজ্য"),
    EUROPE_CYPRUS("Italy, Cyprus & European Countries", "ইতালি, সাইপ্রাস ও ইউরোপীয় দেশসমূহ"),
    LATIN_AMERICA_CARIBBEAN("Latin America & Caribbean", "লাতিন আমেরিকা ও ক্যারিবিয়ান"),
    AFRICA("Africa", "আফ্রিকা");

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

/**
 * Unified single source of truth for ALL 195 sovereign countries & currencies across:
 * 1. Profile Details > Country
 * 2. Profile Details > Main Currency
 * 3. Profile Details > Second Currency
 * 4. Card / New Account Wizard country & bank selection
 *
 * Backed directly by [WorldCountryCatalog.all195CountrySpecs] so every screen stays automatically synchronized.
 */
data class AppCountry(
    val code: String,
    val flag: String,
    val nameEn: String,
    val nameBn: String,
    val suggestedCurrency: AppCurrency,
    val group: CountryRegionGroup
) {
    /** Backward-compatible alias matching enum `.name`. */
    val name: String get() = code

    fun label(lang: AppLanguage): String = if (lang.isBangla) nameBn else nameEn

    /**
     * Dynamic localized heading displayed above the bank list in the Card / New Account Wizard.
     */
    fun bankSelectionHeading(lang: AppLanguage): String = when (code) {
        "BD" -> if (lang.isBangla) "বাংলাদেশী ব্যাংক নির্বাচন করুন" else "Select Bangladeshi Bank"
        "KW" -> if (lang.isBangla) "কুয়েত ব্যাংক নির্বাচন করুন" else "Select Kuwait Bank"
        "SA" -> if (lang.isBangla) "সৌদি আরব ব্যাংক নির্বাচন করুন" else "Select Saudi Arabia Bank"
        "AE" -> if (lang.isBangla) "সংযুক্ত আরব আমিরাত ব্যাংক নির্বাচন করুন" else "Select UAE Bank"
        "QA" -> if (lang.isBangla) "কাতার ব্যাংক নির্বাচন করুন" else "Select Qatar Bank"
        "OM" -> if (lang.isBangla) "ওমান ব্যাংক নির্বাচন করুন" else "Select Oman Bank"
        "BH" -> if (lang.isBangla) "বাহরাইন ব্যাংক নির্বাচন করুন" else "Select Bahrain Bank"
        "MY" -> if (lang.isBangla) "মালয়েশিয়া ব্যাংক নির্বাচন করুন" else "Select Malaysia Bank"
        "SG" -> if (lang.isBangla) "সিঙ্গাপুর ব্যাংক নির্বাচন করুন" else "Select Singapore Bank"
        "US" -> if (lang.isBangla) "যুক্তরাষ্ট্র ব্যাংক নির্বাচন করুন" else "Select USA Bank"
        "GB" -> if (lang.isBangla) "যুক্তরাজ্য ব্যাংক নির্বাচন করুন" else "Select UK Bank"
        "CA" -> if (lang.isBangla) "কানাডা ব্যাংক নির্বাচন করুন" else "Select Canada Bank"
        "IT" -> if (lang.isBangla) "ইতালি ব্যাংক নির্বাচন করুন" else "Select Italy Bank"
        "CY" -> if (lang.isBangla) "সাইপ্রাস ব্যাংক নির্বাচন করুন" else "Select Cyprus Bank"
        "AU" -> if (lang.isBangla) "অস্ট্রেলিয়া ব্যাংক নির্বাচন করুন" else "Select Australia Bank"
        "JP" -> if (lang.isBangla) "জাপান ব্যাংক নির্বাচন করুন" else "Select Japan Bank"
        "KR" -> if (lang.isBangla) "দক্ষিণ কোরিয়া ব্যাংক নির্বাচন করুন" else "Select South Korea Bank"
        "MV" -> if (lang.isBangla) "মালদ্বীপ ব্যাংক নির্বাচন করুন" else "Select Maldives Bank"
        else -> if (lang.isBangla) "${label(lang)} ব্যাংক নির্বাচন করুন" else "Select ${label(lang)} Bank"
    }

    /** Formats the country + currency line cleanly, e.g. `Bangladesh • Bangladeshi Taka (BDT)`. */
    fun countryCurrencyDisplay(lang: AppLanguage): String {
        val cur = suggestedCurrency
        return "${label(lang)} • ${cur.currencyName(lang)} (${cur.code})"
    }

    companion object {
        val entries: List<AppCountry> = WorldCountryCatalog.all195CountrySpecs.map { spec ->
            AppCountry(
                code = spec.code,
                flag = spec.flag,
                nameEn = spec.nameEn,
                nameBn = spec.nameBn,
                suggestedCurrency = AppCurrency.fromCode(spec.currencyCode),
                group = spec.group
            )
        }

        private val byCode: Map<String, AppCountry> = entries.associateBy { it.code.uppercase() }

        // Named constants for all key expatriate & neighboring countries
        val BD: AppCountry get() = byCode.getValue("BD")
        val IN: AppCountry get() = byCode.getValue("IN")
        val NP: AppCountry get() = byCode.getValue("NP")
        val LK: AppCountry get() = byCode.getValue("LK")
        val BT: AppCountry get() = byCode.getValue("BT")
        val MM: AppCountry get() = byCode.getValue("MM")
        val MV: AppCountry get() = byCode.getValue("MV")
        val SA: AppCountry get() = byCode.getValue("SA")
        val AE: AppCountry get() = byCode.getValue("AE")
        val KW: AppCountry get() = byCode.getValue("KW")
        val QA: AppCountry get() = byCode.getValue("QA")
        val OM: AppCountry get() = byCode.getValue("OM")
        val BH: AppCountry get() = byCode.getValue("BH")
        val MY: AppCountry get() = byCode.getValue("MY")
        val SG: AppCountry get() = byCode.getValue("SG")
        val KR: AppCountry get() = byCode.getValue("KR")
        val JP: AppCountry get() = byCode.getValue("JP")
        val AU: AppCountry get() = byCode.getValue("AU")
        val US: AppCountry get() = byCode.getValue("US")
        val GB: AppCountry get() = byCode.getValue("GB")
        val CA: AppCountry get() = byCode.getValue("CA")
        val IT: AppCountry get() = byCode.getValue("IT")
        val ES: AppCountry get() = byCode.getValue("ES")
        val PT: AppCountry get() = byCode.getValue("PT")
        val GR: AppCountry get() = byCode.getValue("GR")
        val DE: AppCountry get() = byCode.getValue("DE")
        val FR: AppCountry get() = byCode.getValue("FR")
        val CY: AppCountry get() = byCode.getValue("CY")

        /**
         * Prominently featured expatriate & neighboring countries shown as fast 1-tap quick chips
         * in the Card / New Account Wizard while the full searchable 195-country picker sits right above.
         */
        val quickSelectCountries: List<AppCountry> by lazy {
            listOf(
                BD, SA, AE, KW, QA, OM, BH, MY, SG,
                US, GB, CA, IT, DE, FR, ES, PT, GR, CY,
                AU, JP, KR, IN, NP, LK, BT, MM, MV
            )
        }

        fun fromCode(code: String?): AppCountry {
            if (code.isNullOrBlank()) return BD
            val normalized = code.trim()
            return byCode[normalized.uppercase()]
                ?: entries.firstOrNull {
                    it.nameEn.equals(normalized, ignoreCase = true) ||
                        it.nameBn.equals(normalized, ignoreCase = true)
                }
                ?: BD
        }

        fun preferredForCurrency(currency: AppCurrency): AppCountry =
            entries.firstOrNull { it.suggestedCurrency.code.equals(currency.code, ignoreCase = true) } ?: BD

        fun grouped(): Map<CountryRegionGroup, List<AppCountry>> =
            CountryRegionGroup.entries.associateWith { grp -> entries.filter { it.group == grp } }
    }
}

/**
 * Legacy banking region enum kept for backward compatibility with older unit tests.
 * New UI flows (Card / New Account Wizard) read directly from [AppCountry] and
 * [InternationalBankCatalog.banksForCountry] so countries stay automatically synchronized.
 */
enum class BankRegion(
    val flag: String,
    val en: String,
    val bn: String,
    val defaultCurrency: AppCurrency,
    val primaryCountry: AppCountry
) {
    BANGLADESH("🇧🇩", "Bangladesh", "বাংলাদেশ", AppCurrency.BDT, AppCountry.BD),
    SAUDI_ARABIA("🇸🇦", "Saudi Arabia", "সৌদি আরব", AppCurrency.SAR, AppCountry.SA),
    QATAR("🇶🇦", "Qatar", "কাতার", AppCurrency.QAR, AppCountry.QA),
    KUWAIT("🇰🇼", "Kuwait", "কুয়েত", AppCurrency.KWD, AppCountry.KW),
    UAE("🇦🇪", "UAE (Dubai)", "দুবাই (আমিরাত)", AppCurrency.AED, AppCountry.AE),
    MALAYSIA("🇲🇾", "Malaysia", "মালয়েশিয়া", AppCurrency.MYR, AppCountry.MY),
    ITALY("🇮🇹", "Italy", "ইতালি", AppCurrency.EUR, AppCountry.IT),
    EUROPE("🇪🇺", "Europe", "ইউরোপ", AppCurrency.EUR, AppCountry.DE),
    UK("🇬🇧", "UK (England)", "যুক্তরাজ্য (ইংল্যান্ড)", AppCurrency.GBP, AppCountry.GB);

    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en

    companion object {
        fun preferredForCurrency(currency: AppCurrency): BankRegion = when (currency) {
            AppCurrency.BDT -> BANGLADESH
            AppCurrency.SAR -> SAUDI_ARABIA
            AppCurrency.QAR -> QATAR
            AppCurrency.KWD -> KUWAIT
            AppCurrency.AED -> UAE
            AppCurrency.MYR -> MALAYSIA
            AppCurrency.EUR -> ITALY
            AppCurrency.GBP -> UK
            else -> BANGLADESH
        }
    }
}

data class InternationalBank(
    val id: String,
    val country: AppCountry,
    val en: String,
    val bn: String,
    val shortCode: String,
    val brandColor: Color,
    val defaultCurrency: AppCurrency = country.suggestedCurrency,
    val category: BankCategory = BankCategory.PRIVATE
) {
    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en
}

/**
 * Curated bank-card colors available for manual/custom banks and card customization in Live Card Preview.
 */
object CardColorCatalog {
    val swatches: List<Color> = listOf(
        Color(0xFF0284C7), // Sapphire Ocean
        Color(0xFF006A4E), // Emerald Vault
        Color(0xFF1E3A8A), // Midnight Navy
        Color(0xFFB91C1C), // Crimson Ruby
        Color(0xFF4F46E5), // Royal Indigo
        Color(0xFF0F766E), // Deep Teal
        Color(0xFFD97706), // Sovereign Gold
        Color(0xFF7C3AED), // Amethyst Violet
        Color(0xFF1F2937), // Obsidian Charcoal
        Color(0xFFBE185D)  // Rose Magenta
    )
}

object InternationalBankCatalog {
    private val sharedMajorEuropeanBanks: List<InternationalBank> = listOf(
        InternationalBank("eu_deutsche", AppCountry.DE, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("eu_bnp", AppCountry.FR, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),
        InternationalBank("eu_santander", AppCountry.ES, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626)),
        InternationalBank("eu_ing", AppCountry.DE, "ING", "আইএনজি ব্যাংক", "ING", Color(0xFFEA580C))
    )

    val all: List<InternationalBank> = listOf(
        // Bangladesh (BDT) - All 22 Government (5) & Private (17) Banks preserved intact
        InternationalBank("bd_city", AppCountry.BD, "City Bank", "সিটি ব্যাংক", "CITY", BrandCityBank, category = BankCategory.PRIVATE),
        InternationalBank("bd_dbbl", AppCountry.BD, "Dutch-Bangla Bank", "ডাচ-বাংলা ব্যাংক", "DBBL", BrandDbbl, category = BankCategory.PRIVATE),
        InternationalBank("bd_brac", AppCountry.BD, "BRAC Bank", "ব্র্যাক ব্যাংক", "BRAC", BrandBracBank, category = BankCategory.PRIVATE),
        InternationalBank("bd_islami", AppCountry.BD, "Islami Bank", "ইসলামী ব্যাংক", "IBBL", BrandIslamiBank, category = BankCategory.PRIVATE),
        InternationalBank("bd_trust", AppCountry.BD, "Trust Bank", "ট্রাস্ট ব্যাংক", "TRUST", BrandTrustBank, category = BankCategory.PRIVATE),
        InternationalBank("bd_sonali", AppCountry.BD, "Sonali Bank", "সোনালী ব্যাংক", "SONALI", BrandSonaliBank, category = BankCategory.GOVERNMENT),
        InternationalBank("bd_janata", AppCountry.BD, "Janata Bank", "জনতা ব্যাংক", "JANATA", BangladeshiBank.JANATA_BANK.brandColor, category = BankCategory.GOVERNMENT),
        InternationalBank("bd_agrani", AppCountry.BD, "Agrani Bank", "অগ্রণী ব্যাংক", "AGRANI", BangladeshiBank.AGRANI_BANK.brandColor, category = BankCategory.GOVERNMENT),
        InternationalBank("bd_rupali", AppCountry.BD, "Rupali Bank", "রূপালী ব্যাংক", "RUPALI", BangladeshiBank.RUPALI_BANK.brandColor, category = BankCategory.GOVERNMENT),
        InternationalBank("bd_bkb", AppCountry.BD, "Bangladesh Krishi Bank", "বাংলাদেশ কৃষি ব্যাংক", "BKB", BangladeshiBank.BKB.brandColor, category = BankCategory.GOVERNMENT),
        InternationalBank("bd_ebl", AppCountry.BD, "Eastern Bank", "ইস্টার্ন ব্যাংক", "EBL", BangladeshiBank.EBL.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_prime", AppCountry.BD, "Prime Bank", "প্রাইম ব্যাংক", "PRIME", BangladeshiBank.PRIME_BANK.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_mtb", AppCountry.BD, "Mutual Trust Bank", "মিউচুয়াল ট্রাস্ট ব্যাংক", "MTB", BangladeshiBank.MTB.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_dhaka", AppCountry.BD, "Dhaka Bank", "ঢাকা ব্যাংক", "DHAKA", BangladeshiBank.DHAKA_BANK.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_pubali", AppCountry.BD, "Pubali Bank", "পূবালী ব্যাংক", "PUBALI", BangladeshiBank.PUBALI_BANK.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_bankasia", AppCountry.BD, "Bank Asia", "ব্যাংক এশিয়া", "BANKASIA", BangladeshiBank.BANK_ASIA.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_sebl", AppCountry.BD, "Southeast Bank", "সাউথইস্ট ব্যাংক", "SEBL", BangladeshiBank.SEBL.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_one", AppCountry.BD, "One Bank", "ওয়ান ব্যাংক", "ONE", BangladeshiBank.ONE_BANK.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_uttara", AppCountry.BD, "Uttara Bank", "উত্তরা ব্যাংক", "UTTARA", BangladeshiBank.UTTARA_BANK.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_jamuna", AppCountry.BD, "Jamuna Bank", "যমুনা ব্যাংক", "JAMUNA", BangladeshiBank.JAMUNA_BANK.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_ucb", AppCountry.BD, "United Commercial Bank", "ইউনাইটেড কমার্শিয়াল ব্যাংক", "UCB", BangladeshiBank.UCB.brandColor, category = BankCategory.PRIVATE),
        InternationalBank("bd_mbl", AppCountry.BD, "Mercantile Bank", "মার্কেন্টাইল ব্যাংক", "MBL", BangladeshiBank.MBL.brandColor, category = BankCategory.PRIVATE),

        // South Asia & Neighboring Countries
        InternationalBank("in_sbi", AppCountry.IN, "State Bank of India (SBI)", "স্টেট ব্যাংক অফ ইন্ডিয়া (SBI)", "SBI", Color(0xFF0284C7), category = BankCategory.GOVERNMENT),
        InternationalBank("in_pnb", AppCountry.IN, "Punjab National Bank (PNB)", "পাঞ্জাব ন্যাশনাল ব্যাংক (PNB)", "PNB", Color(0xFF9F1239), category = BankCategory.GOVERNMENT),
        InternationalBank("in_bob", AppCountry.IN, "Bank of Baroda (BoB)", "ব্যাংক অফ বরোদা (BoB)", "BOB", Color(0xFFEA580C), category = BankCategory.GOVERNMENT),
        InternationalBank("in_hdfc", AppCountry.IN, "HDFC Bank", "এইচডিএফসি ব্যাংক", "HDFC", Color(0xFF1E3A8A), category = BankCategory.PRIVATE),
        InternationalBank("in_icici", AppCountry.IN, "ICICI Bank", "আইসিআইসিআই ব্যাংক", "ICICI", Color(0xFFEA580C), category = BankCategory.PRIVATE),
        InternationalBank("in_axis", AppCountry.IN, "Axis Bank", "অ্যাক্সিস ব্যাংক", "AXIS", Color(0xFF9D174D), category = BankCategory.PRIVATE),
        InternationalBank("in_kotak", AppCountry.IN, "Kotak Mahindra Bank", "কোটাক মাহিন্দ্রা ব্যাংক", "KOTAK", Color(0xFFDC2626), category = BankCategory.PRIVATE),
        InternationalBank("in_indusind", AppCountry.IN, "IndusInd Bank", "ইন্ডাসইন্ড ব্যাংক", "INDUS", Color(0xFF7F1D1D), category = BankCategory.PRIVATE),

        InternationalBank("pk_nbp", AppCountry.fromCode("PK"), "National Bank of Pakistan (NBP)", "ন্যাশনাল ব্যাংক অফ পাকিস্তান (NBP)", "NBP", Color(0xFF15803D), category = BankCategory.GOVERNMENT),
        InternationalBank("pk_hbl", AppCountry.fromCode("PK"), "Habib Bank (HBL)", "হাবিব ব্যাংক (HBL)", "HBL", Color(0xFF047857), category = BankCategory.PRIVATE),
        InternationalBank("pk_ubl", AppCountry.fromCode("PK"), "United Bank (UBL)", "ইউনাইটেড ব্যাংক (UBL)", "UBL", Color(0xFF0284C7), category = BankCategory.PRIVATE),
        InternationalBank("pk_mcb", AppCountry.fromCode("PK"), "MCB Bank", "এমসিবি ব্যাংক", "MCB", Color(0xFF1E3A8A), category = BankCategory.PRIVATE),
        InternationalBank("pk_meezan", AppCountry.fromCode("PK"), "Meezan Bank", "মিজান ব্যাংক", "MEEZAN", Color(0xFF5B21B6), category = BankCategory.PRIVATE),

        InternationalBank("np_rbb", AppCountry.NP, "Rastriya Banijya Bank", "রাষ্ট্রীয় বাণিজ্য ব্যাংক", "RBB", Color(0xFF0369A1), category = BankCategory.GOVERNMENT),
        InternationalBank("np_nabil", AppCountry.NP, "Nabil Bank", "নাবিল ব্যাংক", "NABIL", Color(0xFF15803D), category = BankCategory.PRIVATE),
        InternationalBank("np_nimb", AppCountry.NP, "Nepal Investment Mega Bank", "নেপাল ইনভেস্টমেন্ট মেগা ব্যাংক", "NIMB", Color(0xFF1D4ED8), category = BankCategory.PRIVATE),
        InternationalBank("np_globalime", AppCountry.NP, "Global IME Bank", "গ্লোবাল আইএমই ব্যাংক", "GIME", Color(0xFFB91C1C), category = BankCategory.PRIVATE),

        InternationalBank("lk_boc", AppCountry.LK, "Bank of Ceylon (BOC)", "ব্যাংক অফ সিলন (BOC)", "BOC", Color(0xFFCA8A04), category = BankCategory.GOVERNMENT),
        InternationalBank("lk_peoples", AppCountry.LK, "People's Bank", "পিপলস ব্যাংক", "PBL", Color(0xFFB91C1C), category = BankCategory.GOVERNMENT),
        InternationalBank("lk_combank", AppCountry.LK, "Commercial Bank of Ceylon", "কমার্শিয়াল ব্যাংক অফ সিলন", "COM", Color(0xFF0369A1), category = BankCategory.PRIVATE),
        InternationalBank("lk_sampath", AppCountry.LK, "Sampath Bank", "সম্পথ ব্যাংক", "SAMP", Color(0xFFEA580C), category = BankCategory.PRIVATE),

        InternationalBank("bt_bob", AppCountry.BT, "Bank of Bhutan (BoB)", "ব্যাংক অফ ভুটান (BoB)", "BOB", Color(0xFF0284C7), category = BankCategory.GOVERNMENT),
        InternationalBank("bt_bnb", AppCountry.BT, "Bhutan National Bank (BNB)", "ভুটান ন্যাশনাল ব্যাংক (BNB)", "BNB", Color(0xFF1E40AF), category = BankCategory.PRIVATE),

        InternationalBank("mm_kbz", AppCountry.MM, "KBZ Bank", "কেবিজেড ব্যাংক", "KBZ", Color(0xFF1D4ED8), category = BankCategory.PRIVATE),
        InternationalBank("mm_cb", AppCountry.MM, "CB Bank", "সিবি ব্যাংক", "CB", Color(0xFF0F766E), category = BankCategory.PRIVATE),
        InternationalBank("mm_aya", AppCountry.MM, "AYA Bank", "আয়া ব্যাংক", "AYA", Color(0xFFB91C1C), category = BankCategory.PRIVATE),

        InternationalBank("mv_bml", AppCountry.MV, "Bank of Maldives (BML)", "ব্যাংক অফ মালদ্বীপ (BML)", "BML", Color(0xFFDC2626), category = BankCategory.GOVERNMENT),
        InternationalBank("mv_mib", AppCountry.MV, "Maldives Islamic Bank (MIB)", "মালদ্বীপ ইসলামী ব্যাংক (MIB)", "MIB", Color(0xFF047857), category = BankCategory.PRIVATE),

        // Kuwait (KWD) - All 8 Major Banks
        InternationalBank("kw_nbk", AppCountry.KW, "National Bank of Kuwait (NBK)", "ন্যাশনাল ব্যাংক অফ কুয়েত (NBK)", "NBK", Color(0xFF003B73)),
        InternationalBank("kw_kfh", AppCountry.KW, "Kuwait Finance House (KFH)", "কুয়েত ফিন্যান্স হাউস (KFH)", "KFH", Color(0xFF007A3D)),
        InternationalBank("kw_gulf", AppCountry.KW, "Gulf Bank Kuwait", "গালফ ব্যাংক কুয়েত", "GULF", Color(0xFFB91C1C)),
        InternationalBank("kw_boubyan", AppCountry.KW, "Boubyan Bank", "বুবিয়ান ব্যাংক", "BOUBYAN", Color(0xFF991B1B)),
        InternationalBank("kw_burgan", AppCountry.KW, "Burgan Bank", "বুরগান ব্যাংক", "BURGAN", Color(0xFF0284C7)),
        InternationalBank("kw_abk", AppCountry.KW, "Al Ahli Bank of Kuwait (ABK)", "আল আহলি ব্যাংক অফ কুয়েত (ABK)", "ABK", Color(0xFF0369A1)),
        InternationalBank("kw_cbk", AppCountry.KW, "Commercial Bank of Kuwait (Al Tijari)", "কমার্শিয়াল ব্যাংক অফ কুয়েত (আল তিজারি)", "CBK", Color(0xFF0F766E)),
        InternationalBank("kw_warba", AppCountry.KW, "Warba Bank", "ওয়ারবা ব্যাংক", "WARBA", Color(0xFF0E7490)),

        // Saudi Arabia (SAR) - All 7 Major Banks
        InternationalBank("sa_alrajhi", AppCountry.SA, "Al Rajhi Bank", "আল রাজি ব্যাংক", "RAJHI", Color(0xFF0052A5)),
        InternationalBank("sa_snb", AppCountry.SA, "Saudi National Bank (SNB)", "সৌদি ন্যাশনাল ব্যাংক (SNB)", "SNB", Color(0xFF006C35)),
        InternationalBank("sa_riyad", AppCountry.SA, "Riyad Bank", "রিয়াদ ব্যাংক", "RIYAD", Color(0xFF1D4ED8)),
        InternationalBank("sa_alinma", AppCountry.SA, "Alinma Bank", "আলইনমা ব্যাংক", "INMA", Color(0xFF6B4E3D)),
        InternationalBank("sa_anb", AppCountry.SA, "Arab National Bank (ANB)", "আরব ন্যাশনাল ব্যাংক (ANB)", "ANB", Color(0xFF0369A1)),
        InternationalBank("sa_bsf", AppCountry.SA, "Banque Saudi Fransi (BSF)", "ব্যাংক সৌদি ফ্রান্সি (BSF)", "BSF", Color(0xFF0F766E)),
        InternationalBank("sa_sab", AppCountry.SA, "Saudi British Bank (SAB)", "সৌদি ব্রিটিশ ব্যাংক (SAB)", "SAB", Color(0xFFDC2626)),

        // UAE / Dubai (AED) - All 6 Major Banks
        InternationalBank("ae_enbd", AppCountry.AE, "Emirates NBD", "এমিরেটস এনবিডি", "ENBD", Color(0xFF00338D)),
        InternationalBank("ae_fab", AppCountry.AE, "First Abu Dhabi Bank (FAB)", "ফার্স্ট আবুধাবি ব্যাংক (FAB)", "FAB", Color(0xFF00205B)),
        InternationalBank("ae_dib", AppCountry.AE, "Dubai Islamic Bank (DIB)", "দুবাই ইসলামী ব্যাংক (DIB)", "DIB", Color(0xFF007A4D)),
        InternationalBank("ae_adcb", AppCountry.AE, "Abu Dhabi Commercial Bank (ADCB)", "আবুধাবি কমার্শিয়াল ব্যাংক (ADCB)", "ADCB", Color(0xFF9D174D)),
        InternationalBank("ae_mashreq", AppCountry.AE, "Mashreq Bank", "মাশরেক ব্যাংক", "MASHREQ", Color(0xFFEA580C)),
        InternationalBank("ae_cbd", AppCountry.AE, "Commercial Bank of Dubai (CBD)", "কমার্শিয়াল ব্যাংক অফ দুবাই (CBD)", "CBD", Color(0xFF047857)),

        // Qatar (QAR) - All 5 Major Banks
        InternationalBank("qa_qnb", AppCountry.QA, "Qatar National Bank (QNB)", "কাতার ন্যাশনাল ব্যাংক (QNB)", "QNB", Color(0xFF6B1D4A)),
        InternationalBank("qa_masraf", AppCountry.QA, "Masraf Al Rayan", "মাসরাফ আল রায়ান", "MAR", Color(0xFF047857)),
        InternationalBank("qa_doha", AppCountry.QA, "Doha Bank", "দোহা ব্যাংক", "DOHA", Color(0xFF0E7490)),
        InternationalBank("qa_cbq", AppCountry.QA, "Commercial Bank of Qatar", "কমার্শিয়াল ব্যাংক অফ কাতার", "CBQ", Color(0xFF0F4C81)),
        InternationalBank("qa_qib", AppCountry.QA, "Qatar Islamic Bank (QIB)", "কাতার ইসলামী ব্যাংক (QIB)", "QIB", Color(0xFF0369A1)),

        // Oman (OMR) - All 4 Major Banks
        InternationalBank("om_muscat", AppCountry.OM, "Bank Muscat", "ব্যাংক মাস্কাট", "BMUSCAT", Color(0xFFB91C1C)),
        InternationalBank("om_dhofar", AppCountry.OM, "Bank Dhofar", "ব্যাংক ধোফার", "DHOFAR", Color(0xFF15803D)),
        InternationalBank("om_sohar", AppCountry.OM, "Sohar International", "সোহার ইন্টারন্যাশনাল", "SOHAR", Color(0xFF0284C7)),
        InternationalBank("om_nbo", AppCountry.OM, "National Bank of Oman (NBO)", "ন্যাশনাল ব্যাংক অফ ওমান (NBO)", "NBO", Color(0xFF1E3A8A)),

        // Bahrain (BHD) - All 3 Major Banks
        InternationalBank("bh_nbb", AppCountry.BH, "National Bank of Bahrain (NBB)", "ন্যাশনাল ব্যাংক অফ বাহরাইন (NBB)", "NBB", Color(0xFF0284C7)),
        InternationalBank("bh_bbk", AppCountry.BH, "BBK", "বিবিকে ব্যাংক", "BBK", Color(0xFF1E3A8A)),
        InternationalBank("bh_aub", AppCountry.BH, "Ahli United Bank", "আহলি ইউনাইটেড ব্যাংক", "AUB", Color(0xFF0F766E)),

        // Malaysia (MYR) - All 5 Major Banks
        InternationalBank("my_maybank", AppCountry.MY, "Maybank", "মেব্যাংক", "MAYBANK", Color(0xFFCA8A04)),
        InternationalBank("my_cimb", AppCountry.MY, "CIMB Bank", "সিআইএমবি ব্যাংক", "CIMB", Color(0xFFB91C1C)),
        InternationalBank("my_public", AppCountry.MY, "Public Bank", "পাবলিক ব্যাংক", "PBB", Color(0xFF991B1B)),
        InternationalBank("my_rhb", AppCountry.MY, "RHB Bank", "আরএইচবি ব্যাংক", "RHB", Color(0xFF0284C7)),
        InternationalBank("my_hlb", AppCountry.MY, "Hong Leong Bank", "হং লিওং ব্যাংক", "HLB", Color(0xFF1E3A8A)),

        // Singapore (SGD) - All 3 Major Banks
        InternationalBank("sg_dbs", AppCountry.SG, "DBS Bank", "ডিবিএস ব্যাংক", "DBS", Color(0xFFDC2626)),
        InternationalBank("sg_ocbc", AppCountry.SG, "OCBC Bank", "ওসিবিসি ব্যাংক", "OCBC", Color(0xFFB91C1C)),
        InternationalBank("sg_uob", AppCountry.SG, "UOB", "ইউওবি ব্যাংক", "UOB", Color(0xFF1D4ED8)),

        // South Korea (KRW) - All 3 Major Banks
        InternationalBank("kr_kb", AppCountry.KR, "KB Kookmin Bank", "কেবি কুকমিন ব্যাংক", "KB", Color(0xFFD97706)),
        InternationalBank("kr_shinhan", AppCountry.KR, "Shinhan Bank", "শিনহান ব্যাংক", "SHINHAN", Color(0xFF1D4ED8)),
        InternationalBank("kr_hana", AppCountry.KR, "Hana Bank", "হানা ব্যাংক", "HANA", Color(0xFF0F766E)),

        // Japan (JPY) - All 3 Major Banks
        InternationalBank("jp_mufg", AppCountry.JP, "MUFG Bank", "এমইউএফজি ব্যাংক", "MUFG", Color(0xFFDC2626)),
        InternationalBank("jp_smbc", AppCountry.JP, "SMBC", "এসএমবিসি ব্যাংক", "SMBC", Color(0xFF15803D)),
        InternationalBank("jp_mizuho", AppCountry.JP, "Mizuho Bank", "মিজুহো ব্যাংক", "MIZUHO", Color(0xFF1E3A8A)),

        // Australia (AUD) - All 4 Major Banks
        InternationalBank("au_commbank", AppCountry.AU, "Commonwealth Bank (CommBank)", "কমনওয়েলথ ব্যাংক (CommBank)", "CBA", Color(0xFFCA8A04)),
        InternationalBank("au_nab", AppCountry.AU, "NAB", "এনএবি ব্যাংক", "NAB", Color(0xFFDC2626)),
        InternationalBank("au_westpac", AppCountry.AU, "Westpac", "ওয়েস্টপ্যাক ব্যাংক", "WBC", Color(0xFFB91C1C)),
        InternationalBank("au_anz", AppCountry.AU, "ANZ", "এএনজেড ব্যাংক", "ANZ", Color(0xFF0284C7)),

        // USA (USD) - All 4 Major Banks
        InternationalBank("us_chase", AppCountry.US, "JPMorgan Chase", "জেপিমরগান চেজ", "CHASE", Color(0xFF1D4ED8)),
        InternationalBank("us_bofa", AppCountry.US, "Bank of America", "ব্যাংক অফ আমেরিকা", "BOFA", Color(0xFFDC2626)),
        InternationalBank("us_wells", AppCountry.US, "Wells Fargo", "ওয়েলস ফার্গো", "WELLS", Color(0xFFB91C1C)),
        InternationalBank("us_citi", AppCountry.US, "Citibank", "সিটিব্যাংক", "CITI", Color(0xFF0369A1)),

        // UK / England (GBP) - All 4 Major Banks
        InternationalBank("uk_hsbc", AppCountry.GB, "HSBC UK", "এইচএসবিসি ইউকে", "HSBC", Color(0xFFDC2626)),
        InternationalBank("uk_barclays", AppCountry.GB, "Barclays", "বার্কলেজ ব্যাংক", "BARCLAYS", Color(0xFF0284C7)),
        InternationalBank("uk_lloyds", AppCountry.GB, "Lloyds Bank", "লয়েডস ব্যাংক", "LLOYDS", Color(0xFF047857)),
        InternationalBank("uk_natwest", AppCountry.GB, "NatWest", "ন্যাটওয়েস্ট ব্যাংক", "NATWEST", Color(0xFF5B21B6)),

        // Canada (CAD) - All 4 Major Banks
        InternationalBank("ca_rbc", AppCountry.CA, "RBC", "আরবিসি ব্যাংক", "RBC", Color(0xFF1D4ED8)),
        InternationalBank("ca_td", AppCountry.CA, "TD Canada Trust", "টিডি কানাডা ট্রাস্ট", "TD", Color(0xFF15803D)),
        InternationalBank("ca_scotia", AppCountry.CA, "Scotiabank", "স্কোশিয়াব্যাংক", "BNS", Color(0xFFDC2626)),
        InternationalBank("ca_bmo", AppCountry.CA, "BMO", "বিএমও ব্যাংক", "BMO", Color(0xFF0284C7)),

        // Italy (EUR) - All 4 Major Banks
        InternationalBank("it_intesa", AppCountry.IT, "Intesa Sanpaolo", "ইন্তেসা সানপাওলো", "INTESA", Color(0xFF15803D)),
        InternationalBank("it_unicredit", AppCountry.IT, "UniCredit", "ইউনিক্রেডিট ব্যাংক", "UNICR", Color(0xFFDC2626)),
        InternationalBank("it_bmps", AppCountry.IT, "Banca Monte dei Paschi di Siena (BMPS)", "ব্যাঙ্কা মন্তে দেই পাস্কি দি সিয়েনা (BMPS)", "BMPS", Color(0xFF7F1D1D)),
        InternationalBank("it_bper", AppCountry.IT, "BPER Banca", "বিপিইআর ব্যাঙ্কা", "BPER", Color(0xFF0F766E)),

        // Cyprus (EUR) - Both Major Banks
        InternationalBank("cy_boc", AppCountry.CY, "Bank of Cyprus", "ব্যাংক অফ সাইপ্রাস", "BOCY", Color(0xFF0E7490)),
        InternationalBank("cy_hellenic", AppCountry.CY, "Hellenic Bank", "হেলেনিক ব্যাংক", "HB", Color(0xFF0369A1)),

        // Major European Countries (DE, FR, ES, PT, GR)
        InternationalBank("de_deutsche", AppCountry.DE, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("de_ing", AppCountry.DE, "ING", "আইএনজি ব্যাংক", "ING", Color(0xFFEA580C)),
        InternationalBank("de_bnp", AppCountry.DE, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),
        InternationalBank("de_santander", AppCountry.DE, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626)),

        InternationalBank("fr_bnp", AppCountry.FR, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),
        InternationalBank("fr_deutsche", AppCountry.FR, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("fr_santander", AppCountry.FR, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626)),
        InternationalBank("fr_ing", AppCountry.FR, "ING", "আইএনজি ব্যাংক", "ING", Color(0xFFEA580C)),

        InternationalBank("es_santander", AppCountry.ES, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626)),
        InternationalBank("es_ing", AppCountry.ES, "ING", "আইএনজি ব্যাংক", "ING", Color(0xFFEA580C)),
        InternationalBank("es_deutsche", AppCountry.ES, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("es_bnp", AppCountry.ES, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),

        InternationalBank("pt_santander", AppCountry.PT, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626)),
        InternationalBank("pt_bnp", AppCountry.PT, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),
        InternationalBank("pt_deutsche", AppCountry.PT, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("pt_ing", AppCountry.PT, "ING", "আইএনজি ব্যাংক", "ING", Color(0xFFEA580C)),

        InternationalBank("gr_deutsche", AppCountry.GR, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("gr_bnp", AppCountry.GR, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),
        InternationalBank("gr_santander", AppCountry.GR, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626)),
        InternationalBank("gr_ing", AppCountry.GR, "ING", "আইএনজি ব্যাংক", "ING", Color(0xFFEA580C)),

        // Additional Global Economies
        InternationalBank("cn_icbc", AppCountry.fromCode("CN"), "ICBC", "আইসিবিসি ব্যাংক", "ICBC", Color(0xFFDC2626), category = BankCategory.GOVERNMENT),
        InternationalBank("cn_boc", AppCountry.fromCode("CN"), "Bank of China", "ব্যাংক অফ চায়না", "BOC", Color(0xFFB91C1C), category = BankCategory.GOVERNMENT),
        InternationalBank("cn_ccb", AppCountry.fromCode("CN"), "China Construction Bank", "চায়না কনস্ট্রাকশন ব্যাংক", "CCB", Color(0xFF1D4ED8), category = BankCategory.GOVERNMENT),
        InternationalBank("cn_cmb", AppCountry.fromCode("CN"), "China Merchants Bank", "চায়না মার্চেন্টস ব্যাংক", "CMB", Color(0xFFE11D48), category = BankCategory.PRIVATE),

        InternationalBank("id_mandiri", AppCountry.fromCode("ID"), "Bank Mandiri", "ব্যাংক মান্দিরি", "MANDIRI", Color(0xFF1E3A8A), category = BankCategory.GOVERNMENT),
        InternationalBank("id_bri", AppCountry.fromCode("ID"), "Bank Rakyat Indonesia (BRI)", "ব্যাংক রাকইয়াত ইন্দোনেশিয়া (BRI)", "BRI", Color(0xFF0284C7), category = BankCategory.GOVERNMENT),
        InternationalBank("id_bca", AppCountry.fromCode("ID"), "Bank Central Asia (BCA)", "ব্যাংক সেন্ট্রাল এশিয়া (BCA)", "BCA", Color(0xFF1D4ED8), category = BankCategory.PRIVATE),

        InternationalBank("ph_bdo", AppCountry.fromCode("PH"), "BDO Unibank", "বিডিও ইউনিব্যাংক", "BDO", Color(0xFF1D4ED8)),
        InternationalBank("ph_bpi", AppCountry.fromCode("PH"), "Bank of the Philippine Islands (BPI)", "ব্যাংক অফ ফিলিপাইন (BPI)", "BPI", Color(0xFFB91C1C)),
        InternationalBank("ph_metro", AppCountry.fromCode("PH"), "Metrobank", "মেট্রোব্যাংক", "METRO", Color(0xFF0369A1)),

        InternationalBank("th_bangkok", AppCountry.fromCode("TH"), "Bangkok Bank", "ব্যাংকক ব্যাংক", "BBL", Color(0xFF1E3A8A)),
        InternationalBank("th_kbank", AppCountry.fromCode("TH"), "Kasikornbank (KBank)", "কাসিকর্নব্যাংক (KBank)", "KBANK", Color(0xFF15803D)),
        InternationalBank("th_scb", AppCountry.fromCode("TH"), "Siam Commercial Bank (SCB)", "সিয়াম কমার্শিয়াল ব্যাংক (SCB)", "SCB", Color(0xFF5B21B6)),

        InternationalBank("vn_vcb", AppCountry.fromCode("VN"), "Vietcombank", "ভিয়েতকমব্যাংক", "VCB", Color(0xFF15803D), category = BankCategory.GOVERNMENT),
        InternationalBank("vn_bidv", AppCountry.fromCode("VN"), "BIDV", "বিআইডিভি ব্যাংক", "BIDV", Color(0xFF0F766E), category = BankCategory.GOVERNMENT),
        InternationalBank("vn_techcom", AppCountry.fromCode("VN"), "Techcombank", "টেককমব্যাংক", "TCB", Color(0xFFDC2626), category = BankCategory.PRIVATE),

        InternationalBank("tr_ziraat", AppCountry.fromCode("TR"), "Ziraat Bank", "জিরাত ব্যাংক", "ZIRAAT", Color(0xFFDC2626), category = BankCategory.GOVERNMENT),
        InternationalBank("tr_isbank", AppCountry.fromCode("TR"), "Türkiye İş Bankası", "ইশ ব্যাংক (İşbank)", "ISBANK", Color(0xFF1E3A8A), category = BankCategory.PRIVATE),
        InternationalBank("tr_garanti", AppCountry.fromCode("TR"), "Garanti BBVA", "গারান্তি বিবিভিএ", "GARANTI", Color(0xFF15803D), category = BankCategory.PRIVATE),

        InternationalBank("eg_nbe", AppCountry.fromCode("EG"), "National Bank of Egypt (NBE)", "ন্যাশনাল ব্যাংক অফ ইজিপ্ট (NBE)", "NBE", Color(0xFF047857), category = BankCategory.GOVERNMENT),
        InternationalBank("eg_cib", AppCountry.fromCode("EG"), "CIB Egypt", "সিআইবি মিশর", "CIB", Color(0xFF1D4ED8), category = BankCategory.PRIVATE),
        InternationalBank("eg_qnb", AppCountry.fromCode("EG"), "QNB Alahli", "কিউএনবি আলআহলি", "QNB", Color(0xFF6B1D4A), category = BankCategory.PRIVATE),

        InternationalBank("br_itau", AppCountry.fromCode("BR"), "Itaú Unibanco", "ইতাউ ইউনিবাঙ্কো", "ITAU", Color(0xFFEA580C)),
        InternationalBank("br_bradesco", AppCountry.fromCode("BR"), "Bradesco", "ব্রাদেস্কো ব্যাংক", "BRAD", Color(0xFFDC2626)),
        InternationalBank("br_bb", AppCountry.fromCode("BR"), "Banco do Brasil", "বাঙ্কো দো ব্রাজিল", "BB", Color(0xFFCA8A04), category = BankCategory.GOVERNMENT),
        InternationalBank("br_nubank", AppCountry.fromCode("BR"), "Nubank", "নুব্যাংক", "NU", Color(0xFF7C3AED))
    )

    private fun regionalFallbackBanks(country: AppCountry): List<InternationalBank> {
        val code = country.code.lowercase()
        val cur = country.suggestedCurrency
        return when (country.group) {
            CountryRegionGroup.EUROPE_CYPRUS ->
                sharedMajorEuropeanBanks.map { it.copy(id = "${code}_${it.id}", country = country, defaultCurrency = cur) }
            CountryRegionGroup.MIDDLE_EAST -> listOf(
                InternationalBank("${code}_arabbank", country, "Arab Bank", "আরব ব্যাংক", "ARAB", Color(0xFF0369A1), cur),
                InternationalBank("${code}_hsbc", country, "HSBC Middle East", "এইচএসবিসি মিডল ইস্ট", "HSBC", Color(0xFFDC2626), cur),
                InternationalBank("${code}_scb", country, "Standard Chartered", "স্ট্যান্ডার্ড চার্টার্ড", "SCB", Color(0xFF0F766E), cur)
            )
            CountryRegionGroup.AFRICA -> listOf(
                InternationalBank("${code}_std", country, "Standard Bank", "স্ট্যান্ডার্ড ব্যাংক", "STD", Color(0xFF1D4ED8), cur),
                InternationalBank("${code}_ecobank", country, "Ecobank", "ইকোব্যাংক", "ECO", Color(0xFF0284C7), cur),
                InternationalBank("${code}_absa", country, "Absa Bank", "অ্যাবসা ব্যাংক", "ABSA", Color(0xFFDC2626), cur)
            )
            CountryRegionGroup.LATIN_AMERICA_CARIBBEAN -> listOf(
                InternationalBank("${code}_santander", country, "Banco Santander", "ব্যাঙ্কো সান্তান্দের", "SAN", Color(0xFFDC2626), cur),
                InternationalBank("${code}_bbva", country, "BBVA", "বিবিভিএ ব্যাংক", "BBVA", Color(0xFF1D4ED8), cur),
                InternationalBank("${code}_scotia", country, "Scotiabank", "স্কোশিয়াব্যাংক", "BNS", Color(0xFFB91C1C), cur)
            )
            else -> listOf(
                InternationalBank("${code}_hsbc", country, "HSBC", "এইচএসবিসি ব্যাংক", "HSBC", Color(0xFFDC2626), cur),
                InternationalBank("${code}_scb", country, "Standard Chartered", "স্ট্যান্ডার্ড চার্টার্ড", "SCB", Color(0xFF0F766E), cur),
                InternationalBank("${code}_citi", country, "Citibank", "সিটিব্যাংক", "CITI", Color(0xFF0369A1), cur)
            )
        }
    }

    /**
     * Returns all major banks for any synchronized [AppCountry].
     * Guarantees every country in [AppCountry.entries] has relevant local or regional banks available.
     */
    fun banksForCountry(country: AppCountry): List<InternationalBank> {
        val direct = all.filter { it.country == country }
        if (direct.isNotEmpty()) return direct
        return regionalFallbackBanks(country)
    }

    fun banksForRegion(region: BankRegion): List<InternationalBank> =
        banksForCountry(region.primaryCountry)
}
