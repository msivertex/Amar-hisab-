package com.example.data.support

import java.util.Locale

const val SUPPORT_WHATSAPP_DISPLAY = "+8801708901947"
const val SUPPORT_WHATSAPP_DIGITS = "8801708901947"

const val SUPPORT_INITIAL_GREETING_BN =
    "আসসালামু আলাইকুম! 'আমার হিসাব' কাস্টমার সাপোর্টে আপনাকে স্বাগতম।"

const val SUPPORT_GREETING_REPLY_BN =
    "ওয়ালাইকুম আসসালাম! আপনাকে কীভাবে সাহায্য করতে পারি?"

const val SUPPORT_OFFLINE_AI_NOTICE_BN =
    "এই মুহূর্তে ইন্টারনেট সংযোগ নেই। লাইভ এআই উত্তরের জন্য ইন্টারনেট চালু করুন—অথবা নিচের ক্যাটাগরি গাইডগুলো অফলাইনেই ব্রাউজ করুন।"

const val SUPPORT_OFFLINE_GUIDE_FOOTER_BN =
    "\n\n(অফলাইন মোড সক্রিয়: এটি বিল্ট-ইন গাইড থেকে দেখানো হয়েছে। কাস্টম প্রশ্নের লাইভ এআই উত্তরের জন্য ইন্টারনেট চালু করুন।)"

enum class SupportCategoryId {
    TRANSACTIONS_ENTRY,
    LIVE_MARKET,
    INSIGHTS_REPORTS,
    DATETIME_PERMISSIONS,
    BACKUP_SECURITY,
    OTHER_SUPPORT
}

data class SupportQuickOption(
    val id: String,
    val categoryId: SupportCategoryId,
    val labelBn: String,
    val labelEn: String,
    val guideBn: String,
    val isLiveCareTrigger: Boolean = false
)

data class SupportCategory(
    val id: SupportCategoryId,
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val options: List<SupportQuickOption>
)

enum class SupportSender {
    ASSISTANT,
    USER
}

data class SupportChatMessage(
    val id: String,
    val sender: SupportSender,
    val text: String,
    val timestampMillis: Long,
    val showWhatsAppCard: Boolean = false,
    val offerNotResolvedAction: Boolean = false,
    val isOfflineNotice: Boolean = false,
    val followUpChips: List<String> = emptyList()
)

data class SupportAiReply(
    val replyText: String,
    val needsHumanEscalation: Boolean = false,
    val suggestedFollowUps: List<String> = emptyList()
)

data class SupportUiState(
    val messages: List<SupportChatMessage> = emptyList(),
    val selectedCategoryId: SupportCategoryId? = null,
    val isTyping: Boolean = false,
    val isOnline: Boolean = true,
    val hasAttemptedTroubleshooting: Boolean = false
)

object CustomerSupportCatalog {

