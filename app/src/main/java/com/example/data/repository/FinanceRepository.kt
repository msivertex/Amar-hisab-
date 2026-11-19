package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class FinanceRepository(val db: AppDatabase) {
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val goalDao = db.savingsGoalDao()
    private val loanDao = db.loanDao()
    private val categoryBudgetDao = db.categoryBudgetDao()

    val accounts: Flow<List<AccountEntity>> = accountDao.observeAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
    val goals: Flow<List<SavingsGoalEntity>> = goalDao.observeAllGoals()
    val loans: Flow<List<LoanEntity>> = loanDao.observeAllLoans()
    val categoryBudgets: Flow<List<CategoryBudgetEntity>> = categoryBudgetDao.observeAllCategoryBudgets()

    suspend fun addAccount(account: AccountEntity): Long = accountDao.insertAccount(account)

    suspend fun deleteAccount(accountId: Long) = accountDao.deleteAccountById(accountId)

    suspend fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        subcategory: String = "",
        accountId: Long,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0) return
        db.withTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = title.trim(),
                    amount = amount,
                    type = type,
                    category = category,
                    subcategory = subcategory.trim(),
                    accountId = accountId,
                    note = note.trim(),
                    timestamp = timestamp
                )
            )
            val updatedBalance = when {
                account.type == AccountType.CREDIT_CARD && type == TransactionType.EXPENSE ->
                    account.balance + amount
                account.type == AccountType.CREDIT_CARD && type == TransactionType.INCOME ->
                    (account.balance - amount).coerceAtLeast(0.0)
                type == TransactionType.EXPENSE -> account.balance - amount
                type == TransactionType.INCOME -> account.balance + amount
                else -> account.balance
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))
        }
    }

    suspend fun transferFunds(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        if (amount <= 0.0 || fromAccountId == toAccountId) return false
        return db.withTransaction {
            val fromAcc = accountDao.getAccountById(fromAccountId) ?: return@withTransaction false
            val toAcc = accountDao.getAccountById(toAccountId) ?: return@withTransaction false

            val updatedFromBalance = if (fromAcc.type == AccountType.CREDIT_CARD) {
                fromAcc.balance + amount
            } else {
                fromAcc.balance - amount
            }

            val updatedToBalance = if (toAcc.type == AccountType.CREDIT_CARD) {
                (toAcc.balance - amount).coerceAtLeast(0.0)
            } else {
                toAcc.balance + amount
            }

            accountDao.updateAccount(fromAcc.copy(balance = updatedFromBalance))
            accountDao.updateAccount(toAcc.copy(balance = updatedToBalance))

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "${fromAcc.name} → ${toAcc.name}",
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    category = ExpenseCategory.TRANSFER,
                    subcategory = "Wallet Transfer",
                    accountId = fromAccountId,
                    toAccountId = toAccountId,
                    note = note.ifBlank { "Internal wallet transfer" },
                    timestamp = timestamp
                )
            )
            true
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        db.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
            val source = accountDao.getAccountById(tx.accountId)
            if (source != null) {
                val restored = when (tx.type) {
                    TransactionType.EXPENSE ->
                        if (source.type == AccountType.CREDIT_CARD) (source.balance - tx.amount).coerceAtLeast(0.0)
                        else source.balance + tx.amount
                    TransactionType.INCOME ->
                        if (source.type == AccountType.CREDIT_CARD) source.balance + tx.amount
                        else source.balance - tx.amount
                    TransactionType.TRANSFER ->
                        if (source.type == AccountType.CREDIT_CARD) (source.balance - tx.amount).coerceAtLeast(0.0)
                        else source.balance + tx.amount
                }
                accountDao.updateAccount(source.copy(balance = restored))
            }
            if (tx.type == TransactionType.TRANSFER && tx.toAccountId != null) {
                val dest = accountDao.getAccountById(tx.toAccountId)
                if (dest != null) {
                    val restoredDest = if (dest.type == AccountType.CREDIT_CARD) {
                        dest.balance + tx.amount
                    } else {
                        dest.balance - tx.amount
                    }
                    accountDao.updateAccount(dest.copy(balance = restoredDest))
                }
            }
            transactionDao.deleteTransactionById(transactionId)
        }
    }

    suspend fun addSavingsGoal(
        name: String,
        targetAmount: Double,
        dueDateMillis: Long?,
        colorArgb: Long
    ) {
        if (name.isBlank() || targetAmount <= 0.0) return
        goalDao.insertGoal(
            SavingsGoalEntity(
                name = name.trim(),
                targetAmount = targetAmount,
                savedAmount = 0.0,
                dueDateMillis = dueDateMillis,
                colorArgb = colorArgb
            )
        )
    }

    suspend fun fundGoalFromWallet(goal: SavingsGoalEntity, amount: Double, walletId: Long) {
        if (amount <= 0.0) return
        db.withTransaction {
            val account = accountDao.getAccountById(walletId) ?: return@withTransaction
            val newSaved = (goal.savedAmount + amount).coerceAtMost(goal.targetAmount)
            goalDao.updateGoal(goal.copy(savedAmount = newSaved))

            val updatedBalance = if (account.type == AccountType.CREDIT_CARD) {
                account.balance + amount
            } else {
                account.balance - amount
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Goal: ${goal.name}",
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = ExpenseCategory.OTHER,
                    subcategory = "Savings Contribution",
                    accountId = walletId,
                    note = "Funded savings goal from ${account.name}"
                )
            )
        }
    }

    suspend fun deleteGoal(goalId: Long) = goalDao.deleteGoalById(goalId)

    suspend fun addLoan(
        personName: String,
        loanType: LoanType,
        totalAmount: Double,
        note: String,
        dueDateMillis: Long?
    ) {
        if (personName.isBlank() || totalAmount <= 0.0) return
        loanDao.insertLoan(
            LoanEntity(
                personName = personName.trim(),
                loanType = loanType,
                totalAmount = totalAmount,
                paidAmount = 0.0,
                note = note.trim(),
                dueDateMillis = dueDateMillis
            )
        )
    }

    suspend fun settleLoan(
        loan: LoanEntity,
        settlementAmount: Double,
        walletId: Long
    ) {
        if (settlementAmount <= 0.0) return
        val effectiveAmount = settlementAmount.coerceAtMost(loan.remainingAmount)
        if (effectiveAmount <= 0.0) return

        db.withTransaction {
            val account = accountDao.getAccountById(walletId) ?: return@withTransaction
            val newPaid = (loan.paidAmount + effectiveAmount).coerceAtMost(loan.totalAmount)
            loanDao.updateLoan(loan.copy(paidAmount = newPaid))

            // Money I Owe -> settling deducts from wallet (EXPENSE); Money Owed to Me -> adds to wallet (INCOME)
            val txType = if (loan.loanType == LoanType.I_OWE) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }
            val updatedBalance = when {
                account.type == AccountType.CREDIT_CARD && txType == TransactionType.EXPENSE ->
                    account.balance + effectiveAmount
                account.type == AccountType.CREDIT_CARD && txType == TransactionType.INCOME ->
                    (account.balance - effectiveAmount).coerceAtLeast(0.0)
                txType == TransactionType.EXPENSE -> account.balance - effectiveAmount
                else -> account.balance + effectiveAmount
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))

            val labelPrefix = if (loan.loanType == LoanType.I_OWE) "Debt Paid:" else "Loan Collected:"
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "$labelPrefix ${loan.personName}",
                    amount = effectiveAmount,
                    type = txType,
                    category = ExpenseCategory.OTHER,
                    subcategory = "Debt Repayment",
                    accountId = walletId,
                    note = loan.note.ifBlank { "Loan settlement" }
                )
            )
        }
    }

    suspend fun deleteLoan(loanId: Long) = loanDao.deleteLoanById(loanId)

    suspend fun upsertCategoryBudget(category: ExpenseCategory, limitAmount: Double) {
        if (limitAmount <= 0.0) return
        categoryBudgetDao.upsertCategoryBudget(
            CategoryBudgetEntity(
                categoryName = category.name,
                limitAmount = limitAmount
            )
        )
    }
}
