package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPrefs
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.data.model.StartOfWeek
import com.example.data.model.TransactionType
import com.example.localization.AppLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun exportJson(db: AppDatabase, prefs: UserPrefs): String {
        val accounts = db.accountDao().getAllAccountsOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()
        val goals = db.savingsGoalDao().getAllGoalsOnce()
        val loans = db.loanDao().getAllLoansOnce()
        val categoryBudgets = db.categoryBudgetDao().getAllCategoryBudgetsOnce()

        val root = JSONObject().apply {
            put("app", "Amar Hisab")
            put("version", "1.0.0")
            put("exportedAt", System.currentTimeMillis())

            put("preferences", JSONObject().apply {
                put("userName", prefs.userName)
                put("monthlyBudget", prefs.monthlyBudget)
                put("language", prefs.language.code)
                put("isDarkTheme", prefs.isDarkTheme)
                put("enableSubcategories", prefs.enableSubcategories)
                put("startOfWeek", prefs.startOfWeek.name)
                put("hapticFeedbackEnabled", prefs.hapticFeedbackEnabled)
            })

            put("accounts", JSONArray().apply {
                accounts.forEach { acc ->
                    put(JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("type", acc.type.name)
                        put("balance", acc.balance)
                        put("creditLimit", acc.creditLimit)
                        put("bankOrProvider", acc.bankOrProvider)
                        put("accountSubtype", acc.accountSubtype)
                        put("cardNetwork", acc.cardNetwork)
                        put("accountNumberLast4", acc.accountNumberLast4)
                        put("colorArgb", acc.colorArgb)
                        put("createdAt", acc.createdAt)
                    })
                }
            })

            put("transactions", JSONArray().apply {
                transactions.forEach { tx ->
                    put(JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("amount", tx.amount)
                        put("type", tx.type.name)
                        put("category", tx.category.name)
                        put("subcategory", tx.subcategory)
                        put("accountId", tx.accountId)
                        put("toAccountId", tx.toAccountId ?: JSONObject.NULL)
                        put("note", tx.note)
                        put("timestamp", tx.timestamp)
                    })
                }
            })

            put("savingsGoals", JSONArray().apply {
                goals.forEach { goal ->
                    put(JSONObject().apply {
                        put("id", goal.id)
                        put("name", goal.name)
                        put("targetAmount", goal.targetAmount)
                        put("savedAmount", goal.savedAmount)
                        put("dueDateMillis", goal.dueDateMillis ?: JSONObject.NULL)
                        put("colorArgb", goal.colorArgb)
                        put("createdAt", goal.createdAt)
                    })
                }
            })

            put("loans", JSONArray().apply {
                loans.forEach { loan ->
                    put(JSONObject().apply {
                        put("id", loan.id)
                        put("personName", loan.personName)
                        put("loanType", loan.loanType.name)
                        put("totalAmount", loan.totalAmount)
                        put("paidAmount", loan.paidAmount)
                        put("note", loan.note)
                        put("dueDateMillis", loan.dueDateMillis ?: JSONObject.NULL)
                        put("createdAt", loan.createdAt)
                    })
                }
            })

            put("categoryBudgets", JSONArray().apply {
                categoryBudgets.forEach { cb ->
                    put(JSONObject().apply {
                        put("categoryName", cb.categoryName)
                        put("limitAmount", cb.limitAmount)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun exportCsv(db: AppDatabase): String {
        val accounts = db.accountDao().getAllAccountsOnce().associateBy { it.id }
        val transactions = db.transactionDao().getAllTransactionsOnce()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("ID,Date,Title,Type,Category,Subcategory,Account,ToAccount,Amount_BDT,Note")
        transactions.forEach { tx ->
            val dateStr = fmt.format(Date(tx.timestamp))
            val accName = accounts[tx.accountId]?.name.orEmpty()
            val toAccName = tx.toAccountId?.let { accounts[it]?.name.orEmpty() }.orEmpty()
            sb.appendLine(
                listOf(
                    tx.id.toString(),
                    csvEscape(dateStr),
                    csvEscape(tx.title),
                    tx.type.name,
                    tx.category.en,
                    csvEscape(tx.subcategory),
                    csvEscape(accName),
                    csvEscape(toAccName),
                    tx.amount.toString(),
                    csvEscape(tx.note)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    suspend fun restoreFromJson(
        jsonString: String,
        db: AppDatabase,
        prefsRepo: UserPreferencesRepository
    ): Int {
        val root = JSONObject(jsonString)

        val accountsList = mutableListOf<AccountEntity>()
        val accArr = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArr.length()) {
            val o = accArr.getJSONObject(i)
            accountsList += AccountEntity(
                id = o.optLong("id", 0L),
                name = o.getString("name"),
                type = runCatching { AccountType.valueOf(o.getString("type")) }.getOrDefault(AccountType.CASH),
                balance = o.getDouble("balance"),
                creditLimit = o.optDouble("creditLimit", 0.0),
                bankOrProvider = o.optString("bankOrProvider", ""),
                accountSubtype = o.optString("accountSubtype", ""),
                cardNetwork = o.optString("cardNetwork", ""),
                accountNumberLast4 = o.optString("accountNumberLast4", ""),
                colorArgb = o.optLong("colorArgb", 0xFF0284C7L),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val txList = mutableListOf<TransactionEntity>()
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val toAccId = if (o.isNull("toAccountId")) null else o.optLong("toAccountId")
            txList += TransactionEntity(
                id = o.optLong("id", 0L),
                title = o.getString("title"),
                amount = o.getDouble("amount"),
                type = runCatching { TransactionType.valueOf(o.getString("type")) }.getOrDefault(TransactionType.EXPENSE),
                category = runCatching { ExpenseCategory.valueOf(o.getString("category")) }.getOrDefault(ExpenseCategory.OTHER),
                subcategory = o.optString("subcategory", ""),
                accountId = o.getLong("accountId"),
                toAccountId = toAccId,
                note = o.optString("note", ""),
                timestamp = o.optLong("timestamp", System.currentTimeMillis())
            )
        }

        val goalsList = mutableListOf<SavingsGoalEntity>()
        val goalArr = root.optJSONArray("savingsGoals") ?: JSONArray()
        for (i in 0 until goalArr.length()) {
            val o = goalArr.getJSONObject(i)
            goalsList += SavingsGoalEntity(
                id = o.optLong("id", 0L),
                name = o.getString("name"),
                targetAmount = o.getDouble("targetAmount"),
                savedAmount = o.getDouble("savedAmount"),
                dueDateMillis = if (o.isNull("dueDateMillis")) null else o.optLong("dueDateMillis"),
                colorArgb = o.optLong("colorArgb", 0xFF10B981L),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val loansList = mutableListOf<LoanEntity>()
        val loanArr = root.optJSONArray("loans") ?: JSONArray()
        for (i in 0 until loanArr.length()) {
            val o = loanArr.getJSONObject(i)
            loansList += LoanEntity(
                id = o.optLong("id", 0L),
                personName = o.getString("personName"),
                loanType = runCatching { LoanType.valueOf(o.getString("loanType")) }.getOrDefault(LoanType.I_OWE),
                totalAmount = o.getDouble("totalAmount"),
                paidAmount = o.optDouble("paidAmount", 0.0),
                note = o.optString("note", ""),
                dueDateMillis = if (o.isNull("dueDateMillis")) null else o.optLong("dueDateMillis"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val cbList = mutableListOf<CategoryBudgetEntity>()
        val cbArr = root.optJSONArray("categoryBudgets") ?: JSONArray()
        for (i in 0 until cbArr.length()) {
            val o = cbArr.getJSONObject(i)
            cbList += CategoryBudgetEntity(
                categoryName = o.getString("categoryName"),
                limitAmount = o.getDouble("limitAmount")
            )
        }

        db.withTransaction {
            db.transactionDao().deleteAllTransactions()
            db.accountDao().deleteAllAccounts()
            db.savingsGoalDao().deleteAllGoals()
            db.loanDao().deleteAllLoans()
            db.categoryBudgetDao().deleteAllCategoryBudgets()

            if (accountsList.isNotEmpty()) db.accountDao().insertAccounts(accountsList)
            if (txList.isNotEmpty()) db.transactionDao().insertTransactions(txList)
            if (goalsList.isNotEmpty()) db.savingsGoalDao().insertGoals(goalsList)
            if (loansList.isNotEmpty()) db.loanDao().insertLoans(loansList)
            if (cbList.isNotEmpty()) db.categoryBudgetDao().insertCategoryBudgets(cbList)
        }

        root.optJSONObject("preferences")?.let { p ->
            prefsRepo.updateProfileAndBudget(
                userName = p.optString("userName", ""),
                monthlyBudget = p.optDouble("monthlyBudget", 35000.0)
            )
            prefsRepo.setLanguage(
                if (p.optString("language") == AppLanguage.BN.code) AppLanguage.BN else AppLanguage.EN
            )
            prefsRepo.setDarkTheme(p.optBoolean("isDarkTheme", false))
            prefsRepo.setEnableSubcategories(p.optBoolean("enableSubcategories", true))
            prefsRepo.setHapticFeedback(p.optBoolean("hapticFeedbackEnabled", true))
            val sow = runCatching { StartOfWeek.valueOf(p.optString("startOfWeek")) }
                .getOrDefault(StartOfWeek.SATURDAY)
            prefsRepo.setStartOfWeek(sow)
        }

        return txList.size
    }

    fun generateSampleTemplateCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Type,Category,Subcategory,Account,Amount_BDT,Note")
        sb.appendLine("\"2026-11-01 10:00\",\"Monthly Salary Credit\",INCOME,SALARY_INCOME,\"Monthly Salary\",\"City Bank Salary\",65000.0,\"Payroll deposit\"")
        sb.appendLine("\"2026-11-02 18:30\",\"Shwapno Super Shop\",EXPENSE,GROCERIES,\"Super Shop\",\"bKash Personal\",3420.0,\"Weekly groceries\"")
        sb.appendLine("\"2026-11-05 09:15\",\"DESCO Prepaid Meter\",EXPENSE,RENT_UTILITIES,\"Electricity\",\"Nagad Wallet\",1850.0,\"Electricity top-up\"")
        sb.appendLine("\"2026-11-08 13:20\",\"Star Kabab Lunch\",EXPENSE,FOOD_DINING,\"Restaurant\",\"Cash Wallet\",780.0,\"Lunch with colleagues\"")
        return sb.toString()
    }

    suspend fun clearAllFinancialData(db: AppDatabase) {
        db.withTransaction {
            db.transactionDao().deleteAllTransactions()
            db.accountDao().deleteAllAccounts()
            db.savingsGoalDao().deleteAllGoals()
            db.loanDao().deleteAllLoans()
            db.categoryBudgetDao().deleteAllCategoryBudgets()
        }
    }

    private fun csvEscape(raw: String): String {
        val cleaned = raw.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