    val categories: List<SupportCategory> = listOf(
        SupportCategory(
            id = SupportCategoryId.TRANSACTIONS_ENTRY,
            titleBn = "হিসাব ও লেনদেন এন্ট্রি",
            titleEn = "Accounts & Transactions",
            subtitleBn = "ম্যানুয়াল, ভয়েস, রসিদ স্ক্যান ও ওয়ালেট",
            subtitleEn = "Manual, voice, receipt scan & wallets",
            options = listOf(
                SupportQuickOption(
                    id = "tx_manual",
                    categoryId = SupportCategoryId.TRANSACTIONS_ENTRY,
                    labelBn = "ম্যানুয়াল এন্ট্রি ও ক্যালকুলেটর",
                    labelEn = "Manual Entry & Calculator",
                    guideBn = "ম্যানুয়াল লেনদেন যোগ করার নিয়ম:\n" +
                        "১. নিচের নেভিগেশন বারের মাঝের (+) বাটনে ট্যাপ করুন।\n" +
                        "২. উপরে 'ব্যয়', 'আয়' বা 'স্থানান্তর' বেছে নিন।\n" +
                        "৩. বিল্ট-ইন ক্যালকুলেটর কিপ্যাডে সরাসরি হিসাব (যেমন: 250+180) লিখতে পারবেন।\n" +
                        "৪. ওয়ালেট, ক্যাটাগরি/সাব-ক্যাটাগরি, তারিখ ও নোট দিয়ে 'লেনদেন সংরক্ষণ' চাপুন।"
                ),
                SupportQuickOption(
                    id = "tx_voice",
                    categoryId = SupportCategoryId.TRANSACTIONS_ENTRY,
                    labelBn = "ভয়েস ও স্মার্ট এন্ট্রি",
                    labelEn = "Voice & Smart Entry",
                    guideBn = "ভয়েস ও স্মার্ট এন্ট্রি ব্যবহারের নিয়ম:\n" +
                        "১. হোম স্ক্রিনের 'ভয়েস এন্ট্রি' শর্টকাটে ট্যাপ করুন।\n" +
                        "২. 'মাইকে বলুন' চেপে বাংলা বা ইংরেজিতে বলুন—যেমন: \"বিকাশ থেকে ৩৮০ টাকা স্বপ্ন বাজার\"।\n" +
                        "৩. চাইলে মাইক ছাড়াই নিচের বক্সে টাইপ করে 'স্মার্ট বিশ্লেষণ' চাপতে পারবেন।\n" +
                        "৪. শনাক্তকৃত পরিমাণ, খাত ও ওয়ালেট মিলিয়ে সংরক্ষণ করুন।"
                ),
                SupportQuickOption(
                    id = "tx_receipt",
                    categoryId = SupportCategoryId.TRANSACTIONS_ENTRY,
                    labelBn = "রসিদ স্ক্যান ও ছবি আপলোড",
                    labelEn = "Receipt Scan & Photo",
                    guideBn = "রসিদ স্ক্যান করার ধাপ:\n" +
                        "১. হোম বা অ্যাক্টিভিটি ট্যাব থেকে 'রসিদ স্ক্যান' খুলুন।\n" +
                        "২. সরাসরি ছবি তুলতে 'ক্যামেরা দিয়ে স্ক্যান' অথবা সংরক্ষিত ছবির জন্য 'রসিদের ছবি বাছাই করুন' ট্যাপ করুন।\n" +
                        "৩. এআই রসিদ থেকে দোকান, মোট টাকা ও খাত বের করে দেবে—যাচাই করে সংরক্ষণ করুন।"
                ),
                SupportQuickOption(
                    id = "tx_wallet_cat",
                    categoryId = SupportCategoryId.TRANSACTIONS_ENTRY,
                    labelBn = "ওয়ালেট ও ক্যাটাগরি পরিচালনা",
                    labelEn = "Wallets & Categories",
                    guideBn = "ওয়ালেট ও ক্যাটাগরি সাজানোর নিয়ম:\n" +
                        "• নতুন ব্যাংক, ক্রেডিট কার্ড, বিকাশ/নগদ/রকেট/উপায় বা নগদ ওয়ালেট যোগ করতে হোমের 'সকল অ্যাকাউন্ট >' অথবা সেটিংস > 'অ্যাকাউন্ট ও কার্ড পরিচালনা' খুলুন।\n" +
                        "• ক্যাটাগরি ও সাব-ক্যাটাগরি যোগ, এডিট বা ড্র্যাগ করে সাজাতে সেটিংস > 'ক্যাটাগরি' পেজে যান (খরচ, আয় ও ট্রান্সফার ট্যাবে লং-প্রেস করে সাজানো যায়)।"
                )
            )
        ),
        SupportCategory(
            id = SupportCategoryId.LIVE_MARKET,
            titleBn = "লাইভ বাজার ও কারেন্সি রেট",
            titleEn = "Live Market & Rates",
            subtitleBn = "কারেন্সি কনভার্টার, সোনার দাম ও অফলাইন ক্যাশ",
            subtitleEn = "Currency converter, gold & offline cache",
            options = listOf(
                SupportQuickOption(
                    id = "mkt_currency",
                    categoryId = SupportCategoryId.LIVE_MARKET,
                    labelBn = "কারেন্সি কনভার্টার (১ থেকে শুরু)",
                    labelEn = "Currency Converter (From 1)",
                    guideBn = "মাল্টি-কারেন্সি কনভার্টার ব্যবহার:\n" +
                        "১. হোম স্ক্রিনে 'আর্থিক স্বাস্থ্য' (Financial Health)-এর ঠিক নিচে হাইলাইট করা 'লাইভ বাজার ও রেট' কার্ডে ট্যাপ করুন।\n" +
                        "২. ৫টি অপশন থেকে 'মুদ্রা রূপান্তর' বেছে নিয়ে ১ (সর্বনিম্ন) থেকে যেকোনো পরিমাণ লিখুন।\n" +
                        "৩. KWD, SAR, AED, QAR, BHD, USD, EUR, GBP, INR, MYR, PKR, LKR, NPR, BTN ও BDT—যেকোনো দুটি মুদ্রা অদলবদল (⇄) করে তাৎক্ষণিক হিসাব দেখুন।"
                ),
                SupportQuickOption(
                    id = "mkt_gold",
                    categoryId = SupportCategoryId.LIVE_MARKET,
                    labelBn = "সোনার দাম (ভরি ও গ্রাম)",
                    labelEn = "Gold Price (Vori & Gram)",
                    guideBn = "স্বর্ণের দর ও হিসাব:\n" +
                        "১. হোম স্ক্রিন > 'আর্থিক স্বাস্থ্য'-এর নিচে 'লাইভ বাজার ও রেট' > 'স্বর্ণের দর' অপশনে ট্যাপ করুন।\n" +
                        "২. ২২ ক্যারেট, ২১ ক্যারেট, ১৮ ক্যারেট বা সনাতন সোনা এবং প্রতি ভরি (১১.৬৬৪ গ্রাম) অথবা প্রতি গ্রাম বেছে নিন।\n" +
                        "৩. চাইলে ৫% ভ্যাট ও মজুরি অপশন চালু করে এবং পছন্দের মুদ্রায় আনুমানিক মোট দাম দেখতে পারবেন।"
                ),
                SupportQuickOption(
                    id = "mkt_tools",
                    categoryId = SupportCategoryId.LIVE_MARKET,
                    labelBn = "নিত্যপণ্য, FDR/DPS ও রেমিট্যান্স",
                    labelEn = "Essentials, FDR/DPS & Remittance",
                    guideBn = "অতিরিক্ত বাজার ক্যালকুলেটর:\n" +
                        "• নিত্যপণ্য: চাল, তেল, ডিম, পেঁয়াজ ইত্যাদি খুঁজে বাজার তালিকার মোট হিসাব করুন।\n" +
                        "• FDR ও DPS: মেয়াদ, সুদের হার ও ই-টিআইএন (১০% বনাম ১৫% কর) অনুযায়ী নিট প্রাপ্য দেখুন।\n" +
                        "• রেমিট্যান্স ও বিকাশ/নগদ: ২.৫% সরকারি প্রণোদনাসহ প্রাপ্য টাকা এবং ক্যাশ-আউট চার্জ তাৎক্ষণিক হিসাব করুন।"
                ),
                SupportQuickOption(
                    id = "mkt_offline_cache",
                    categoryId = SupportCategoryId.LIVE_MARKET,
                    labelBn = "অফলাইন ক্যাশ ও লাইভ সিঙ্ক",
                    labelEn = "Offline Cache & Auto-Sync",
                    guideBn = "অটো-সিঙ্ক ও অফলাইন ব্যবহার:\n" +
                        "• ইন্টারনেট চালু থাকলেই অ্যাপ দিনে অন্তত একবার এবং সংযোগ ফিরে এলে স্বয়ংক্রিয়ভাবে লাইভ রেট আপডেট করে লোকাল ক্যাশে সংরক্ষণ করে।\n" +
                        "• অফলাইনে থাকলেও সর্বশেষ সংরক্ষিত রেট দিয়ে সব কনভার্টার ও ক্যালকুলেটর নির্বিঘ্নে কাজ করে। ইন্টারনেট চালু করলে অফলাইন নোটিশ স্বয়ংক্রিয়ভাবে সরে যায়।"
                )
            )
        ),
        SupportCategory(
            id = SupportCategoryId.INSIGHTS_REPORTS,
            titleBn = "ইনসাইটস ও রিপোর্ট এক্সপোর্ট",
            titleEn = "Insights & Report Export",
            subtitleBn = "বিশ্লেষণ, ভয়েস সারসংক্ষেপ ও PDF/CSV",
            subtitleEn = "Analytics, voice summary & PDF/CSV",
            options = listOf(
                SupportQuickOption(
                    id = "ins_periods",
                    categoryId = SupportCategoryId.INSIGHTS_REPORTS,
                    labelBn = "দৈনিক, সাপ্তাহিক ও মাসিক বিশ্লেষণ",
                    labelEn = "Daily, Weekly & Monthly Analytics",
                    guideBn = "আর্থিক বিশ্লেষণ দেখার নিয়ম:\n" +
                        "১. নিচের 'ইনসাইটস' ট্যাবে যান।\n" +
                        "২. উপরের ফিল্টার থেকে 'দৈনিক', 'এই সপ্তাহ', 'এই মাস' অথবা 'গত মাস' বেছে নিন।\n" +
                        "৩. এখানে মোট আয়, মোট ব্যয়, নিট প্রবাহ, দৈনিক গড়, শীর্ষ ব্যয়ের খাত ও ট্রেন্ড চার্ট স্পষ্টভাবে দেখা যায়।"
                ),
                SupportQuickOption(
                    id = "ins_voice_summary",
                    categoryId = SupportCategoryId.INSIGHTS_REPORTS,
                    labelBn = "ভয়েস আর্থিক সারসংক্ষেপ",
                    labelEn = "Voice Financial Summary",
                    guideBn = "ভয়েস আর্থিক সারসংক্ষেপ ব্যবহার:\n" +
                        "১. ইনসাইটস স্ক্রিনে 'ভয়েস আর্থিক সারসংক্ষেপ' কার্ডে 'সারসংক্ষেপ শুনুন' চাপুন।\n" +
                        "২. চলার সময় পজ, রিজিউম ও স্টপ কন্ট্রোল ব্যবহার করতে পারবেন।\n" +
                        "৩. 'টেক্সট সারসংক্ষেপ দেখুন' ট্যাপ করে অফলাইনেও পুরো বাংলা বিশ্লেষণ পড়তে পারবেন।"
                ),
                SupportQuickOption(
                    id = "ins_export",
                    categoryId = SupportCategoryId.INSIGHTS_REPORTS,
                    labelBn = "PDF ও CSV রিপোর্ট এক্সপোর্ট",
                    labelEn = "PDF & CSV Export & Share",
                    guideBn = "মাসিক রিপোর্ট PDF/CSV এক্সপোর্ট ও শেয়ার:\n" +
                        "১. ইনসাইটস স্ক্রিনের উপরে 'এক্সপোর্ট' বাটন অথবা 'মাসিক আর্থিক রিপোর্ট এক্সপোর্ট' কার্ডে যান।\n" +
                        "২. ফরম্যাটেড বিবরণীর জন্য 'PDF শেয়ার' বা Excel/Sheets-এর জন্য 'CSV শেয়ার' ট্যাপ করুন।\n" +
                        "৩. ফাইল তৈরি হলে সরাসরি হোয়াটসঅ্যাপ, ইমেইল বা যেকোনো অ্যাপে শেয়ার করতে পারবেন।"
                )
            )
        ),
        SupportCategory(
            id = SupportCategoryId.DATETIME_PERMISSIONS,
            titleBn = "তারিখ, সময় ও পারমিশন",
            titleEn = "Date, Time & Permissions",
            subtitleBn = "টাইমজোন সিঙ্ক, সপ্তাহের শুরু, মাইক/ক্যামেরা/লোকেশন",
            subtitleEn = "Timezone sync, week start & permissions",
            options = listOf(
                SupportQuickOption(
                    id = "dt_timezone",
                    categoryId = SupportCategoryId.DATETIME_PERMISSIONS,
                    labelBn = "লোকাল টাইম ও টাইমজোন সিঙ্ক",
                    labelEn = "Local Time & Timezone Sync",
                    guideBn = "ডিভাইসের লোকাল সময় ও টাইমজোন সিঙ্ক:\n" +
                        "• 'আমার হিসাব' সবসময় আপনার মোবাইলের বর্তমান লোকাল তারিখ, সময় ও টাইমজোন স্বয়ংক্রিয়ভাবে অনুসরণ করে।\n" +
                        "• বিদেশ ভ্রমণ বা টাইমজোন বদলালে দৈনিক, সাপ্তাহিক ও মাসিক হিসাবের সীমা স্বয়ংক্রিয়ভাবে আপডেট হয়।\n" +
                        "• সেটিংস > 'তারিখ ও সময়' কার্ডে লাইভ টাইমজোন দেখতে ও রিফ্রেশ করতে পারবেন।"
                ),
                SupportQuickOption(
                    id = "dt_weekstart",
                    categoryId = SupportCategoryId.DATETIME_PERMISSIONS,
                    labelBn = "সপ্তাহের শুরু পরিবর্তন",
                    labelEn = "Change Start of Week",
                    guideBn = "সপ্তাহের শুরু সেট করার নিয়ম:\n" +
                        "১. সেটিংস > 'তারিখ ও সময়' > 'সপ্তাহের শুরু' অপশনে ট্যাপ করুন।\n" +
                        "২. শনিবার থেকে শুক্রবারের যেকোনো দিন বেছে নিন।\n" +
                        "৩. সাথে সাথেই ইনসাইটসের সাপ্তাহিক বিশ্লেষণ নতুন দিন অনুযায়ী হালনাগাদ হয়ে যাবে।"
                ),
                SupportQuickOption(
                    id = "dt_mic_cam",
                    categoryId = SupportCategoryId.DATETIME_PERMISSIONS,
                    labelBn = "মাইক্রোফোন ও ক্যামেরা পারমিশন",
                    labelEn = "Mic & Camera Permissions",
                    guideBn = "মাইক্রোফোন ও ক্যামেরা পারমিশন:\n" +
                        "• অ্যাপ প্রথমবার চালুর সময় মাইক্রোফোন, ক্যামেরা ও লোকেশন অনুমতি চাওয়া হয়—'Allow' দিলে সব স্মার্ট ফিচার চালু থাকে।\n" +
                        "• মাইক্রোফোন বন্ধ থাকলেও টাইপ করে এন্ট্রি করা যায়, আর ক্যামেরা বন্ধ থাকলেও গ্যালারি থেকে রসিদের ছবি নেওয়া যায়।\n" +
                        "• পরে অনুমতি বদলাতে চাইলে ফোনের Android Settings > Apps > আমার হিসাব > Permissions থেকে পরিবর্তন করতে পারবেন।"
                ),
                SupportQuickOption(
                    id = "dt_location",
                    categoryId = SupportCategoryId.DATETIME_PERMISSIONS,
                    labelBn = "গুগল লোকেশন ও ভ্রমণ মোড",
                    labelEn = "Google Location & Travel",
                    guideBn = "গুগল লোকেশন পারমিশন:\n" +
                        "• ভ্রমণের সময় বর্তমান অঞ্চল ও টাইমজোন প্রসঙ্গ ঠিক রাখতে ফোরগ্রাউন্ড লোকেশন ব্যবহৃত হয়।\n" +
                        "• এটি সম্পূর্ণ ঐচ্ছিক—অনুমতি না দিলেও আপনার মোবাইলের টাইমজোন দিয়ে অফলাইন ও ম্যানুয়াল হিসাব স্বাভাবিকভাবে চলবে।"
                )
            )
        ),
        SupportCategory(
            id = SupportCategoryId.BACKUP_SECURITY,
            titleBn = "ব্যাকআপ, নিরাপত্তা ও অফলাইন মোড",
            titleEn = "Backup, Security & Offline",
            subtitleBn = "লোকাল ভল্ট, JSON/CSV ব্যাকআপ ও অ্যাপ লক",
            subtitleEn = "Local vault, backup/restore & app lock",
            options = listOf(
                SupportQuickOption(
                    id = "sec_offline",
                    categoryId = SupportCategoryId.BACKUP_SECURITY,
                    labelBn = "১০০% অফলাইন লোকাল ভল্ট",
                    labelEn = "100% Offline Local Vault",
                    guideBn = "অফলাইন লোকাল ভল্ট:\n" +
                        "• আপনার সব ওয়ালেট, লেনদেন, বাজেট, সেভিংস গোল এবং দেনা-পাওনা সম্পূর্ণ নিরাপদে আপনার নিজস্ব ফোনে সংরক্ষিত থাকে।\n" +
                        "• প্রতিদিনের হিসাব রাখতে কোনো ইন্টারনেটের প্রয়োজন হয় না—ইন্টারনেট শুধু লাইভ বাজারদর, ভয়েস/রসিদ এআই ও লাইভ সাপোর্টে লাগে।"
                ),
                SupportQuickOption(
                    id = "sec_backup",
                    categoryId = SupportCategoryId.BACKUP_SECURITY,
                    labelBn = "ব্যাকআপ ও রিস্টোর (JSON/CSV)",
                    labelEn = "Backup & Restore (JSON/CSV)",
                    guideBn = "ডাটা ব্যাকআপ ও রিস্টোর করার নিয়ম:\n" +
                        "১. সেটিংস > 'ব্যাকআপ ও রিস্টোর' পেজে যান।\n" +
                        "২. সম্পূর্ণ ভল্ট সংরক্ষণ করতে 'JSON' এবং স্প্রেডশিটের জন্য 'CSV' ডাউনলোড করুন।\n" +
                        "৩. নতুন ফোন বা আগের ডাটা ফিরিয়ে আনতে 'ইম্পোর্ট ও রিস্টোর' থেকে সংরক্ষিত `.json` ফাইল বেছে নিন।"
                ),
                SupportQuickOption(
                    id = "sec_applock",
                    categoryId = SupportCategoryId.BACKUP_SECURITY,
                    labelBn = "ফিঙ্গারপ্রিন্ট, ফেস ও পিন লক",
                    labelEn = "Biometric & PIN Lock",
                    guideBn = "অ্যাপ লক ও বায়োমেট্রিক নিরাপত্তা:\n" +
                        "১. সেটিংস > 'নিরাপত্তা ও সিস্টেম' সেকশনে যান।\n" +
                        "২. 'লঞ্চে বায়োমেট্রিক আনলক' চালু করলে অ্যাপ খোলার সময় ফিঙ্গারপ্রিন্ট বা ফেস যাচাই চাইবে।\n" +
                        "৩. পাশাপাশি 'অ্যাপ স্ক্রিন লক' থেকে ৪-ডিজিট পিন সেট করতে পারবেন—অ্যাপ ব্যাকগ্রাউন্ডে গেলে স্বয়ংক্রিয়ভাবে ভল্ট লক হয়ে যায়।"
                )
            )
        ),
        SupportCategory(
            id = SupportCategoryId.OTHER_SUPPORT,
            titleBn = "অন্যান্য সমস্যা / লাইভ কাস্টমার কেয়ার",
            titleEn = "Other Issues / Live Care",
            subtitleBn = "সমস্যা সমাধান বা সরাসরি সহায়তা",
            subtitleEn = "Troubleshooting or live support",
            options = listOf(
                SupportQuickOption(
                    id = "oth_crash",
                    categoryId = SupportCategoryId.OTHER_SUPPORT,
                    labelBn = "অ্যাপ ধীরগতি বা ক্র্যাশ সমস্যা",
                    labelEn = "App Slow or Crash Issue",
                    guideBn = "অ্যাপ ধীরগতি বা ক্র্যাশ হলে প্রথমে এই ধাপগুলো চেষ্টা করুন:\n" +
                        "১. অ্যাপটি পুরোপুরি বন্ধ করে পুনরায় চালু করুন।\n" +
                        "২. ফোনে পর্যাপ্ত স্টোরেজ আছে কিনা যাচাই করুন।\n" +
                        "৩. সেটিংস > 'ব্যাকআপ ও রিস্টোর' থেকে একটি `.json` ব্যাকআপ সংরক্ষণ করে রাখুন।\n" +
                        "এই ধাপগুলোতেও সমস্যা থেকে গেলে নিচের বাটনে ট্যাপ করে লাইভ সাপোর্টে জানাতে পারবেন।"
                ),
                SupportQuickOption(
                    id = "oth_balance",
                    categoryId = SupportCategoryId.OTHER_SUPPORT,
                    labelBn = "হিসাব বা ব্যালেন্স মিলছে না",
                    labelEn = "Balance Mismatch Help",
                    guideBn = "ব্যালেন্স বা হিসাব না মিললে যাচাই করুন:\n" +
                        "১. হোম বা অ্যাক্টিভিটি ট্যাবে সাম্প্রতিক লেনদেনে 'আয়', 'ব্যয়' ও 'স্থানান্তর' সঠিকভাবে দেওয়া আছে কিনা দেখুন।\n" +
                        "২. ক্রেডিট কার্ডের বকেয়া 'নিট সম্পদ' অংশে দায় হিসেবে দেখায়, তরল ব্যালেন্সে নয়।\n" +
                        "৩. ইনসাইটসের তারিখ ফিল্টার ('দৈনিক'/'এই সপ্তাহ'/'এই মাস') ও 'সপ্তাহের শুরু' মিলিয়ে দেখুন।"
                ),
                SupportQuickOption(
                    id = "oth_live_care",
                    categoryId = SupportCategoryId.OTHER_SUPPORT,
                    labelBn = "সরাসরি কাস্টমার কেয়ারের সাথে কথা",
                    labelEn = "Talk to Live Customer Care",
                    guideBn = "আমি প্রথমে এখানেই আপনার সমস্যার দ্রুত সমাধান করে দিতে চেষ্টা করছি। অনুগ্রহ করে নিচে সংক্ষেপে লিখুন আপনি কোন ফিচার বা সমস্যার মুখোমুখি হচ্ছেন (যেমন: ব্যাকআপ, লক, হিসাব বা ক্র্যাশ)—আর যদি ইতিমধ্যে চেষ্টা করেও সমাধান না হয়ে থাকে, তাহলে নিচে 'সমাধান হয়নি — লাইভ সাপোর্ট চাই' ট্যাপ করুন।",
                    isLiveCareTrigger = true
                )
            )
        )
    )

