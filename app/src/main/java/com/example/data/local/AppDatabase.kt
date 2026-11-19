package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.LoanType
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        SavingsGoalEntity::class,
        LoanEntity::class,
        CategoryBudgetEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun loanDao(): LoanDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amar_hisab_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(SeedDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialData(database)
                }
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            val accountDao = db.accountDao()
            if (accountDao.getAccountCount() > 0) return

            val now = System.currentTimeMillis()
            val cashId = accountDao.insertAccount(
                AccountEntity(
                    name = "Cash Wallet",
                    type = AccountType.CASH,
                    balance = 8450.0,
                    bankOrProvider = "Cash",
                    accountNumberLast4 = "BDT",
                    colorArgb = 0xFF0D9488L,
                    createdAt = now - 5000
                )
            )
            val bkashId = accountDao.insertAccount(
                AccountEntity(
                    name = "bKash Personal",
                    type = AccountType.MFS,
                    balance = 14250.0,
                    bankOrProvider = "bKash",
                    accountNumberLast4 = "01711-8429",
                    colorArgb = 0xFFE2136EL,
                    createdAt = now - 4000
                )
            )
            val nagadId = accountDao.insertAccount(
                AccountEntity(
                    name = "Nagad Wallet",
                    type = AccountType.MFS,
                    balance = 6800.0,
                    bankOrProvider = "Nagad",
                    accountNumberLast4 = "01819-3310",
                    colorArgb = 0xFFEA580CL,
                    createdAt = now - 3000
                )
            )
            val bankId = accountDao.insertAccount(
                AccountEntity(
                    name = "City Bank Salary",
                    type = AccountType.DEBIT_BANK,
                    balance = 74500.0,
                    bankOrProvider = "City Bank",
                    accountSubtype = "Salary Account",
                    cardNetwork = "Visa",
                    accountNumberLast4 = "4829",
                    colorArgb = 0xFFC8102EL,
                    createdAt = now - 2000
                )
            )
            val creditId = accountDao.insertAccount(
                AccountEntity(
                    name = "BRAC Platinum Card",
                    type = AccountType.CREDIT_CARD,
                    balance = 16500.0,
                    creditLimit = 100000.0,
                    bankOrProvider = "BRAC Bank",
                    cardNetwork = "Mastercard",
                    accountNumberLast4 = "9042",
                    colorArgb = 0xFF0072BCL,
                    createdAt = now - 1000
                )
            )

            fun daysAgo(days: Int, hour: Int = 14): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_MONTH, -days)
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, 15)
                return cal.timeInMillis
            }

            fun daysAhead(days: Int): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_MONTH, days)
                return cal.timeInMillis
            }

            db.transactionDao().insertTransactions(
                listOf(
                    TransactionEntity(
                        title = "Monthly Salary Credit",
                        amount = 65000.0,
                        type = TransactionType.INCOME,
                        category = ExpenseCategory.SALARY_INCOME,
                        subcategory = "Monthly Salary",
                        accountId = bankId,
                        note = "Payroll deposit",
                        timestamp = daysAgo(12, 10)
                    ),
                    TransactionEntity(
                        title = "House Rent & Service Charge",
                        amount = 14500.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.RENT_UTILITIES,
                        subcategory = "House Rent",
                        accountId = bankId,
                        note = "Dhanmondi Flat 4B",
                        timestamp = daysAgo(10, 11)
                    ),
                    TransactionEntity(
                        title = "Shwapno Super Shop",
                        amount = 3420.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.GROCERIES,
                        subcategory = "Super Shop",
                        accountId = bkashId,
                        note = "Weekly groceries & produce",
                        timestamp = daysAgo(7, 18)
                    ),
                    TransactionEntity(
                        title = "DESCO Prepaid Meter",
                        amount = 1850.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.RENT_UTILITIES,
                        subcategory = "Electricity",
                        accountId = nagadId,
                        note = "Electricity top-up",
                        timestamp = daysAgo(5, 9)
                    ),
                    TransactionEntity(
                        title = "Star Kabab & Restaurant",
                        amount = 1280.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.FOOD_DINING,
                        subcategory = "Restaurant",
                        accountId = creditId,
                        note = "Team lunch",
                        timestamp = daysAgo(3, 13)
                    ),
                    TransactionEntity(
                        title = "Pathao & Metro Rail",
                        amount = 520.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.TRANSPORT,
                        subcategory = "Metro & Bus",
                        accountId = cashId,
                        note = "Office commute",
                        timestamp = daysAgo(2, 17)
                    ),
                    TransactionEntity(
                        title = "Chaldal Fresh Delivery",
                        amount = 1490.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.GROCERIES,
                        subcategory = "Kacha Bazar",
                        accountId = bkashId,
                        note = "Rice, oil, vegetables",
                        timestamp = daysAgo(1, 19)
                    ),
                    TransactionEntity(
                        title = "North End Coffee",
                        amount = 420.0,
                        type = TransactionType.EXPENSE,
                        category = ExpenseCategory.FOOD_DINING,
                        subcategory = "Coffee & Tea",
                        accountId = cashId,
                        note = "Morning flat white",
                        timestamp = daysAgo(0, 9)
                    )
                )
            )

            db.savingsGoalDao().insertGoals(
                listOf(
                    SavingsGoalEntity(
                        name = "Emergency Reserve Fund",
                        targetAmount = 150000.0,
                        savedAmount = 82000.0,
                        dueDateMillis = daysAhead(120),
                        colorArgb = 0xFF0284C7L
                    ),
                    SavingsGoalEntity(
                        name = "Cox's Bazar Retreat",
                        targetAmount = 35000.0,
                        savedAmount = 19500.0,
                        dueDateMillis = daysAhead(45),
                        colorArgb = 0xFF10B981L
                    )
                )
            )

            db.loanDao().insertLoans(
                listOf(
                    LoanEntity(
                        personName = "Rafiul Islam",
                        loanType = LoanType.OWED_TO_ME,
                        totalAmount = 5000.0,
                        paidAmount = 1500.0,
                        note = "Shared dinner & concert tickets",
                        dueDateMillis = daysAhead(10)
                    ),
                    LoanEntity(
                        personName = "Sadia Rahman",
                        loanType = LoanType.I_OWE,
                        totalAmount = 2500.0,
                        paidAmount = 0.0,
                        note = "Emergency medicine split",
                        dueDateMillis = daysAhead(7)
                    )
                )
            )

            db.categoryBudgetDao().insertCategoryBudgets(
                listOf(
                    CategoryBudgetEntity(ExpenseCategory.FOOD_DINING.name, 6500.0),
                    CategoryBudgetEntity(ExpenseCategory.GROCERIES.name, 9000.0),
                    CategoryBudgetEntity(ExpenseCategory.RENT_UTILITIES.name, 18000.0),
                    CategoryBudgetEntity(ExpenseCategory.TRANSPORT.name, 3500.0)
                )
            )
        }
    }
}
