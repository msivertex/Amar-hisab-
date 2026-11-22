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
 * Supported primary & secondary/current currencies for Bangladesh & international expat tracking.
 * Each currency includes country flag, localized country & currency names, symbol, and
 * direct mapping into [MarketCurrency] for live/offline exchange conversions.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val flag: String,
    val countryEn: String,
    val countryBn: String,
    val nameEn: String,
    val nameBn: String,
    val usesIndianGrouping: Boolean = false,
    val decimalPlaces: Int = 2
) {
    BDT(
        code = "BDT",
        symbol = "৳",
        flag = "🇧🇩",
        countryEn = "Bangladesh",
        countryBn = "বাংলাদেশ",
        nameEn = "Bangladeshi Taka",
        nameBn = "বাংলাদেশী টাকা",
        usesIndianGrouping = true,
        decimalPlaces = 0
    ),
    KWD(
        code = "KWD",
        symbol = "KD",
        flag = "🇰🇼",
        countryEn = "Kuwait",
        countryBn = "কুয়েত",
        nameEn = "Kuwaiti Dinar",
        nameBn = "কুয়েতি দিনার",
        decimalPlaces = 3
    ),
    SAR(
        code = "SAR",
        symbol = "SAR",
        flag = "🇸🇦",
        countryEn = "Saudi Arabia",
        countryBn = "সৌদি আরব",
        nameEn = "Saudi Riyal",
        nameBn = "সৌদি রিয়াল"
    ),
    AED(
        code = "AED",
        symbol = "AED",
        flag = "🇦🇪",
        countryEn = "UAE (Dubai)",
        countryBn = "দুবাই / আমিরাত",
        nameEn = "UAE Dirham",
        nameBn = "আমিরাত দিরহাম"
    ),
    QAR(
        code = "QAR",
        symbol = "QAR",
        flag = "🇶🇦",
        countryEn = "Qatar",
        countryBn = "কাতার",
        nameEn = "Qatari Riyal",
        nameBn = "কাতারি রিয়াল"
    ),
    MYR(
        code = "MYR",
        symbol = "RM",
        flag = "🇲🇾",
        countryEn = "Malaysia",
        countryBn = "মালয়েশিয়া",
        nameEn = "Malaysian Ringgit",
        nameBn = "মালয়েশিয়ান রিংগিত"
    ),
    EUR(
        code = "EUR",
        symbol = "€",
        flag = "🇮🇹",
        countryEn = "Italy / Europe",
        countryBn = "ইতালি / ইউরোপ",
        nameEn = "Euro",
        nameBn = "ইউরো"
    ),
    GBP(
        code = "GBP",
        symbol = "£",
        flag = "🇬🇧",
        countryEn = "UK / England",
        countryBn = "যুক্তরাজ্য / ইংল্যান্ড",
        nameEn = "British Pound",
        nameBn = "ব্রিটিশ পাউন্ড"
    ),
    USD(
        code = "USD",
        symbol = "$",
        flag = "🇺🇸",
        countryEn = "USA",
        countryBn = "যুক্তরাষ্ট্র",
        nameEn = "US Dollar",
        nameBn = "মার্কিন ডলার"
    ),
    INR(
        code = "INR",
        symbol = "₹",
        flag = "🇮🇳",
        countryEn = "India",
        countryBn = "ভারত",
        nameEn = "Indian Rupee",
        nameBn = "ভারতীয় রুপি",
        usesIndianGrouping = true,
        decimalPlaces = 0
    );

    fun countryName(lang: AppLanguage): String = if (lang == AppLanguage.BN) countryBn else countryEn
    fun currencyName(lang: AppLanguage): String = if (lang == AppLanguage.BN) nameBn else nameEn

    fun shortBadge(): String = "$flag $code"

    /** Clean flag-free badge label for balance cards (e.g., "BDT", "KD", "SAR", "USD"). */
    fun cleanBadgeLabel(): String = when (this) {
        KWD -> "KD"
        else -> code
    }

    fun fullDisplay(lang: AppLanguage): String = "$flag $code ($symbol) • ${countryName(lang)}"

    fun toMarketCurrency(): MarketCurrency = when (this) {
        BDT -> MarketCurrency.BDT
        KWD -> MarketCurrency.KWD
        SAR -> MarketCurrency.SAR
        AED -> MarketCurrency.AED
        QAR -> MarketCurrency.QAR
        MYR -> MarketCurrency.MYR
        EUR -> MarketCurrency.EUR
        GBP -> MarketCurrency.GBP
        USD -> MarketCurrency.USD
        INR -> MarketCurrency.INR
    }

    companion object {
        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return BDT
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: BDT
        }

        fun fromCodeOrNull(code: String?): AppCurrency? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}

