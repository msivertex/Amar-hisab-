package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.local.AccountEntity
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter

/**
 * Country-synchronized Mobile Financial Service (MFS), UPI, Instant Payment, or Digital Wallet provider.
 * Serves as the single source of truth for both Profile Details > Bank & MFS Database and
 * the Add / Edit Account & Card Wizard.
 */
data class InternationalMfsProvider(
    val id: String,
    val countryCode: String,
    val en: String,
    val bn: String,
    val brandColor: Color,
    val secondaryColor: Color,
    val parentBankEn: String = "",
    val parentBankBn: String = ""
) {
    fun label(lang: AppLanguage): String = if (lang.isBangla) bn else en

    fun directoryLabel(lang: AppLanguage): String {
        val base = label(lang)
        val parent = if (lang.isBangla) parentBankBn else parentBankEn
        return if (parent.isNotBlank()) "$base ($parent)" else base
    }
}

/**
 * Unified country-synced catalog of MFS, UPI, and digital wallet providers across all supported countries,
 * plus shared formatting & lookup helpers for Profile Details and Add/Edit Account flows.
 */
object InternationalMfsCatalog {

    private val bangladeshProviders: List<InternationalMfsProvider> = MfsProvider.entries.map { legacy ->
        InternationalMfsProvider(
            id = "bd_${legacy.name.lowercase()}",
            countryCode = "BD",
            en = legacy.en,
            bn = legacy.bn,
            brandColor = legacy.brandColor,
            secondaryColor = legacy.secondaryColor,
            parentBankEn = legacy.parentBankEn,
            parentBankBn = legacy.parentBankBn
        )
    }

