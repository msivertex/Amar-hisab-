package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.data.model.TransactionType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val creditLimit: Double = 0.0,
    val bankOrProvider: String,
    val accountSubtype: String = "",
    val cardNetwork: String = "",
    val accountNumberLast4: String = "",
    val colorArgb: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    val utilizationPercent: Int
        get() = if (type == AccountType.CREDIT_CARD && creditLimit > 0.0) {
            ((balance / creditLimit) * 100.0).toInt().coerceIn(0, 100)
        } else 0
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: ExpenseCategory,
    val subcategory: String = "",
    val accountId: Long,
    val toAccountId: Long? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val dueDateMillis: Long? = null,
    val colorArgb: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personName: String,
    val loanType: LoanType,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val note: String = "",
    val dueDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
    val isSettled: Boolean get() = remainingAmount <= 0.01
}

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey val categoryName: String,
    val limitAmount: Double
)