/**
 * Supported user countries/regions for the interactive Country selector in Profile Details.
 */
enum class AppCountry(
    val code: String,
    val flag: String,
    val nameEn: String,
    val nameBn: String,
    val suggestedCurrency: AppCurrency
) {
    BD("BD", "🇧🇩", "Bangladesh", "বাংলাদেশ", AppCurrency.BDT),
    KW("KW", "🇰🇼", "Kuwait", "কুয়েত", AppCurrency.KWD),
    SA("SA", "🇸🇦", "Saudi Arabia", "সৌদি আরব", AppCurrency.SAR),
    AE("AE", "🇦🇪", "UAE (Dubai)", "সংযুক্ত আরব আমিরাত", AppCurrency.AED),
    QA("QA", "🇶🇦", "Qatar", "কাতার", AppCurrency.QAR),
    OM("OM", "🇴🇲", "Oman", "ওমান", AppCurrency.SAR),
    BH("BH", "🇧🇭", "Bahrain", "বাহরাইন", AppCurrency.USD),
    MY("MY", "🇲🇾", "Malaysia", "মালয়েশিয়া", AppCurrency.MYR),
    SG("SG", "🇸🇬", "Singapore", "সিঙ্গাপুর", AppCurrency.USD),
    IT("IT", "🇮🇹", "Italy", "ইতালি", AppCurrency.EUR),
    DE("DE", "🇩🇪", "Germany", "জার্মানি", AppCurrency.EUR),
    FR("FR", "🇫🇷", "France", "ফ্রান্স", AppCurrency.EUR),
    GB("GB", "🇬🇧", "United Kingdom", "যুক্তরাজ্য", AppCurrency.GBP),
    US("US", "🇺🇸", "United States", "যুক্তরাষ্ট্র", AppCurrency.USD),
    CA("CA", "🇨🇦", "Canada", "কানাডা", AppCurrency.USD),
    AU("AU", "🇦🇺", "Australia", "অস্ট্রেলিয়া", AppCurrency.USD),
    IN("IN", "🇮🇳", "India", "ভারত", AppCurrency.INR),
    JP("JP", "🇯🇵", "Japan", "জাপান", AppCurrency.USD);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) nameBn else nameEn

    companion object {
        fun fromCode(code: String?): AppCountry {
            if (code.isNullOrBlank()) return BD
            val normalized = code.trim()
            return entries.firstOrNull {
                it.code.equals(normalized, ignoreCase = true) ||
                    it.nameEn.equals(normalized, ignoreCase = true)
            } ?: BD
        }
    }
}

/**
 * Supported banking regions for Account & Card creation (Bangladesh + expat destinations).
 */
enum class BankRegion(
    val flag: String,
    val en: String,
    val bn: String,
    val defaultCurrency: AppCurrency
) {
    BANGLADESH("🇧🇩", "Bangladesh", "বাংলাদেশ", AppCurrency.BDT),
    SAUDI_ARABIA("🇸🇦", "Saudi Arabia", "সৌদি আরব", AppCurrency.SAR),
    QATAR("🇶🇦", "Qatar", "কাতার", AppCurrency.QAR),
    KUWAIT("🇰🇼", "Kuwait", "কুয়েত", AppCurrency.KWD),
    UAE("🇦🇪", "UAE (Dubai)", "দুবাই (আমিরাত)", AppCurrency.AED),
    MALAYSIA("🇲🇾", "Malaysia", "মালয়েশিয়া", AppCurrency.MYR),
    ITALY("🇮🇹", "Italy", "ইতালি", AppCurrency.EUR),
    EUROPE("🇪🇺", "Europe", "ইউরোপ", AppCurrency.EUR),
    UK("🇬🇧", "UK (England)", "যুক্তরাজ্য (ইংল্যান্ড)", AppCurrency.GBP);

    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en

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
            AppCurrency.USD, AppCurrency.INR -> BANGLADESH
        }
    }
}