    fun findCategory(id: SupportCategoryId): SupportCategory =
        categories.first { it.id == id }

    fun allOptions(): List<SupportQuickOption> = categories.flatMap { it.options }
}

object CustomerSupportEngine {

    private val pureGreetingPatterns = listOf(
        "ওয়ালাইকুম আসসালাম",
        "ওয়ালাইকুম আসসালাম",
        "ওয়ালাইকুম সালাম",
        "ওয়ালাইকুম সালাম",
        "আসসালামু আলাইকুম",
        "আসসালামুয়ালাইকুম",
        "সালাম",
        "হ্যালো",
        "হ্যালো!",
        "হাই",
        "হাই!",
        "নমস্কার",
        "আদাব",
        "walaikum",
        "wa alaikum",
        "assalamu",
        "salam",
        "hello",
        "hi",
        "hey"
    )

    private val criticalEscalationKeywords = listOf(
        "ক্র্যাশ", "crash", "বন্ধ হয়ে যায়", "বন্ধ হয়ে যায়", "ওপেন হচ্ছে না", "খুলছে না",
        "ডাটা মুছে", "ডাটা হারিয়ে", "ডাটা হারিয়ে", "data lost", "deleted all",
        "পিন ভুলে", "pin ভুলে", "লক খুলছে না", "ভল্ট লক", "আনলক হচ্ছে না", "forgot pin", "locked out",
        "জরুরি", "emergency", "urgent", "টাকা হারিয়ে", "গুরুতর", "critical", "bug", "বাগ", "এরর", "error"
    )

