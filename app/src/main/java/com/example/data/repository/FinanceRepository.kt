package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryBudgetEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AccountBalanceGuard
import com.example.data.model.AccountNameResolver
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import com.example.data.model.TransferNoteCodec
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

    suspend fun addAccount(account: AccountEntity): Long {
        val normalized = AccountNameResolver.normalizeAccountForSave(account)
        return accountDao.insertAccount(
            normalized.copy(balance = AccountBalanceGuard.clampNonNegative(normalized.balance))
        )
    }

    suspend fun updateAccount(account: AccountEntity) {
        val normalized = AccountNameResolver.normalizeAccountForSave(account)
        accountDao.updateAccount(
            normalized.copy(balance = AccountBalanceGuard.clampNonNegative(normalized.balance))
        )
    }

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
    ): Boolean {
        if (amount <= 0.0 || !amount.isFinite()) return false
        return db.withTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction false
            if (type == TransactionType.EXPENSE &&
                AccountBalanceGuard.isInsufficientForDeduction(account, amount)
            ) {
                return@withTransaction false
            }
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = title.trim(),
                    amount = amount,
                    type = type,
                    category = category,
                    subcategory = subcategory.trim(),
                    accountId = accountId,
                    note = note.trim(),
                    timestamp = timestamp,
                    currencyCode = account.currencyCode
                )
            )
            val updatedBalance = when {
                account.type == AccountType.CREDIT_CARD && type == TransactionType.EXPENSE ->
                    AccountBalanceGuard.clampNonNegative(account.balance + amount)
                account.type == AccountType.CREDIT_CARD && type == TransactionType.INCOME ->
                    AccountBalanceGuard.deductOrZero(account.balance, amount)
                type == TransactionType.EXPENSE ->
                    AccountBalanceGuard.deductOrZero(account.balance, amount)
                type == TransactionType.INCOME ->
                    AccountBalanceGuard.clampNonNegative(AccountBalanceGuard.clampNonNegative(account.balance) + amount)
                else -> AccountBalanceGuard.clampNonNegative(account.balance)
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))
            true
        }
    }

    suspend fun transferFunds(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        conversionRate: Double = 1.0
    ): Boolean {
        if (amount <= 0.0 || !amount.isFinite() || fromAccountId == toAccountId) return false
        return db.withTransaction {
            val fromAcc = accountDao.getAccountById(fromAccountId) ?: return@withTransaction false
            val toAcc = accountDao.getAccountById(toAccountId) ?: return@withTransaction false
            if (AccountBalanceGuard.isInsufficientForDeduction(fromAcc, amount)) {
                return@withTransaction false
            }
            val effectiveRate = if (fromAcc.currencyCode.equals(toAcc.currencyCode, ignoreCase = true)) {
                1.0
            } else {
                conversionRate.takeIf { it > 0.0 } ?: 1.0
            }
            val creditedAmount = amount * effectiveRate

            val updatedFromBalance = if (fromAcc.type == AccountType.CREDIT_CARD) {
                AccountBalanceGuard.clampNonNegative(fromAcc.balance + amount)
            } else {
                AccountBalanceGuard.deductOrZero(fromAcc.balance, amount)
            }

            val updatedToBalance = if (toAcc.type == AccountType.CREDIT_CARD) {
                AccountBalanceGuard.deductOrZero(toAcc.balance, creditedAmount)
            } else {
                AccountBalanceGuard.clampNonNegative(AccountBalanceGuard.clampNonNegative(toAcc.balance) + creditedAmount)
            }

            accountDao.updateAccount(fromAcc.copy(balance = updatedFromBalance))
            accountDao.updateAccount(toAcc.copy(balance = updatedToBalance))

            val allAccounts = accountDao.getAllAccountsOnce()
            val routeTitle = AccountNameResolver.formatTransferRouteTitle(
                fromAccount = fromAcc,
                toAccount = toAcc,
                allAccounts = allAccounts
            )
            val transferNote = TransferNoteCodec.encode(
                userNote = note,
                sourceCurrencyCode = fromAcc.currencyCode,
                destCurrencyCode = toAcc.currencyCode,
                exchangeRate = effectiveRate,
                receivedAmount = creditedAmount
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    title = routeTitle,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    category = ExpenseCategory.TRANSFER,
                    subcategory = "Wallet Transfer",
                    accountId = fromAccountId,
                    toAccountId = toAccountId,
                    note = transferNote,
                    timestamp = timestamp,
                    currencyCode = fromAcc.currencyCode
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
                        if (source.type == AccountType.CREDIT_CARD) AccountBalanceGuard.deductOrZero(source.balance, tx.amount)
                        else AccountBalanceGuard.clampNonNegative(AccountBalanceGuard.clampNonNegative(source.balance) + tx.amount)
                    TransactionType.INCOME ->
                        if (source.type == AccountType.CREDIT_CARD) AccountBalanceGuard.clampNonNegative(source.balance + tx.amount)
                        else AccountBalanceGuard.deductOrZero(source.balance, tx.amount)
                    TransactionType.TRANSFER ->
                        if (source.type == AccountType.CREDIT_CARD) AccountBalanceGuard.deductOrZero(source.balance, tx.amount)
                        else AccountBalanceGuard.clampNonNegative(AccountBalanceGuard.clampNonNegative(source.balance) + tx.amount)
                }
                accountDao.updateAccount(source.copy(balance = restored))
            }
            if (tx.type == TransactionType.TRANSFER && tx.toAccountId != null) {
                val dest = accountDao.getAccountById(tx.toAccountId)
                if (dest != null) {
                    val parsedTransfer = TransferNoteCodec.parse(tx, dest)
                    val destReversalAmount = if (parsedTransfer != null && parsedTransfer.isCrossCurrency && parsedTransfer.isRecordedRate) {
                        parsedTransfer.receivedAmount
                    } else {
                        tx.amount
                    }
                    val restoredDest = if (dest.type == AccountType.CREDIT_CARD) {
                        AccountBalanceGuard.clampNonNegative(dest.balance + destReversalAmount)
                    } else {
                        AccountBalanceGuard.deductOrZero(dest.balance, destReversalAmount)
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

    suspend fun fundGoalFromWallet(goal: SavingsGoalEntity, amount: Double, walletId: Long): Boolean {
        if (amount <= 0.0 || !amount.isFinite()) return false
        return db.withTransaction {
            val account = accountDao.getAccountById(walletId) ?: return@withTransaction false
            if (AccountBalanceGuard.isInsufficientForDeduction(account, amount)) {
                return@withTransaction false
            }
            val newSaved = (goal.savedAmount + amount).coerceAtMost(goal.targetAmount)
            goalDao.updateGoal(goal.copy(savedAmount = newSaved))

            val updatedBalance = if (account.type == AccountType.CREDIT_CARD) {
                AccountBalanceGuard.clampNonNegative(account.balance + amount)
            } else {
                AccountBalanceGuard.deductOrZero(account.balance, amount)
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
                    note = "Funded savings goal from ${account.name}",
                    currencyCode = account.currencyCode
                )
            )
            true
        }
    }

    suspend fun deleteGoal(goalId: Long) = goalDao.deleteGoalById(goalId)

    suspend fun addLoan(
        personName: String,
        loanType: LoanType,
        totalAmount: Double,
        note: String,
        dueDateMillis: Long?,
        currencyCode: String = "BDT"
    ) {
        if (personName.isBlank() || totalAmount <= 0.0) return
        val normalizedCurrency = com.example.data.model.AppCurrency.fromCode(currencyCode).code
        loanDao.insertLoan(
            LoanEntity(
                personName = personName.trim(),
                loanType = loanType,
                totalAmount = totalAmount,
                paidAmount = 0.0,
                note = note.trim(),
                dueDateMillis = dueDateMillis,
                currencyCode = normalizedCurrency
            )
        )
    }

    suspend fun updateLoan(loan: LoanEntity) {
        if (loan.personName.isBlank() || loan.totalAmount <= 0.0) return
        val normalizedCurrency = com.example.data.model.AppCurrency.fromCode(loan.currencyCode).code
        val clampedPaid = loan.paidAmount.coerceIn(0.0, loan.totalAmount)
        loanDao.updateLoan(
            loan.copy(
                personName = loan.personName.trim(),
                note = loan.note.trim(),
                paidAmount = clampedPaid,
                currencyCode = normalizedCurrency
            )
        )
    }

    suspend fun settleLoan(
        loan: LoanEntity,
        settlementAmount: Double,
        walletId: Long?,
        conversionRate: Double = 1.0
    ): Boolean {
        if (settlementAmount <= 0.0 || !settlementAmount.isFinite()) return false
        val effectiveAmount = settlementAmount.coerceAtMost(loan.remainingAmount)
        if (effectiveAmount <= 0.0) return false

        return db.withTransaction {
            val targetWalletId = walletId?.takeIf { it > 0L }
            val account = targetWalletId?.let { accountDao.getAccountById(it) }
            val sameCurrency = account == null || account.currencyCode.equals(loan.currencyCode, ignoreCase = true)
            val effectiveRate = if (sameCurrency) 1.0 else conversionRate.takeIf { it > 0.0 } ?: 1.0
            val walletImpactAmount = effectiveAmount * effectiveRate

            val txType = if (loan.loanType == LoanType.I_OWE) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }
            if (account != null && txType == TransactionType.EXPENSE &&
                AccountBalanceGuard.isInsufficientForDeduction(account, walletImpactAmount)
            ) {
                return@withTransaction false
            }

            val newPaid = (loan.paidAmount + effectiveAmount).coerceAtMost(loan.totalAmount)
            loanDao.updateLoan(loan.copy(paidAmount = newPaid))

            // Allow recording partial/full settlement even without linking a wallet
            if (targetWalletId == null || account == null) return@withTransaction true

            val updatedBalance = when {
                account.type == AccountType.CREDIT_CARD && txType == TransactionType.EXPENSE ->
                    AccountBalanceGuard.clampNonNegative(account.balance + walletImpactAmount)
                account.type == AccountType.CREDIT_CARD && txType == TransactionType.INCOME ->
                    AccountBalanceGuard.deductOrZero(account.balance, walletImpactAmount)
                txType == TransactionType.EXPENSE ->
                    AccountBalanceGuard.deductOrZero(account.balance, walletImpactAmount)
                else ->
                    AccountBalanceGuard.clampNonNegative(AccountBalanceGuard.clampNonNegative(account.balance) + walletImpactAmount)
            }
            accountDao.updateAccount(account.copy(balance = updatedBalance))

            val labelPrefix = if (loan.loanType == LoanType.I_OWE) "Debt Paid:" else "Loan Collected:"
            val settlementNote = buildString {
                append(loan.note.ifBlank { "Loan settlement" })
                if (!sameCurrency) {
                    append(" (${loan.currencyCode} → ${account.currencyCode})")
                }
            }
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "$labelPrefix ${loan.personName}",
                    amount = walletImpactAmount,
                    type = txType,
                    category = ExpenseCategory.OTHER,
                    subcategory = "Debt Repayment",
                    accountId = targetWalletId,
                    note = settlementNote,
                    currencyCode = account.currencyCode
                )
            )
            true
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