    private val byCountryCode: Map<String, List<InternationalMfsProvider>> = mapOf(
        // Bangladesh (10 MFS Providers — 1:1 with MfsProvider)
        "BD" to bangladeshProviders,

        // India (UPI & Digital Wallets)
        "IN" to listOf(
            InternationalMfsProvider("in_paytm", "IN", "Paytm", "পেটিএম (Paytm)", Color(0xFF002E6E), Color(0xFF00BAF2), "UPI & Wallet", "ইউপিআই ও ওয়ালেট"),
            InternationalMfsProvider("in_phonepe", "IN", "PhonePe", "ফোনপে (PhonePe)", Color(0xFF5F259F), Color(0xFF7C3AED), "UPI", "ইউপিআই"),
            InternationalMfsProvider("in_gpay", "IN", "Google Pay", "গুগল পে (GPay)", Color(0xFF1A73E8), Color(0xFF34A853), "UPI", "ইউপিআই"),
            InternationalMfsProvider("in_bhim", "IN", "BHIM UPI", "ভিম ইউপিআই (BHIM)", Color(0xFF0F766E), Color(0xFFF97316), "NPCI", "এনপিসিআই"),
            InternationalMfsProvider("in_amazonpay", "IN", "Amazon Pay", "অ্যামাজন পে", Color(0xFF1F2937), Color(0xFFF59E0B), "UPI & Wallet", "ইউপিআই ও ওয়ালেট"),
            InternationalMfsProvider("in_cred", "IN", "CRED Pay", "ক্রেড পে (CRED)", Color(0xFF111827), Color(0xFF4B5563), "UPI & Cards", "ইউপিআই ও কার্ড"),
            InternationalMfsProvider("in_mobikwik", "IN", "MobiKwik", "মোবিকুইক", Color(0xFF0284C7), Color(0xFF0EA5E9), "Wallet", "ওয়ালেট")
        ),

        // Kuwait (Local Payment Networks & Digital Wallets)
        "KW" to listOf(
            InternationalMfsProvider("kw_knet", "KW", "KNET", "কেনেট (KNET)", Color(0xFF004B8D), Color(0xFF0284C7), "Kuwait National Switch", "কুয়েত ন্যাশনাল সুইচ"),
            InternationalMfsProvider("kw_myfatoorah", "KW", "MyFatoorah", "মাইফাতুরা (MyFatoorah)", Color(0xFF0369A1), Color(0xFF38BDF8), "Digital Payments", "ডিজিটাল পেমেন্ট"),
            InternationalMfsProvider("kw_weyay", "KW", "Weyay", "উইয়ে (Weyay)", Color(0xFF0F766E), Color(0xFF14B8A6), "NBK Digital", "এনবিকে ডিজিটাল"),
            InternationalMfsProvider("kw_wamd", "KW", "WAMD", "ওয়ামদ (WAMD)", Color(0xFF4F46E5), Color(0xFF818CF8), "Instant Transfer", "তাৎক্ষণিক ট্রান্সফার"),
            InternationalMfsProvider("kw_tap", "KW", "Tap Payments", "ট্যাপ পেমেন্টস", Color(0xFF15803D), Color(0xFF22C55E), "Kuwait", "কুয়েত"),
            InternationalMfsProvider("kw_ooredoo", "KW", "Ooredoo Pay", "উরিদু পে", Color(0xFFDC2626), Color(0xFFEF4444), "Mobile Wallet", "মোবাইল ওয়ালেট")
        ),

        // Saudi Arabia
        "SA" to listOf(
            InternationalMfsProvider("sa_stcpay", "SA", "stc pay", "এসটিসি পে (stc pay)", Color(0xFF4F008C), Color(0xFFFF375E), "stc Bank", "এসটিসি ব্যাংক"),
            InternationalMfsProvider("sa_urpay", "SA", "urpay", "ইউআরপে (urpay)", Color(0xFF0052A5), Color(0xFF0EA5E9), "Al Rajhi", "আল রাজি"),
            InternationalMfsProvider("sa_madapay", "SA", "mada Pay", "মাদা পে (mada Pay)", Color(0xFF006C35), Color(0xFF16A34A), "Saudi Payments", "সৌদি পেমেন্টস"),
            InternationalMfsProvider("sa_mobilypay", "SA", "Mobily Pay", "মোবাইলি পে", Color(0xFF0284C7), Color(0xFF38BDF8), "Mobily", "মোবাইলি"),
            InternationalMfsProvider("sa_tiqmo", "SA", "Tiqmo", "তিকমো (Tiqmo)", Color(0xFF7C3AED), Color(0xFFA78BFA), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("sa_barq", "SA", "barq", "বার্ক (barq)", Color(0xFF0F172A), Color(0xFF10B981), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // UAE / Dubai
        "AE" to listOf(
            InternationalMfsProvider("ae_eand_money", "AE", "e& money", "ই অ্যান্ড মানি (e& money)", Color(0xFFE11D48), Color(0xFFFB7185), "Etisalat", "এতিসালাত"),
            InternationalMfsProvider("ae_payit", "AE", "Payit", "পে-ইট (Payit)", Color(0xFF00205B), Color(0xFF0284C7), "FAB", "এফএবি"),
            InternationalMfsProvider("ae_botim", "AE", "Botim Pay", "বোটিম পে", Color(0xFF0F766E), Color(0xFF2DD4BF), "PayBy", "পে-বাই"),
            InternationalMfsProvider("ae_careem", "AE", "Careem Pay", "করিম পে", Color(0xFF15803D), Color(0xFF4ADE80), "Careem", "করিম"),
            InternationalMfsProvider("ae_alansari", "AE", "Al Ansari Wallet", "আল আনসারি ওয়ালেট", Color(0xFF1D4ED8), Color(0xFF38BDF8), "Al Ansari Exchange", "আল আনসারি এক্সচেঞ্জ"),
            InternationalMfsProvider("ae_applepay", "AE", "Apple Pay", "অ্যাপল পে", Color(0xFF111827), Color(0xFF4B5563), "UAE Cards", "আমিরাত কার্ড")
        ),

        // Qatar
        "QA" to listOf(
            InternationalMfsProvider("qa_ooredoo", "QA", "Ooredoo Money", "উরিদু মানি", Color(0xFFDC2626), Color(0xFFF87171), "Ooredoo Qatar", "উরিদু কাতার"),
            InternationalMfsProvider("qa_qpay", "QA", "QPay", "কিউপে (QPay)", Color(0xFF6B1D4A), Color(0xFF9D174D), "Qatar Payment", "কাতার পেমেন্ট"),
            InternationalMfsProvider("qa_ipay", "QA", "iPay by Vodafone", "ভোডাফোন আইপে", Color(0xFFB91C1C), Color(0xFFEF4444), "Vodafone Qatar", "ভোডাফোন কাতার"),
            InternationalMfsProvider("qa_skipcash", "QA", "SkipCash", "স্কিপক্যাশ", Color(0xFF0284C7), Color(0xFF38BDF8), "Mobile Wallet", "মোবাইল ওয়ালেট"),
            InternationalMfsProvider("qa_himyan", "QA", "Himyan", "হিমইয়ান (Himyan)", Color(0xFF047857), Color(0xFF10B981), "QCB National", "কিউসিবি ন্যাশনাল")
        ),

        // Oman
        "OM" to listOf(
            InternationalMfsProvider("om_thawani", "OM", "Thawani Pay", "থাওয়ানি পে", Color(0xFF0284C7), Color(0xFF38BDF8), "Oman Wallet", "ওমান ওয়ালেট"),
            InternationalMfsProvider("om_omantel", "OM", "Omantel Money", "ওমানটেল মানি", Color(0xFF1D4ED8), Color(0xFFF97316), "Omantel", "ওমানটেল"),
            InternationalMfsProvider("om_ooredoo", "OM", "Ooredoo Pay Oman", "উরিদু পে ওমান", Color(0xFFDC2626), Color(0xFFEF4444), "Ooredoo", "উরিদু"),
            InternationalMfsProvider("om_bmwallet", "OM", "bm Wallet", "বিএম ওয়ালেট", Color(0xFFB91C1C), Color(0xFFF59E0B), "Bank Muscat", "ব্যাংক মাস্কাট")
        ),

        // Bahrain
        "BH" to listOf(
            InternationalMfsProvider("bh_benefitpay", "BH", "BenefitPay", "বেনিফিটপে (BenefitPay)", Color(0xFFDC2626), Color(0xFFEF4444), "BENEFIT", "বেনিফিট"),
            InternationalMfsProvider("bh_stcpay", "BH", "stc pay Bahrain", "এসটিসি পে বাহরাইন", Color(0xFF4F008C), Color(0xFFFF375E), "stc Bahrain", "এসটিসি বাহরাইন"),
            InternationalMfsProvider("bh_ila", "BH", "ila Digital Wallet", "ইলা ডিজিটাল ওয়ালেট", Color(0xFF0284C7), Color(0xFF06B6D4), "Bank ABC", "ব্যাংক এবিসি"),
            InternationalMfsProvider("bh_beyon", "BH", "Beyon Money", "বিয়োন মানি", Color(0xFF0F766E), Color(0xFF14B8A6), "Batelco", "ব্যাটেলকো")
        ),

        // Malaysia
        "MY" to listOf(
            InternationalMfsProvider("my_tng", "MY", "Touch 'n Go eWallet", "টাচ এন গো ই-ওয়ালেট", Color(0xFF005EB8), Color(0xFFFACC15), "TNG Digital", "টিএনজি ডিজিটাল"),
            InternationalMfsProvider("my_grabpay", "MY", "GrabPay", "গ্র্যাবপে (GrabPay)", Color(0xFF00B14F), Color(0xFF22C55E), "Grab", "গ্র্যাব"),
            InternationalMfsProvider("my_duitnow", "MY", "DuitNow", "ডুইটনাউ (DuitNow)", Color(0xFFDB2777), Color(0xFFF472B6), "PayNet", "পেনেট"),
            InternationalMfsProvider("my_mae", "MY", "MAE by Maybank", "এমএই (মেব্যাংক)", Color(0xFFCA8A04), Color(0xFFEAB308), "Maybank", "মেব্যাংক"),
            InternationalMfsProvider("my_boost", "MY", "Boost", "বুস্ট (Boost)", Color(0xFFDC2626), Color(0xFFEF4444), "Axiata", "আজিয়াটা"),
            InternationalMfsProvider("my_shopeepay", "MY", "ShopeePay", "শপিপে (ShopeePay)", Color(0xFFEA580C), Color(0xFFFB923C), "Shopee", "শপি")
        ),

        // Singapore
        "SG" to listOf(
            InternationalMfsProvider("sg_paynow", "SG", "PayNow", "পেনাউ (PayNow)", Color(0xFF7C3AED), Color(0xFFA855F7), "ABS Singapore", "সিঙ্গাপুর"),
            InternationalMfsProvider("sg_grabpay", "SG", "GrabPay", "গ্র্যাবপে (GrabPay)", Color(0xFF00B14F), Color(0xFF22C55E), "Grab", "গ্র্যাব"),
            InternationalMfsProvider("sg_paylah", "SG", "DBS PayLah!", "ডিবিএস পে-লাহ!", Color(0xFFDC2626), Color(0xFFEF4444), "DBS Bank", "ডিবিএস ব্যাংক"),
            InternationalMfsProvider("sg_dash", "SG", "Singtel Dash", "সিংটেল ড্যাশ", Color(0xFF0284C7), Color(0xFF38BDF8), "Singtel", "সিংটেল"),
            InternationalMfsProvider("sg_youtrip", "SG", "YouTrip", "ইউট্রিপ (YouTrip)", Color(0xFF4F46E5), Color(0xFF06B6D4), "Multi-Currency Wallet", "মাল্টি-কারেন্সি ওয়ালেট")
        ),

        // Pakistan
        "PK" to listOf(
            InternationalMfsProvider("pk_jazzcash", "PK", "JazzCash", "জ্যাজক্যাশ (JazzCash)", Color(0xFFB91C1C), Color(0xFFF59E0B), "Mobilink", "মোবিলিংক"),
            InternationalMfsProvider("pk_easypaisa", "PK", "easypaisa", "ইজিপয়সা (easypaisa)", Color(0xFF15803D), Color(0xFF22C55E), "Telenor Microfinance", "টেলিনর"),
            InternationalMfsProvider("pk_sadapay", "PK", "SadaPay", "সাদাপে (SadaPay)", Color(0xFF0F766E), Color(0xFFFB7185), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("pk_nayapay", "PK", "NayaPay", "নয়াপে (NayaPay)", Color(0xFFEA580C), Color(0xFFFB923C), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("pk_raast", "PK", "Raast", "রাস্ত (Raast)", Color(0xFF047857), Color(0xFF10B981), "SBP Instant", "এসবিপি")
        ),

        // Nepal
        "NP" to listOf(
            InternationalMfsProvider("np_esewa", "NP", "eSewa", "ই-সেবা (eSewa)", Color(0xFF16A34A), Color(0xFF4ADE80), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("np_khalti", "NP", "Khalti", "খালতি (Khalti)", Color(0xFF5B21B6), Color(0xFF8B5CF6), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("np_imepay", "NP", "IME Pay", "আইএমই পে", Color(0xFFDC2626), Color(0xFFEF4444), "IME Digital", "আইএমই ডিজিটাল"),
            InternationalMfsProvider("np_fonepay", "NP", "Fonepay", "ফোনপে (Fonepay)", Color(0xFFB91C1C), Color(0xFFF97316), "QR Network", "কিউআর নেটওয়ার্ক"),
            InternationalMfsProvider("np_connectips", "NP", "connectIPS", "কানেক্টআইপিএস", Color(0xFF0369A1), Color(0xFF38BDF8), "NCHL", "এনসিএইচএল")
        ),

        // Sri Lanka
        "LK" to listOf(
            InternationalMfsProvider("lk_frimi", "LK", "FriMi", "ফ্রিমি (FriMi)", Color(0xFF009688), Color(0xFF26A69A), "Nations Trust", "নেশনস ট্রাস্ট"),
            InternationalMfsProvider("lk_genie", "LK", "Genie", "জিনি (Genie)", Color(0xFF7C3AED), Color(0xFFEC4899), "Dialog", "ডায়ালগ"),
            InternationalMfsProvider("lk_ezcash", "LK", "eZ Cash", "ই-জেড ক্যাশ", Color(0xFFDC2626), Color(0xFFF97316), "Dialog Axiata", "ডায়ালগ আজিয়াটা"),
            InternationalMfsProvider("lk_mcash", "LK", "mCash", "এমক্যাশ (Sri Lanka)", Color(0xFF15803D), Color(0xFF22C55E), "Mobitel", "মোবিটেল"),
            InternationalMfsProvider("lk_lankaqr", "LK", "LankaQR", "লঙ্কা কিউআর", Color(0xFF1D4ED8), Color(0xFF3B82F6), "LankaPay", "লঙ্কাপে")
        ),

        // Bhutan
        "BT" to listOf(
            InternationalMfsProvider("bt_mbob", "BT", "mBoB", "এমবিওবি (mBoB)", Color(0xFF0284C7), Color(0xFF38BDF8), "Bank of Bhutan", "ব্যাংক অফ ভুটান"),
            InternationalMfsProvider("bt_mpay", "BT", "mPay", "এমপে (mPay)", Color(0xFF1E40AF), Color(0xFF3B82F6), "BNB", "বিএনবি"),
            InternationalMfsProvider("bt_eteeru", "BT", "eTeeru", "ই-তিরু (eTeeru)", Color(0xFFEA580C), Color(0xFFF59E0B), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // Myanmar
        "MM" to listOf(
            InternationalMfsProvider("mm_kbzpay", "MM", "KBZPay", "কেবিজেডপে (KBZPay)", Color(0xFF1D4ED8), Color(0xFF38BDF8), "KBZ Bank", "কেবিজেড ব্যাংক"),
            InternationalMfsProvider("mm_wavepay", "MM", "WavePay", "ওয়েভপে (WavePay)", Color(0xFFEAB308), Color(0xFF0284C7), "Wave Money", "ওয়েভ মানি"),
            InternationalMfsProvider("mm_ayapay", "MM", "AYA Pay", "আয়া পে", Color(0xFFB91C1C), Color(0xFFEF4444), "AYA Bank", "আয়া ব্যাংক"),
            InternationalMfsProvider("mm_cbpay", "MM", "CB Pay", "সিবি পে", Color(0xFF0F766E), Color(0xFF14B8A6), "CB Bank", "সিবি ব্যাংক")
        ),

        // Maldives
        "MV" to listOf(
            InternationalMfsProvider("mv_bmlpay", "MV", "BML MobilePay", "বিএমএল মোবাইলপে", Color(0xFFDC2626), Color(0xFFEF4444), "Bank of Maldives", "ব্যাংক অফ মালদ্বীপ"),
            InternationalMfsProvider("mv_mfaisaa", "MV", "m-Faisaa", "এম-ফাইসা (m-Faisaa)", Color(0xFFB91C1C), Color(0xFFF97316), "Ooredoo Maldives", "উরিদু মালদ্বীপ"),
            InternationalMfsProvider("mv_dhiraagupay", "MV", "Dhiraagu Pay", "ধিরাগু পে", Color(0xFFEA580C), Color(0xFFF59E0B), "Dhiraagu", "ধিরাগু")
        ),

        // USA
        "US" to listOf(
            InternationalMfsProvider("us_zelle", "US", "Zelle", "জেল (Zelle)", Color(0xFF6D28D9), Color(0xFF8B5CF6), "US Bank Network", "ইউএস ব্যাংক নেটওয়ার্ক"),
            InternationalMfsProvider("us_venmo", "US", "Venmo", "ভেনমো (Venmo)", Color(0xFF0284C7), Color(0xFF38BDF8), "PayPal", "পেপাল"),
            InternationalMfsProvider("us_cashapp", "US", "Cash App", "ক্যাশ অ্যাপ", Color(0xFF00D632), Color(0xFF16A34A), "Block", "ব্লক"),
            InternationalMfsProvider("us_paypal", "US", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("us_applecash", "US", "Apple Cash", "অ্যাপল ক্যাশ", Color(0xFF111827), Color(0xFF4B5563), "Apple Wallet", "অ্যাপল ওয়ালেট"),
            InternationalMfsProvider("us_gpay", "US", "Google Pay", "গুগল পে", Color(0xFF1A73E8), Color(0xFF34A853), "Google Wallet", "গুগল ওয়ালেট")
        ),

        // UK
        "GB" to listOf(
            InternationalMfsProvider("gb_monzo", "GB", "Monzo", "মনজো (Monzo)", Color(0xFFFB7185), Color(0xFFE11D48), "Digital Bank", "ডিজিটাল ব্যাংক"),
            InternationalMfsProvider("gb_revolut", "GB", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("gb_wise", "GB", "Wise", "ওয়াইজ (Wise)", Color(0xFF15803D), Color(0xFF84CC16), "Multi-Currency", "মাল্টি-কারেন্সি"),
            InternationalMfsProvider("gb_paypal", "GB", "PayPal UK", "পেপাল ইউকে", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("gb_applepay", "GB", "Apple Pay", "অ্যাপল পে", Color(0xFF1F2937), Color(0xFF4B5563), "UK Cards", "ইউকে কার্ড"),
            InternationalMfsProvider("gb_gpay", "GB", "Google Pay", "গুগল পে", Color(0xFF1A73E8), Color(0xFF34A853), "Google Wallet", "গুগল ওয়ালেট")
        ),

        // Canada
        "CA" to listOf(
            InternationalMfsProvider("ca_interac", "CA", "Interac e-Transfer", "ইন্টারঅ্যাক ই-ট্রান্সফার", Color(0xFFCA8A04), Color(0xFFEAB308), "Canada Instant", "কানাডা ইনস্ট্যান্ট"),
            InternationalMfsProvider("ca_wealthsimple", "CA", "Wealthsimple Cash", "ওয়েলথসিম্পল ক্যাশ", Color(0xFF111827), Color(0xFF4B5563), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("ca_paypal", "CA", "PayPal Canada", "পেপাল কানাডা", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("ca_applepay", "CA", "Apple Pay", "অ্যাপল পে", Color(0xFF1F2937), Color(0xFF4B5563), "Apple Wallet", "অ্যাপল ওয়ালেট"),
            InternationalMfsProvider("ca_gpay", "CA", "Google Pay", "গুগল পে", Color(0xFF1A73E8), Color(0xFF34A853), "Google Wallet", "গুগল ওয়ালেট")
        ),

        // Australia
        "AU" to listOf(
            InternationalMfsProvider("au_payid", "AU", "PayID (Osko)", "পে-আইডি (Osko)", Color(0xFF0284C7), Color(0xFF06B6D4), "NPP Australia", "এনপিপি অস্ট্রেলিয়া"),
            InternationalMfsProvider("au_beem", "AU", "Beem", "বিম (Beem)", Color(0xFF7C3AED), Color(0xFFEC4899), "Australian Wallets", "অস্ট্রেলিয়া ওয়ালেট"),
            InternationalMfsProvider("au_upbank", "AU", "Up", "আপ (Up Digital)", Color(0xFFEA580C), Color(0xFFFACC15), "Digital Bank", "ডিজিটাল ব্যাংক"),
            InternationalMfsProvider("au_paypal", "AU", "PayPal AU", "পেপাল অস্ট্রেলিয়া", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("au_applepay", "AU", "Apple Pay", "অ্যাপল পে", Color(0xFF111827), Color(0xFF4B5563), "Apple Wallet", "অ্যাপল ওয়ালেট")
        ),

        // Japan
        "JP" to listOf(
            InternationalMfsProvider("jp_paypay", "JP", "PayPay", "পে-পে (PayPay)", Color(0xFFDC2626), Color(0xFFEF4444), "QR Wallet", "কিউআর ওয়ালেট"),
            InternationalMfsProvider("jp_rakutenpay", "JP", "Rakuten Pay", "রাকুতেন পে", Color(0xFFBF0000), Color(0xFFE11D48), "Rakuten", "রাকুতেন"),
            InternationalMfsProvider("jp_suica", "JP", "Mobile Suica", "মোবাইল সুইকা", Color(0xFF15803D), Color(0xFF22C55E), "JR East", "জেআর ইস্ট"),
            InternationalMfsProvider("jp_dbarai", "JP", "d-Barai", "ডি-বারাই (d-Barai)", Color(0xFFB91C1C), Color(0xFFF43F5E), "NTT Docomo", "ডোকোমো")
        ),

        // South Korea
        "KR" to listOf(
            InternationalMfsProvider("kr_kakaopay", "KR", "KakaoPay", "কাকাও পে", Color(0xFFEAB308), Color(0xFF1F2937), "Kakao", "কাকাও"),
            InternationalMfsProvider("kr_naverpay", "KR", "Naver Pay", "নেভার পে", Color(0xFF16A34A), Color(0xFF22C55E), "Naver", "নেভার"),
            InternationalMfsProvider("kr_toss", "KR", "Toss", "টস (Toss)", Color(0xFF0284C7), Color(0xFF3B82F6), "Viva Republica", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("kr_samsungpay", "KR", "Samsung Pay", "স্যামসাং পে", Color(0xFF1E3A8A), Color(0xFF3B82F6), "Samsung Wallet", "স্যামসাং ওয়ালেট")
        ),

        // Italy
        "IT" to listOf(
            InternationalMfsProvider("it_satispay", "IT", "Satispay", "স্যাটিসপে (Satispay)", Color(0xFFDC2626), Color(0xFFF97316), "Italy Mobile Pay", "ইতালি মোবাইল পে"),
            InternationalMfsProvider("it_postepay", "IT", "PostePay", "পোস্তেপে (PostePay)", Color(0xFFCA8A04), Color(0xFF1D4ED8), "Poste Italiane", "পোস্তে ইতালিয়ানে"),
            InternationalMfsProvider("it_bancomat", "IT", "BANCOMAT Pay", "ব্যাঙ্কোম্যাট পে", Color(0xFF0369A1), Color(0xFF0EA5E9), "Italy Instant", "ইতালি ইনস্ট্যান্ট"),
            InternationalMfsProvider("it_revolut", "IT", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট"),
            InternationalMfsProvider("it_paypal", "IT", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // Cyprus
        "CY" to listOf(
            InternationalMfsProvider("cy_quickpay", "CY", "QuickPay", "কুইকপে (QuickPay)", Color(0xFF0E7490), Color(0xFF06B6D4), "Bank of Cyprus", "ব্যাংক অফ সাইপ্রাস"),
            InternationalMfsProvider("cy_jccsmart", "CY", "JCCsmart", "জেসিসিস্মার্ট", Color(0xFF0369A1), Color(0xFF38BDF8), "Cyprus Payments", "সাইপ্রাস পেমেন্টস"),
            InternationalMfsProvider("cy_revolut", "CY", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট"),
            InternationalMfsProvider("cy_paypal", "CY", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // Germany
        "DE" to listOf(
            InternationalMfsProvider("de_paypal", "DE", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Germany", "জার্মানি"),
            InternationalMfsProvider("de_n26", "DE", "N26", "এন২৬ (N26)", Color(0xFF0F766E), Color(0xFF14B8A6), "Digital Bank", "ডিজিটাল ব্যাংক"),
            InternationalMfsProvider("de_klarna", "DE", "Klarna", "ক্লারনা (Klarna)", Color(0xFFDB2777), Color(0xFFF472B6), "Instant Pay", "ইনস্ট্যান্ট পে"),
            InternationalMfsProvider("de_wero", "DE", "Wero", "ওয়েরো (Wero)", Color(0xFF1E3A8A), Color(0xFF06B6D4), "EU Instant", "ইউরোপ ইনস্ট্যান্ট"),
            InternationalMfsProvider("de_revolut", "DE", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট")
        ),

        // France
        "FR" to listOf(
            InternationalMfsProvider("fr_lydia", "FR", "Sumeria (Lydia)", "লিডিয়া (Sumeria)", Color(0xFF0284C7), Color(0xFF38BDF8), "France Mobile Pay", "ফ্রান্স মোবাইল পে"),
            InternationalMfsProvider("fr_wero", "FR", "Wero / Paylib", "ওয়েরো / পেলিব", Color(0xFF1E3A8A), Color(0xFF3B82F6), "French Banks", "ফরাসি ব্যাংক"),
            InternationalMfsProvider("fr_revolut", "FR", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট"),
            InternationalMfsProvider("fr_paypal", "FR", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // Spain
        "ES" to listOf(
            InternationalMfsProvider("es_bizum", "ES", "Bizum", "বিজুম (Bizum)", Color(0xFF0E7490), Color(0xFF22D3EE), "Spain Instant", "স্পেন ইনস্ট্যান্ট"),
            InternationalMfsProvider("es_revolut", "ES", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট"),
            InternationalMfsProvider("es_paypal", "ES", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("es_applepay", "ES", "Apple Pay", "অ্যাপল পে", Color(0xFF1F2937), Color(0xFF4B5563), "Apple Wallet", "অ্যাপল ওয়ালেট")
        ),

        // Portugal
        "PT" to listOf(
            InternationalMfsProvider("pt_mbway", "PT", "MB WAY", "এমবি ওয়ে (MB WAY)", Color(0xFFDC2626), Color(0xFFEF4444), "SIBS Portugal", "পর্তুগাল"),
            InternationalMfsProvider("pt_revolut", "PT", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট"),
            InternationalMfsProvider("pt_paypal", "PT", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // Greece
        "GR" to listOf(
            InternationalMfsProvider("gr_iris", "GR", "IRIS Payments", "আইরিস পেমেন্টস", Color(0xFF0369A1), Color(0xFF38BDF8), "DIAS Greece", "গ্রিস ইনস্ট্যান্ট"),
            InternationalMfsProvider("gr_viva", "GR", "Viva.com Wallet", "ভিভা ওয়ালেট", Color(0xFF1E3A8A), Color(0xFF06B6D4), "Greece Digital", "গ্রিস ডিজিটাল"),
            InternationalMfsProvider("gr_revolut", "GR", "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "EU Wallet", "ইউরোপ ওয়ালেট"),
            InternationalMfsProvider("gr_paypal", "GR", "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),

        // Additional Major Global Countries
        "CN" to listOf(
            InternationalMfsProvider("cn_alipay", "CN", "Alipay", "আলিপে (Alipay)", Color(0xFF0284C7), Color(0xFF38BDF8), "Ant Group", "অ্যান্ট গ্রুপ"),
            InternationalMfsProvider("cn_wechatpay", "CN", "WeChat Pay", "উইচ্যাট পে", Color(0xFF16A34A), Color(0xFF22C55E), "Tencent", "টেনসেন্ট"),
            InternationalMfsProvider("cn_unionpay", "CN", "UnionPay QuickPass", "ইউনিয়নপে কুইকপাস", Color(0xFFDC2626), Color(0xFF0369A1), "UnionPay", "ইউনিয়নপে")
        ),
        "ID" to listOf(
            InternationalMfsProvider("id_gopay", "ID", "GoPay", "গোপে (GoPay)", Color(0xFF0284C7), Color(0xFF22C55E), "GoTo", "গোটু"),
            InternationalMfsProvider("id_ovo", "ID", "OVO", "ওভো (OVO)", Color(0xFF5B21B6), Color(0xFF8B5CF6), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("id_dana", "ID", "DANA", "দানা (DANA)", Color(0xFF0284C7), Color(0xFF38BDF8), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("id_shopeepay", "ID", "ShopeePay", "শপিপে (ShopeePay)", Color(0xFFEA580C), Color(0xFFFB923C), "Shopee", "শপি")
        ),
        "PH" to listOf(
            InternationalMfsProvider("ph_gcash", "PH", "GCash", "জিক্যাশ (GCash)", Color(0xFF0284C7), Color(0xFF2563EB), "Globe Fintech", "গ্লোব ফিনটেক"),
            InternationalMfsProvider("ph_maya", "PH", "Maya", "মায়া (Maya)", Color(0xFF15803D), Color(0xFF22C55E), "Digital Bank", "ডিজিটাল ব্যাংক"),
            InternationalMfsProvider("ph_grabpay", "PH", "GrabPay", "গ্র্যাবপে (GrabPay)", Color(0xFF00B14F), Color(0xFF4ADE80), "Grab", "গ্র্যাব")
        ),
        "TH" to listOf(
            InternationalMfsProvider("th_promptpay", "TH", "PromptPay", "প্রম্পটপে (PromptPay)", Color(0xFF1E3A8A), Color(0xFF0284C7), "Thailand Instant", "থাইল্যান্ড ইনস্ট্যান্ট"),
            InternationalMfsProvider("th_truemoney", "TH", "TrueMoney Wallet", "ট্রুমানি ওয়ালেট", Color(0xFFEA580C), Color(0xFFEF4444), "Ascend Money", "অ্যাসেন্ড মানি"),
            InternationalMfsProvider("th_linepay", "TH", "LINE Pay", "লাইন পে", Color(0xFF16A34A), Color(0xFF4ADE80), "LINE", "লাইন")
        ),
        "VN" to listOf(
            InternationalMfsProvider("vn_momo", "VN", "MoMo", "মোমো (MoMo)", Color(0xFFBE185D), Color(0xFFEC4899), "Vietnam Wallet", "ভিয়েতনাম ওয়ালেট"),
            InternationalMfsProvider("vn_zalopay", "VN", "ZaloPay", "জালোপে (ZaloPay)", Color(0xFF0284C7), Color(0xFF38BDF8), "VNG", "ভিএনজি"),
            InternationalMfsProvider("vn_vnpay", "VN", "VNPAY", "ভিএনপে (VNPAY)", Color(0xFF1D4ED8), Color(0xFFDC2626), "QR Pay", "কিউআর পে")
        ),
        "TR" to listOf(
            InternationalMfsProvider("tr_papara", "TR", "Papara", "পাপারা (Papara)", Color(0xFF111827), Color(0xFF4B5563), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("tr_paycell", "TR", "Paycell", "পেসেল (Paycell)", Color(0xFF0284C7), Color(0xFFEAB308), "Turkcell", "টার্কসেল"),
            InternationalMfsProvider("tr_troy", "TR", "TROY Pay", "ট্রয় পে", Color(0xFF0369A1), Color(0xFF38BDF8), "BKM Turkey", "বিকএম")
        ),
        "EG" to listOf(
            InternationalMfsProvider("eg_vodafone", "EG", "Vodafone Cash", "ভোডাফোন ক্যাশ", Color(0xFFDC2626), Color(0xFFEF4444), "Vodafone Egypt", "ভোডাফোন মিশর"),
            InternationalMfsProvider("eg_instapay", "EG", "InstaPay Egypt", "ইনস্টাপে মিশর", Color(0xFF5B21B6), Color(0xFFEA580C), "CBE Instant", "সিবিই"),
            InternationalMfsProvider("eg_fawry", "EG", "Fawry Pay", "ফাওরি পে", Color(0xFFEAB308), Color(0xFF0284C7), "Fawry", "ফাওরি")
        ),
        "KE" to listOf(
            InternationalMfsProvider("ke_mpesa", "KE", "M-Pesa", "এম-পেসা (M-Pesa)", Color(0xFF16A34A), Color(0xFFDC2626), "Safaricom", "সাফারিকম"),
            InternationalMfsProvider("ke_airtel", "KE", "Airtel Money", "এয়ারটেল মানি", Color(0xFFDC2626), Color(0xFFEF4444), "Airtel Kenya", "এয়ারটেল কেনিয়া")
        ),
        "NG" to listOf(
            InternationalMfsProvider("ng_opay", "NG", "OPay", "ও-পে (OPay)", Color(0xFF15803D), Color(0xFF22C55E), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("ng_palmpay", "NG", "PalmPay", "পামপে (PalmPay)", Color(0xFF6D28D9), Color(0xFF8B5CF6), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("ng_kuda", "NG", "Kuda", "কুদা (Kuda)", Color(0xFF4C1D95), Color(0xFF7C3AED), "Digital Bank", "ডিজিটাল ব্যাংক")
        ),
        "ZA" to listOf(
            InternationalMfsProvider("za_payshap", "ZA", "PayShap", "পেশ্যাপ (PayShap)", Color(0xFF0284C7), Color(0xFF16A34A), "South Africa Instant", "দক্ষিণ আফ্রিকা"),
            InternationalMfsProvider("za_snapscan", "ZA", "SnapScan", "স্ন্যাপস্ক্যান", Color(0xFF0369A1), Color(0xFF38BDF8), "Standard Bank", "স্ট্যান্ডার্ড ব্যাংক"),
            InternationalMfsProvider("za_zapper", "ZA", "Zapper", "জ্যাপার (Zapper)", Color(0xFF0284C7), Color(0xFF0EA5E9), "QR Wallet", "কিউআর ওয়ালেট")
        ),
        "BR" to listOf(
            InternationalMfsProvider("br_pix", "BR", "Pix", "পিক্স (Pix)", Color(0xFF0F766E), Color(0xFF14B8A6), "Banco Central", "কেন্দ্রীয় ব্যাংক"),
            InternationalMfsProvider("br_mercadopago", "BR", "Mercado Pago", "মের্কাদো পাগো", Color(0xFF0284C7), Color(0xFF38BDF8), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("br_picpay", "BR", "PicPay", "পিকপে (PicPay)", Color(0xFF16A34A), Color(0xFF4ADE80), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        ),
        "MX" to listOf(
            InternationalMfsProvider("mx_mercadopago", "MX", "Mercado Pago", "মের্কাদো পাগো", Color(0xFF0284C7), Color(0xFF38BDF8), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
            InternationalMfsProvider("mx_codi", "MX", "CoDi / DiMo", "কোডি (CoDi)", Color(0xFF1E3A8A), Color(0xFF0284C7), "Banxico", "ব্যাংক্সিকো"),
            InternationalMfsProvider("mx_spin", "MX", "Spin by OXXO", "স্পিন (OXXO)", Color(0xFFDC2626), Color(0xFFEAB308), "Digital Wallet", "ডিজিটাল ওয়ালেট")
        )
    )

    private fun regionalFallbackProviders(country: AppCountry): List<InternationalMfsProvider> {
        val code = country.code.lowercase()
        return when (country.group) {
            CountryRegionGroup.EUROPE_CYPRUS -> listOf(
                InternationalMfsProvider("${code}_sepa", country.code, "SEPA Instant", "সেপা ইনস্ট্যান্ট", Color(0xFF1E3A8A), Color(0xFF3B82F6), "EU Instant", "ইউরোপ ইনস্ট্যান্ট"),
                InternationalMfsProvider("${code}_revolut", country.code, "Revolut", "রেভোলুট (Revolut)", Color(0xFF111827), Color(0xFF0284C7), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
                InternationalMfsProvider("${code}_wise", country.code, "Wise", "ওয়াইজ (Wise)", Color(0xFF15803D), Color(0xFF84CC16), "Multi-Currency", "মাল্টি-কারেন্সি"),
                InternationalMfsProvider("${code}_paypal", country.code, "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট")
            )
            CountryRegionGroup.AFRICA -> listOf(
                InternationalMfsProvider("${code}_momo", country.code, "Mobile Money (MoMo)", "মোবাইল মানি (MoMo)", Color(0xFFCA8A04), Color(0xFFEAB308), "Mobile Wallet", "মোবাইল ওয়ালেট"),
                InternationalMfsProvider("${code}_wave", country.code, "Wave Mobile Money", "ওয়েভ মোবাইল মানি", Color(0xFF0284C7), Color(0xFF38BDF8), "Mobile Wallet", "মোবাইল ওয়ালেট"),
                InternationalMfsProvider("${code}_airtel", country.code, "Airtel Money", "এয়ারটেল মানি", Color(0xFFDC2626), Color(0xFFEF4444), "Mobile Wallet", "মোবাইল ওয়ালেট"),
                InternationalMfsProvider("${code}_wise", country.code, "Wise", "ওয়াইজ (Wise)", Color(0xFF15803D), Color(0xFF84CC16), "International", "আন্তর্জাতিক")
            )
            CountryRegionGroup.LATIN_AMERICA_CARIBBEAN -> listOf(
                InternationalMfsProvider("${code}_mercadopago", country.code, "Mercado Pago", "মের্কাদো পাগো", Color(0xFF0284C7), Color(0xFF38BDF8), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
                InternationalMfsProvider("${code}_paypal", country.code, "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
                InternationalMfsProvider("${code}_wise", country.code, "Wise", "ওয়াইজ (Wise)", Color(0xFF15803D), Color(0xFF84CC16), "Multi-Currency", "মাল্টি-কারেন্সি"),
                InternationalMfsProvider("${code}_applepay", country.code, "Apple Pay", "অ্যাপল পে", Color(0xFF111827), Color(0xFF4B5563), "Mobile Pay", "মোবাইল পে")
            )
            else -> listOf(
                InternationalMfsProvider("${code}_applepay", country.code, "Apple Pay", "অ্যাপল পে", Color(0xFF111827), Color(0xFF4B5563), "Mobile Wallet", "মোবাইল ওয়ালেট"),
                InternationalMfsProvider("${code}_gpay", country.code, "Google Pay", "গুগল পে", Color(0xFF1A73E8), Color(0xFF34A853), "Mobile Wallet", "মোবাইল ওয়ালেট"),
                InternationalMfsProvider("${code}_paypal", country.code, "PayPal", "পেপাল (PayPal)", Color(0xFF003087), Color(0xFF0079C1), "Digital Wallet", "ডিজিটাল ওয়ালেট"),
                InternationalMfsProvider("${code}_wise", country.code, "Wise", "ওয়াইজ (Wise)", Color(0xFF15803D), Color(0xFF84CC16), "Multi-Currency", "মাল্টি-কারেন্সি")
            )
        }
    }

    /**
     * Returns country-matched MFS / UPI / digital wallet providers for any [AppCountry].
     * Guarantees every country has relevant selectable options.
     */
    fun providersForCountry(country: AppCountry): List<InternationalMfsProvider> {
        return byCountryCode[country.code.uppercase()] ?: regionalFallbackProviders(country)
    }

    fun providersForCountryCode(countryCode: String): List<InternationalMfsProvider> =
        providersForCountry(AppCountry.fromCode(countryCode))

    fun mfsSelectionHeading(country: AppCountry, lang: AppLanguage): String = when (country.code) {
        "BD" -> if (lang.isBangla) "বাংলাদেশী এমএফএস নির্বাচন করুন" else "Select Bangladeshi MFS Provider"
        "IN" -> if (lang.isBangla) "ভারতীয় ইউপিআই / ওয়ালেট নির্বাচন করুন" else "Select Indian UPI / Wallet"
        "KW" -> if (lang.isBangla) "কুয়েত পেমেন্ট / ডিজিটাল ওয়ালেট নির্বাচন করুন" else "Select Kuwait Payment / Wallet"
        "SA" -> if (lang.isBangla) "সৌদি আরব ডিজিটাল ওয়ালেট নির্বাচন করুন" else "Select Saudi Digital Wallet"
        "AE" -> if (lang.isBangla) "আমিরাত ডিজিটাল ওয়ালেট নির্বাচন করুন" else "Select UAE Digital Wallet"
        else -> if (lang.isBangla) {
            "${country.label(lang)} এমএফএস / ডিজিটাল ওয়ালেট নির্বাচন করুন"
        } else {
            "Select ${country.label(lang)} MFS / Wallet"
        }
    }

    fun bankDirectorySubtitle(country: AppCountry, lang: AppLanguage): String {
        val banks = InternationalBankCatalog.banksForCountry(country)
        val total = banks.size
        val govtCount = banks.count { it.category == BankCategory.GOVERNMENT }
        val privateCount = banks.count { it.category == BankCategory.PRIVATE }
        val countryName = country.label(lang)
        return if (govtCount > 0 && privateCount > 0) {
            if (lang.isBangla) {
                "$countryName • ${BdtFormatter.formatNumber(total, lang)}টি ব্যাংক (${BdtFormatter.formatNumber(govtCount, lang)}টি সরকারি • ${BdtFormatter.formatNumber(privateCount, lang)}টি বেসরকারি)"
            } else {
                "$countryName • $total banks ($govtCount Govt • $privateCount Private)"
            }
        } else {
            val preview = banks.take(3).joinToString(", ") { it.shortCode.ifBlank { it.label(lang) } }
            val hasMore = total > 3
            if (lang.isBangla) {
                val suffix = if (hasMore) "$preview ও আরও" else preview
                "$countryName • ${BdtFormatter.formatNumber(total, lang)}টি ব্যাংক ($suffix)"
            } else {
                val suffix = if (hasMore) "$preview & more" else preview
                "$countryName • $total banks ($suffix)"
            }
        }
    }

    fun mfsDirectorySubtitle(country: AppCountry, lang: AppLanguage): String {
        val providers = providersForCountry(country)
        val total = providers.size
        val preview = providers.take(3).joinToString(", ") { it.label(lang) }
        val hasMore = total > 3
        val countryName = country.label(lang)
        return if (lang.isBangla) {
            val suffix = if (hasMore) "$preview ও আরও" else preview
            "$countryName • ${BdtFormatter.formatNumber(total, lang)}টি সেবা ($suffix)"
        } else {
            val suffix = if (hasMore) "$preview & more" else preview
            "$countryName • $total wallets ($suffix)"
        }
    }

    fun findMatchingBank(country: AppCountry, rawBankName: String): InternationalBank? {
        val needle = rawBankName.trim()
        if (needle.isBlank()) return null
        val banks = InternationalBankCatalog.banksForCountry(country)
        return banks.firstOrNull {
            it.en.equals(needle, ignoreCase = true) ||
                it.bn.equals(needle, ignoreCase = true) ||
                it.shortCode.equals(needle, ignoreCase = true)
        } ?: banks.firstOrNull {
            needle.contains(it.en, ignoreCase = true) ||
                needle.contains(it.bn, ignoreCase = true)
        }
    }

    fun findMatchingMfs(country: AppCountry, rawProviderName: String): InternationalMfsProvider? {
        val needle = rawProviderName.trim()
        if (needle.isBlank()) return null
        val providers = providersForCountry(country)
        return providers.firstOrNull {
            it.en.equals(needle, ignoreCase = true) ||
                it.bn.equals(needle, ignoreCase = true) ||
                it.directoryLabel(AppLanguage.EN).equals(needle, ignoreCase = true) ||
                it.directoryLabel(AppLanguage.BN).equals(needle, ignoreCase = true)
        } ?: providers.firstOrNull {
            needle.contains(it.en, ignoreCase = true) ||
                needle.contains(it.bn, ignoreCase = true)
        }
    }

    /**
     * Resolves the best matching country when editing an existing account:
     * prefers [userCountry] if the account matches a local bank/MFS or its currency;
     * otherwise matches by bank/MFS across catalogs or currency.
     */
    fun resolveCountryForExistingAccount(
        account: AccountEntity,
        userCountry: AppCountry
    ): AppCountry {
        val provider = account.bankOrProvider.trim()
        if (account.type == AccountType.MFS) {
            if (findMatchingMfs(userCountry, provider) != null) return userCountry
            byCountryCode.entries.firstOrNull { (_, list) ->
                list.any {
                    it.en.equals(provider, ignoreCase = true) ||
                        it.bn.equals(provider, ignoreCase = true)
                }
            }?.let { return AppCountry.fromCode(it.key) }
        } else if (account.type == AccountType.DEBIT_BANK || account.type == AccountType.CREDIT_CARD) {
            if (findMatchingBank(userCountry, provider) != null) return userCountry
            InternationalBankCatalog.all.firstOrNull {
                it.en.equals(provider, ignoreCase = true) ||
                    it.bn.equals(provider, ignoreCase = true)
            }?.let { return it.country }
        }
        if (userCountry.suggestedCurrency.code.equals(account.currencyCode, ignoreCase = true)) {
            return userCountry
        }
        return AppCurrency.fromCodeOrNull(account.currencyCode)?.let { AppCountry.preferredForCurrency(it) }
            ?: userCountry
    }
}