data class InternationalBank(
    val id: String,
    val region: BankRegion,
    val en: String,
    val bn: String,
    val shortCode: String,
    val brandColor: Color,
    val defaultCurrency: AppCurrency = region.defaultCurrency
) {
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

object InternationalBankCatalog {
    val all: List<InternationalBank> = listOf(
        // Bangladesh (BDT) - Existing 6 + 16 expanded Government & Private banks
        InternationalBank("bd_city", BankRegion.BANGLADESH, "City Bank", "সিটি ব্যাংক", "CITY", BrandCityBank),
        InternationalBank("bd_dbbl", BankRegion.BANGLADESH, "Dutch-Bangla Bank", "ডাচ-বাংলা ব্যাংক", "DBBL", BrandDbbl),
        InternationalBank("bd_brac", BankRegion.BANGLADESH, "BRAC Bank", "ব্র্যাক ব্যাংক", "BRAC", BrandBracBank),
        InternationalBank("bd_islami", BankRegion.BANGLADESH, "Islami Bank", "ইসলামী ব্যাংক", "IBBL", BrandIslamiBank),
        InternationalBank("bd_trust", BankRegion.BANGLADESH, "Trust Bank", "ট্রাস্ট ব্যাংক", "TRUST", BrandTrustBank),
        InternationalBank("bd_sonali", BankRegion.BANGLADESH, "Sonali Bank", "সোনালী ব্যাংক", "SONALI", BrandSonaliBank),
        InternationalBank("bd_janata", BankRegion.BANGLADESH, "Janata Bank", "জনতা ব্যাংক", "JANATA", BangladeshiBank.JANATA_BANK.brandColor),
        InternationalBank("bd_agrani", BankRegion.BANGLADESH, "Agrani Bank", "অগ্রণী ব্যাংক", "AGRANI", BangladeshiBank.AGRANI_BANK.brandColor),
        InternationalBank("bd_rupali", BankRegion.BANGLADESH, "Rupali Bank", "রূপালী ব্যাংক", "RUPALI", BangladeshiBank.RUPALI_BANK.brandColor),
        InternationalBank("bd_bkb", BankRegion.BANGLADESH, "Bangladesh Krishi Bank", "বাংলাদেশ কৃষি ব্যাংক", "BKB", BangladeshiBank.BKB.brandColor),
        InternationalBank("bd_ebl", BankRegion.BANGLADESH, "Eastern Bank", "ইস্টার্ন ব্যাংক", "EBL", BangladeshiBank.EBL.brandColor),
        InternationalBank("bd_prime", BankRegion.BANGLADESH, "Prime Bank", "প্রাইম ব্যাংক", "PRIME", BangladeshiBank.PRIME_BANK.brandColor),
        InternationalBank("bd_mtb", BankRegion.BANGLADESH, "Mutual Trust Bank", "মিউচুয়াল ট্রাস্ট ব্যাংক", "MTB", BangladeshiBank.MTB.brandColor),
        InternationalBank("bd_dhaka", BankRegion.BANGLADESH, "Dhaka Bank", "ঢাকা ব্যাংক", "DHAKA", BangladeshiBank.DHAKA_BANK.brandColor),
        InternationalBank("bd_pubali", BankRegion.BANGLADESH, "Pubali Bank", "পূবালী ব্যাংক", "PUBALI", BangladeshiBank.PUBALI_BANK.brandColor),
        InternationalBank("bd_bankasia", BankRegion.BANGLADESH, "Bank Asia", "ব্যাংক এশিয়া", "BANKASIA", BangladeshiBank.BANK_ASIA.brandColor),
        InternationalBank("bd_sebl", BankRegion.BANGLADESH, "Southeast Bank", "সাউথইস্ট ব্যাংক", "SEBL", BangladeshiBank.SEBL.brandColor),
        InternationalBank("bd_one", BankRegion.BANGLADESH, "One Bank", "ওয়ান ব্যাংক", "ONE", BangladeshiBank.ONE_BANK.brandColor),
        InternationalBank("bd_uttara", BankRegion.BANGLADESH, "Uttara Bank", "উত্তরা ব্যাংক", "UTTARA", BangladeshiBank.UTTARA_BANK.brandColor),
        InternationalBank("bd_jamuna", BankRegion.BANGLADESH, "Jamuna Bank", "যমুনা ব্যাংক", "JAMUNA", BangladeshiBank.JAMUNA_BANK.brandColor),
        InternationalBank("bd_ucb", BankRegion.BANGLADESH, "United Commercial Bank", "ইউনাইটেড কমার্শিয়াল ব্যাংক", "UCB", BangladeshiBank.UCB.brandColor),
        InternationalBank("bd_mbl", BankRegion.BANGLADESH, "Mercantile Bank", "মার্কেন্টাইল ব্যাংক", "MBL", BangladeshiBank.MBL.brandColor),

        // Saudi Arabia (SAR)
        InternationalBank("sa_alrajhi", BankRegion.SAUDI_ARABIA, "Al Rajhi Bank", "আল রাজি ব্যাংক", "RAJHI", Color(0xFF0052A5)),
        InternationalBank("sa_snb", BankRegion.SAUDI_ARABIA, "Saudi National Bank (SNB)", "সৌদি ন্যাশনাল ব্যাংক (SNB)", "SNB", Color(0xFF006C35)),
        InternationalBank("sa_riyad", BankRegion.SAUDI_ARABIA, "Riyad Bank", "রিয়াদ ব্যাংক", "RIYAD", Color(0xFF1D4ED8)),
        InternationalBank("sa_alinma", BankRegion.SAUDI_ARABIA, "Alinma Bank", "আলইনমা ব্যাংক", "INMA", Color(0xFF6B4E3D)),
        InternationalBank("sa_stcpay", BankRegion.SAUDI_ARABIA, "stc bank / stc pay", "এসটিসি ব্যাংক / stc pay", "STC", Color(0xFF4F008C)),

        // Qatar (QAR)
        InternationalBank("qa_qnb", BankRegion.QATAR, "Qatar National Bank (QNB)", "কাতার ন্যাশনাল ব্যাংক (QNB)", "QNB", Color(0xFF6B1D4A)),
        InternationalBank("qa_cbq", BankRegion.QATAR, "Commercial Bank of Qatar", "কমার্শিয়াল ব্যাংক অফ কাতার", "CBQ", Color(0xFF0F4C81)),
        InternationalBank("qa_dukhan", BankRegion.QATAR, "Dukhan Bank", "দুখান ব্যাংক", "DUKHAN", Color(0xFF0E7490)),
        InternationalBank("qa_masraf", BankRegion.QATAR, "Masraf Al Rayan", "মাসরাফ আল রায়ান", "MAR", Color(0xFF047857)),

        // Kuwait (KWD)
        InternationalBank("kw_nbk", BankRegion.KUWAIT, "National Bank of Kuwait (NBK)", "ন্যাশনাল ব্যাংক অফ কুয়েত (NBK)", "NBK", Color(0xFF003B73)),
        InternationalBank("kw_kfh", BankRegion.KUWAIT, "Kuwait Finance House (KFH)", "কুয়েত ফিন্যান্স হাউস (KFH)", "KFH", Color(0xFF007A3D)),
        InternationalBank("kw_gulf", BankRegion.KUWAIT, "Gulf Bank Kuwait", "গালফ ব্যাংক কুয়েত", "GULF", Color(0xFFB91C1C)),
        InternationalBank("kw_boubyan", BankRegion.KUWAIT, "Boubyan Bank", "বুবিয়ান ব্যাংক", "BOUBYAN", Color(0xFF991B1B)),

        // UAE / Dubai (AED)
        InternationalBank("ae_enbd", BankRegion.UAE, "Emirates NBD", "এমিরেটস এনবিডি", "ENBD", Color(0xFF00338D)),
        InternationalBank("ae_fab", BankRegion.UAE, "First Abu Dhabi Bank (FAB)", "ফার্স্ট আবুধাবি ব্যাংক (FAB)", "FAB", Color(0xFF00205B)),
        InternationalBank("ae_dib", BankRegion.UAE, "Dubai Islamic Bank (DIB)", "দুবাই ইসলামী ব্যাংক (DIB)", "DIB", Color(0xFF007A4D)),
        InternationalBank("ae_mashreq", BankRegion.UAE, "Mashreq Bank", "মাশরেক ব্যাংক", "MASHREQ", Color(0xFFEA580C)),
        InternationalBank("ae_adcb", BankRegion.UAE, "ADCB Bank", "এডিসিবি ব্যাংক", "ADCB", Color(0xFF9D174D)),

        // Malaysia (MYR)
        InternationalBank("my_maybank", BankRegion.MALAYSIA, "Maybank", "মেব্যাংক", "MAYBANK", Color(0xFFCA8A04)),
        InternationalBank("my_cimb", BankRegion.MALAYSIA, "CIMB Bank", "সিআইএমবি ব্যাংক", "CIMB", Color(0xFFB91C1C)),
        InternationalBank("my_public", BankRegion.MALAYSIA, "Public Bank", "পাবলিক ব্যাংক", "PBB", Color(0xFF991B1B)),
        InternationalBank("my_rhb", BankRegion.MALAYSIA, "RHB Bank", "আরএইচবি ব্যাংক", "RHB", Color(0xFF0284C7)),
        InternationalBank("my_hlb", BankRegion.MALAYSIA, "Hong Leong Bank", "হং লিওং ব্যাংক", "HLB", Color(0xFF1E3A8A)),

        // Italy (EUR)
        InternationalBank("it_intesa", BankRegion.ITALY, "Intesa Sanpaolo", "ইন্তেসা সানপাওলো", "INTESA", Color(0xFF15803D)),
        InternationalBank("it_unicredit", BankRegion.ITALY, "UniCredit Italia", "ইউনিক্রেডিট ইতালি", "UNICR", Color(0xFFDC2626)),
        InternationalBank("it_postepay", BankRegion.ITALY, "PostePay / BancoPosta", "পোস্টেপে / ব্যাংকোপোস্টা", "POSTE", Color(0xFF0369A1)),
        InternationalBank("it_fineco", BankRegion.ITALY, "Fineco Bank", "ফাইনেকো ব্যাংক", "FINECO", Color(0xFF1D4ED8)),

        // Europe (EUR)
        InternationalBank("eu_revolut", BankRegion.EUROPE, "Revolut Europe", "রেভোলুট ইউরোপ", "REVOLUT", Color(0xFF111827)),
        InternationalBank("eu_n26", BankRegion.EUROPE, "N26 Bank", "এন২৬ ব্যাংক", "N26", Color(0xFF0D9488)),
        InternationalBank("eu_wise", BankRegion.EUROPE, "Wise Multi-Currency", "ওয়াইজ মাল্টি-কারেন্সি", "WISE", Color(0xFF16A34A)),
        InternationalBank("eu_deutsche", BankRegion.EUROPE, "Deutsche Bank", "ডয়েচে ব্যাংক", "DB", Color(0xFF1E3A8A)),
        InternationalBank("eu_bnp", BankRegion.EUROPE, "BNP Paribas", "বিএনপি পারিবাস", "BNP", Color(0xFF047857)),

        // UK / England (GBP)
        InternationalBank("uk_barclays", BankRegion.UK, "Barclays UK", "বার্কলেজ ইউকে", "BARCLAYS", Color(0xFF0284C7)),
        InternationalBank("uk_hsbc", BankRegion.UK, "HSBC UK", "এইচএসবিসি ইউকে", "HSBC", Color(0xFFDC2626)),
        InternationalBank("uk_lloyds", BankRegion.UK, "Lloyds Bank", "লয়েডস ব্যাংক", "LLOYDS", Color(0xFF047857)),
        InternationalBank("uk_monzo", BankRegion.UK, "Monzo Bank", "মনজো ব্যাংক", "MONZO", Color(0xFFE11D48)),
        InternationalBank("uk_natwest", BankRegion.UK, "NatWest UK", "ন্যাটওয়েস্ট ইউকে", "NATWEST", Color(0xFF5B21B6)),
        InternationalBank("uk_starling", BankRegion.UK, "Starling Bank", "স্টারলিং ব্যাংক", "STARLING", Color(0xFF4F46E5))
    )

    fun banksForRegion(region: BankRegion): List<InternationalBank> =
        all.filter { it.region == region }
}