    private val explicitHumanSupportKeywords = listOf(
        "হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "whatsapp", "কাস্টমার কেয়ার", "কাস্টমার কেয়ার",
        "মানুষ", "এজেন্ট", "লাইভ সাপোর্ট", "সরাসরি কথা", "হেল্পলাইন", "ফোন নম্বর", "নাম্বার দিন",
        "সমাধান হয়নি", "সমাধান হয়নি", "কাজ করছে না", "হচ্ছে না", "human", "live support", "agent", "call center"
    )

    fun isPureGreeting(rawInput: String): Boolean {
        val cleaned = rawInput.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[!?,।.]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty() || cleaned.length > 40) return false
        val wordCount = cleaned.split(" ").size
        if (wordCount > 5) return false

        // If the user also asks a question/issue alongside the greeting, handle the question directly
        val hasQuestionWords = listOf(
            "কিভাবে", "কীভাবে", "সমস্যা", "কাজ", "ব্যাকআপ", "রেট", "দাম", "লক", "পিন",
            "রসিদ", "ভয়েস", "ভয়েস", "এক্সপোর্ট", "pdf", "csv", "how", "help", "issue", "problem"
        ).any { cleaned.contains(it) }
        if (hasQuestionWords) return false

        return pureGreetingPatterns.any { pattern ->
            cleaned == pattern || cleaned.startsWith("$pattern ") || cleaned.contains(pattern)
        }
    }

    fun isCriticalIssue(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return criticalEscalationKeywords.any { lower.contains(it) }
    }

    fun isExplicitHumanRequest(input: String): Boolean {
        val lower = input.lowercase(Locale.ROOT)
        return explicitHumanSupportKeywords.any { lower.contains(it) }
    }

    /**
     * Decides whether WhatsApp Customer Care card (+8801708901947) is allowed to appear.
     * Strictly enforces Rule 4:
     * - NEVER on initial greeting or standard how-to questions.
     * - ONLY when issue is critical/emergency, cannot be resolved in chat, or user explicitly
     *   requests live human support AFTER troubleshooting has already been attempted.
     */
    fun shouldEscalateToWhatsApp(
        userMessage: String,
        hasAttemptedTroubleshooting: Boolean,
        aiSuggestedEscalation: Boolean = false
    ): Boolean {
        if (isPureGreeting(userMessage)) return false
        if (isCriticalIssue(userMessage)) return true
        if (hasAttemptedTroubleshooting && (isExplicitHumanRequest(userMessage) || aiSuggestedEscalation)) {
            return true
        }
        return false
    }

    fun buildWhatsAppEscalationMessage(isCritical: Boolean): String {
        return if (isCritical) {
            "আমি বুঝতে পারছি এটি একটি জরুরি বা গুরুত্বপূর্ণ সমস্যা। দ্রুত ও সরাসরি সহায়তার জন্য নিচের হোয়াটসঅ্যাপ কাস্টমার কেয়ার বাটনে ট্যাপ করে আমাদের টিমের সাথে যোগাযোগ করুন।"
        } else {
            "চ্যাটের ধাপগুলোতে সমস্যাটির সমাধান না হওয়ায় দুঃখিত। সরাসরি লাইভ সহায়তার জন্য নিচের হোয়াটসঅ্যাপ কাস্টমার কেয়ার কার্ড থেকে আমাদের সাথে যোগাযোগ করতে পারেন।"
        }
    }

    /**
     * Offline/local knowledge matcher so built-in guides answer accurately even without internet.
     */
    fun matchLocalGuide(userMessage: String): SupportQuickOption? {
        val q = userMessage.lowercase(Locale.ROOT)
        val rules: List<Pair<List<String>, String>> = listOf(
            listOf("রসিদ", "receipt", "স্ক্যান", "মেমো", "ক্যামেরা ছবি") to "tx_receipt",
            listOf("ভয়েস এন্ট্রি", "ভয়েস এন্ট্রি", "মুখে বল", "voice entry", "মাইকে বল") to "tx_voice",
            listOf("ক্যালকুলেটর", "ম্যানুয়াল", "ম্যানুয়াল", "লেনদেন যোগ", "খরচ যোগ", "আয় যোগ", "আয় যোগ", "ট্রান্সফার") to "tx_manual",
            listOf("ওয়ালেট", "ওয়ালেট", "ব্যাংক", "ক্রেডিট কার্ড", "ক্যাটাগরি", "সাব-ক্যাটাগরি", "ড্র্যাগ") to "tx_wallet_cat",
            listOf("সোনা", "স্বর্ণ", "gold", "ভরি", "ক্যারেট", "22k", "21k") to "mkt_gold",
            listOf("ডলার", "দিনার", "রিয়াল", "রিয়াল", "দিরহাম", "কারেন্সি", "মুদ্রা", "কনভার্টার", "kwd", "sar", "aed", "usd", "eur", "inr", "bdt", "রেট") to "mkt_currency",
            listOf("নিত্যপণ্য", "চাল", "তেল", "fdr", "dps", "রেমিট্যান্স", "প্রণোদনা", "ক্যাশ-আউট", "বিকাশ চার্জ", "নগদ চার্জ") to "mkt_tools",
            listOf("ক্যাশ", "সিঙ্ক", "sync", "বাজার অফলাইন", "অটো আপডেট") to "mkt_offline_cache",
            listOf("pdf", "csv", "পিডিএফ", "সিএসভি", "রিপোর্ট এক্সপোর্ট", "শেয়ার রিপোর্ট", "শেয়ার রিপোর্ট") to "ins_export",
            listOf("সারসংক্ষেপ শুন", "ভয়েস সারসংক্ষেপ", "ভয়েস সারসংক্ষেপ", "অডিও", "tts") to "ins_voice_summary",
            listOf("ইনসাইট", "বিশ্লেষণ", "চার্ট", "দৈনিক", "মাসিক", "সাপ্তাহিক") to "ins_periods",
            listOf("সপ্তাহের শুরু", "week start", "শনিবার", "রবিবার", "সোমবার") to "dt_weekstart",
            listOf("টাইমজোন", "timezone", "তারিখ", "সময়", "সময়", "ভ্রমণ", "বিদেশ") to "dt_timezone",
            listOf("মাইক্রোফোন", "ক্যামেরা", "পারমিশন", "permission", "অনুমতি") to "dt_mic_cam",
            listOf("লোকেশন", "location", "জিপিএস", "gps") to "dt_location",
            listOf("ব্যাকআপ", "রিস্টোর", "backup", "restore", "json", "ইম্পোর্ট") to "sec_backup",
            listOf("পিন", "pin", "ফিঙ্গারপ্রিন্ট", "ফেস", "বায়োমেট্রিক", "বায়োমেট্রিক", "অ্যাপ লক", "লক") to "sec_applock",
            listOf("অফলাইন", "offline", "ভল্ট", "নিরাপদ", "ডাটা কোথায়", "ডাটা কোথায়") to "sec_offline",
            listOf("ক্র্যাশ", "crash", "ধীর", "হ্যাং", "slow") to "oth_crash",
            listOf("মিলছে না", "ব্যালেন্স ভুল", "mismatch", "কম দেখাচ্ছে", "বেশি দেখাচ্ছে") to "oth_balance"
        )

        val matchedId = rules.firstOrNull { (keywords, _) ->
            keywords.any { kw -> q.contains(kw) }
        }?.second ?: return null

        return CustomerSupportCatalog.allOptions().firstOrNull { it.id == matchedId }
    }

    /**
     * Full knowledge prompt for Firebase AI Logic (`gemini-3.8-flash`) so custom queries receive
     * accurate, step-by-step Bangla answers grounded in the real features of "আমার হিসাব".
     */
    const val AMAR_HISAB_SYSTEM_KNOWLEDGE = """
আপনি 'আমার হিসাব' (Amar Hisab) অ্যান্ড্রয়েড পার্সোনাল ফিন্যান্স অ্যাপের অফিশিয়াল কাস্টমার সাপোর্ট অ্যাসিস্ট্যান্ট।
সবসময় ভদ্র, স্পষ্ট, সংক্ষিপ্ত ও ধাপে ধাপে সাবলীল বাংলায় উত্তর দিন।

'আমার হিসাব' অ্যাপের সম্পূর্ণ ফিচার জ্ঞান:
১. হিসাব ও লেনদেন এন্ট্রি:
- নিচের নেভিগেশন বারের (+) বাটনে ট্যাপ করে ব্যয়, আয় ও স্থানান্তর (Transfer) যোগ করা যায়।
- বিল্ট-ইন ক্যালকুলেটর কিপ্যাড দিয়ে সরাসরি যোগ/বিয়োগ/গুণ/ভাগ করা যায়।
- হোমে 'ভয়েস এন্ট্রি' (মাইক্রোফোন বা টেক্সট টাইপ করে এআই বিশ্লেষণ) এবং 'রসিদ স্ক্যান' (ক্যামেরা বা গ্যালারি থেকে ছবি দিয়ে জেমিনি ভিশন স্ক্যান) রয়েছে।
- অ্যাকাউন্ট ও কার্ড: নগদ (Cash), বাংলাদেশি ব্যাংক, ডেবিট/ক্রেডিট কার্ড (লিমিট ও ব্যবহারের শতকরা হারসহ) এবং এমএফএস (বিকাশ, নগদ, রকেট, উপায়)।
- সেটিংস > 'ক্যাটাগরি' পেজে খরচ, আয় ও ট্রান্সফার ট্যাবে লং-প্রেস করে ড্র্যাগ সাজানো, নতুন ক্যাটাগরি/সাব-ক্যাটাগরি তৈরি, রং ও আইকন পরিবর্তন এবং এডিট/ডিলিট করা যায়।

২. পরিকল্পনা (Plan ট্যাব):
- মাসিক বাজেট ও খাতভিত্তিক বাজেট সীমা, সেভিংস গোল (ওয়ালেট থেকে ফান্ড জমা), এবং দেনা-পাওনা (আংশিক বা সম্পূর্ণ পরিশোধ)।

৩. ইনসাইটস ও রিপোর্ট এক্সপোর্ট:
- দৈনিক, এই সপ্তাহ, এই মাস এবং গত মাসের আয়-ব্যয়, নিট প্রবাহ, দৈনিক গড়, শীর্ষ ব্যয়ের খাত ও চার্ট।
- ভয়েস আর্থিক সারসংক্ষেপ (Play/Pause/Stop কন্ট্রোল ও অফলাইন টেক্সট প্রিভিউ)।
- মাসিক রিপোর্ট সরাসরি PDF স্টেটমেন্ট বা CSV স্প্রেডশিট হিসেবে এক্সপোর্ট ও শেয়ার (ইনসাইটস > এক্সপোর্ট বাটন বা মাসিক রিপোর্ট কার্ড)।

৪. লাইভ বাজার ও রেট (হোম স্ক্রিনে 'আর্থিক স্বাস্থ্য'-এর নিচে):
- হোম স্ক্রিনের হাইলাইট করা 'লাইভ বাজার ও রেট' কার্ডে ট্যাপ করলে ৫টি মূল অপশন খোলে: কারেন্সি রেট, স্বর্ণের দর, নিত্যপণ্য, ব্যাংক FDR ও DPS, এবং রেমিট্যান্স ও ওয়ালেট চার্জ।
- ১৫টি মুদ্রার কারেন্সি কনভার্টার ১ (সর্বনিম্ন) থেকে শুরু: KWD, SAR, AED, QAR, BHD, INR, LKR, MYR, NPR, BTN, PKR, BDT, USD, EUR, GBP—অদলবদল (⇄) বাটনসহ।
- স্বর্ণের দর (২২K/২১K/১৮K/সনাতন, প্রতি ভরি ও প্রতি গ্রাম, ৫% ভ্যাট/মজুরি অপশন ও মুদ্রা পরিবর্তন)।
- নিত্যপণ্য তালিকা ও বাজার বাস্কেট, ব্যাংক FDR/DPS ক্যালকুলেটর (ই-টিআইএন ১০%/১৫% করসহ), রেমিট্যান্স + ২.৫% সরকারি প্রণোদনা, এবং বিকাশ/নগদ ক্যাশ-আউট চার্জ ক্যালকুলেটর।
- অটো-সিঙ্ক ও অফলাইন ক্যাশ: ইন্টারনেট থাকলে স্বয়ংক্রিয়ভাবে রেট আপডেট ও সংরক্ষণ হয়; অফলাইনে সর্বশেষ সংরক্ষিত রেট দিয়ে সব ক্যালকুলেটর চলে।

৫. তারিখ, সময় ও পারমিশন:
- ডিভাইসের বর্তমান লোকাল তারিখ, সময় ও টাইমজোন স্বয়ংক্রিয়ভাবে সিঙ্ক হয় (বিদেশ ভ্রমণেও)।
- সেটিংস > 'তারিখ ও সময়' > 'সপ্তাহের শুরু' (শনিবার–শুক্রবার)।
- অ্যাপ প্রথমবার চালুর সময় মাইক্রোফোন (ভয়েস এন্ট্রি), ক্যামেরা (রসিদ স্ক্যান) ও গুগল লোকেশন (ভ্রমণ ও অঞ্চল প্রসঙ্গ) পারমিশন চাওয়া হয়। সব পারমিশন ঐচ্ছিক—বন্ধ থাকলেও ম্যানুয়াল ও অফলাইন হিসাব কাজ করে, এবং ফোনের সিস্টেম সেটিংস থেকে যেকোনো সময় পরিবর্তন করা যায়।

৬. ব্যাকআপ, নিরাপত্তা ও অফলাইন মোড:
- ১০০% অফলাইন লোকাল ভল্ট (ফোনে নিরাপদে সংরক্ষিত)।
- সেটিংস > 'ব্যাকআপ ও রিস্টোর': JSON পূর্ণাঙ্গ ব্যাকআপ, CSV লেজার এক্সপোর্ট এবং JSON থেকে রিস্টোর।
- সেটিংস > 'নিরাপত্তা ও সিস্টেম': লঞ্চে বায়োমেট্রিক আনলক (ফিঙ্গারপ্রিন্ট/ফেস) এবং ৪-ডিজিট পিন লক।

গুরুত্বপূর্ণ নিয়মাবলি:
- উত্তরের মধ্যে কখনো নিজে থেকে কোনো ফোন বা হোয়াটসঅ্যাপ নম্বর লিখবেন না (অ্যাপের UI কার্ড প্রয়োজনে নিজেই তা দেখাবে)।
- সাধারণ ব্যবহারের প্রশ্ন বা গাইডের ক্ষেত্রে কখনোই needsHumanEscalation = true দেবেন না—সবসময় আগে চ্যাটেই স্পষ্ট ও সম্পূর্ণ সমাধান দিন।
- শুধুমাত্র গুরুতর/জরুরি সমস্যা (যেমন: অ্যাপ ক্র্যাশ, ডাটা হারিয়ে যাওয়া, পিন ভুলে লক হয়ে যাওয়া, বাগ) অথবা আগের কথোপকথনে সমাধান চেষ্টা করার পরও ব্যবহারকারী সরাসরি হিউম্যান/লাইভ সাপোর্ট চাইলে needsHumanEscalation = true দিন।
"""
}
