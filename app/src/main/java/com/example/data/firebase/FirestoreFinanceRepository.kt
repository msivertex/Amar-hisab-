package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.room.withTransaction
import com.example.R
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
import com.example.data.model.TransactionType
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "FirestoreFinanceRepo"

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val userCountryCode: String = "BD",
    val mainCurrencyCode: String = "BDT",
    val secondaryCurrencyCode: String = "",
    val monthlyBudget: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreAccountRecord(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val localId: Long = 0L,
    val name: String = "",
    val type: String = AccountType.CASH.name,
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val bankOrProvider: String = "",
    val accountSubtype: String = "",
    val cardNetwork: String = "",
    val accountNumberLast4: String = "",
    val colorArgb: Long = 0xFF0284C7L,
    val currencyCode: String = "BDT",
    val localCreatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreTransactionRecord(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val localId: Long = 0L,
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = ExpenseCategory.OTHER.name,
    val subcategory: String = "",
    val accountLocalId: Long = 0L,
    val toAccountLocalId: Long? = null,
    val note: String = "",
    val currencyCode: String = "BDT",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreSavingsGoalRecord(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val localId: Long = 0L,
    val name: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val dueDateMillis: Long? = null,
    val colorArgb: Long = 0xFF10B981L,
    val localCreatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreLoanRecord(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val localId: Long = 0L,
    val personName: String = "",
    val loanType: String = LoanType.I_OWE.name,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val note: String = "",
    val dueDateMillis: Long? = null,
    val currencyCode: String = "BDT",
    val localCreatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreCategoryBudgetRecord(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val userId: String = "",
    val categoryName: String = "",
    val limitAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Pure payload mappers matching `firestore.rules` 1:1 (no `"id"` key, null optional fields omitted,
 * strict string bounds enforced, and `FieldValue.serverTimestamp()` used for timestamps).
 */
object FirestorePayloadMapper {
    fun accountDocId(localId: Long): String = "acc_${localId.coerceAtLeast(1L)}"
    fun transactionDocId(localId: Long): String = "tx_${localId.coerceAtLeast(1L)}"
    fun goalDocId(localId: Long): String = "goal_${localId.coerceAtLeast(1L)}"
    fun loanDocId(localId: Long): String = "loan_${localId.coerceAtLeast(1L)}"
    fun budgetDocId(categoryName: String): String {
        val clean = categoryName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(60).ifBlank { "OTHER" }
        return "budget_$clean"
    }

    fun profileCreateMap(uid: String, prefs: UserPrefs): Map<String, Any> = mapOf(
        "userId" to uid,
        "displayName" to prefs.userName.trim().take(120),
        "userCountryCode" to prefs.userCountryCode.trim().ifBlank { "BD" }.take(8),
        "mainCurrencyCode" to prefs.mainCurrencyCode.trim().ifBlank { "BDT" }.take(8),
        "secondaryCurrencyCode" to prefs.secondaryCurrencyCode.trim().take(8),
        "monthlyBudget" to prefs.monthlyBudget.coerceAtLeast(0.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun profileUpdateMap(prefs: UserPrefs): Map<String, Any> = mapOf(
        "displayName" to prefs.userName.trim().take(120),
        "userCountryCode" to prefs.userCountryCode.trim().ifBlank { "BD" }.take(8),
        "mainCurrencyCode" to prefs.mainCurrencyCode.trim().ifBlank { "BDT" }.take(8),
        "secondaryCurrencyCode" to prefs.secondaryCurrencyCode.trim().take(8),
        "monthlyBudget" to prefs.monthlyBudget.coerceAtLeast(0.0),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun accountCreateMap(uid: String, entity: AccountEntity): Map<String, Any> = mapOf(
        "userId" to uid,
        "localId" to entity.id.coerceAtLeast(1L),
        "name" to entity.name.trim().ifBlank { "Wallet" }.take(120),
        "type" to entity.type.name,
        "balance" to entity.balance,
        "creditLimit" to entity.creditLimit.coerceAtLeast(0.0),
        "bankOrProvider" to entity.bankOrProvider.trim().take(120),
        "accountSubtype" to entity.accountSubtype.trim().take(80),
        "cardNetwork" to entity.cardNetwork.trim().take(60),
        "accountNumberLast4" to entity.accountNumberLast4.trim().take(8),
        "colorArgb" to entity.colorArgb,
        "currencyCode" to entity.currencyCode.trim().ifBlank { "BDT" }.take(8),
        "localCreatedAtMillis" to entity.createdAt.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun accountUpdateMap(entity: AccountEntity): Map<String, Any> = mapOf(
        "localId" to entity.id.coerceAtLeast(1L),
        "name" to entity.name.trim().ifBlank { "Wallet" }.take(120),
        "type" to entity.type.name,
        "balance" to entity.balance,
        "creditLimit" to entity.creditLimit.coerceAtLeast(0.0),
        "bankOrProvider" to entity.bankOrProvider.trim().take(120),
        "accountSubtype" to entity.accountSubtype.trim().take(80),
        "cardNetwork" to entity.cardNetwork.trim().take(60),
        "accountNumberLast4" to entity.accountNumberLast4.trim().take(8),
        "colorArgb" to entity.colorArgb,
        "currencyCode" to entity.currencyCode.trim().ifBlank { "BDT" }.take(8),
        "localCreatedAtMillis" to entity.createdAt.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun transactionCreateMap(uid: String, entity: TransactionEntity): Map<String, Any> = mapOf<String, Any?>(
        "userId" to uid,
        "localId" to entity.id.coerceAtLeast(1L),
        "title" to entity.title.trim().ifBlank { "Transaction" }.take(160),
        "amount" to entity.amount.coerceAtLeast(0.01),
        "type" to entity.type.name,
        "category" to entity.category.name.take(60),
        "subcategory" to entity.subcategory.trim().take(80),
        "accountLocalId" to entity.accountId.coerceAtLeast(1L),
        "toAccountLocalId" to entity.toAccountId?.takeIf { it >= 1L },
        "note" to entity.note.trim().take(500),
        "currencyCode" to entity.currencyCode.trim().ifBlank { "BDT" }.take(8),
        "timestampMillis" to entity.timestamp.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun transactionUpdateMap(entity: TransactionEntity): Map<String, Any> = mapOf<String, Any?>(
        "localId" to entity.id.coerceAtLeast(1L),
        "title" to entity.title.trim().ifBlank { "Transaction" }.take(160),
        "amount" to entity.amount.coerceAtLeast(0.01),
        "type" to entity.type.name,
        "category" to entity.category.name.take(60),
        "subcategory" to entity.subcategory.trim().take(80),
        "accountLocalId" to entity.accountId.coerceAtLeast(1L),
        "toAccountLocalId" to entity.toAccountId?.takeIf { it >= 1L },
        "note" to entity.note.trim().take(500),
        "currencyCode" to entity.currencyCode.trim().ifBlank { "BDT" }.take(8),
        "timestampMillis" to entity.timestamp.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun goalCreateMap(uid: String, entity: SavingsGoalEntity): Map<String, Any> = mapOf<String, Any?>(
        "userId" to uid,
        "localId" to entity.id.coerceAtLeast(1L),
        "name" to entity.name.trim().ifBlank { "Goal" }.take(120),
        "targetAmount" to entity.targetAmount.coerceAtLeast(0.01),
        "savedAmount" to entity.savedAmount.coerceAtLeast(0.0),
        "dueDateMillis" to entity.dueDateMillis?.takeIf { it >= 0L },
        "colorArgb" to entity.colorArgb,
        "localCreatedAtMillis" to entity.createdAt.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun goalUpdateMap(entity: SavingsGoalEntity): Map<String, Any> = mapOf<String, Any?>(
        "localId" to entity.id.coerceAtLeast(1L),
        "name" to entity.name.trim().ifBlank { "Goal" }.take(120),
        "targetAmount" to entity.targetAmount.coerceAtLeast(0.01),
        "savedAmount" to entity.savedAmount.coerceAtLeast(0.0),
        "dueDateMillis" to entity.dueDateMillis?.takeIf { it >= 0L },
        "colorArgb" to entity.colorArgb,
        "localCreatedAtMillis" to entity.createdAt.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun loanCreateMap(uid: String, entity: LoanEntity): Map<String, Any> = mapOf<String, Any?>(
        "userId" to uid,
        "localId" to entity.id.coerceAtLeast(1L),
        "personName" to entity.personName.trim().ifBlank { "Contact" }.take(120),
        "loanType" to entity.loanType.name,
        "totalAmount" to entity.totalAmount.coerceAtLeast(0.01),
        "paidAmount" to entity.paidAmount.coerceAtLeast(0.0),
        "note" to entity.note.trim().take(500),
        "dueDateMillis" to entity.dueDateMillis?.takeIf { it >= 0L },
        "currencyCode" to entity.currencyCode.trim().ifBlank { "BDT" }.take(8),
        "localCreatedAtMillis" to entity.createdAt.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun loanUpdateMap(entity: LoanEntity): Map<String, Any> = mapOf<String, Any?>(
        "localId" to entity.id.coerceAtLeast(1L),
        "personName" to entity.personName.trim().ifBlank { "Contact" }.take(120),
        "loanType" to entity.loanType.name,
        "totalAmount" to entity.totalAmount.coerceAtLeast(0.01),
        "paidAmount" to entity.paidAmount.coerceAtLeast(0.0),
        "note" to entity.note.trim().take(500),
        "dueDateMillis" to entity.dueDateMillis?.takeIf { it >= 0L },
        "currencyCode" to entity.currencyCode.trim().ifBlank { "BDT" }.take(8),
        "localCreatedAtMillis" to entity.createdAt.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }

    fun budgetCreateMap(uid: String, entity: CategoryBudgetEntity): Map<String, Any> = mapOf(
        "userId" to uid,
        "categoryName" to entity.categoryName.trim().ifBlank { "OTHER" }.take(60),
        "limitAmount" to entity.limitAmount.coerceAtLeast(0.01),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun budgetUpdateMap(entity: CategoryBudgetEntity): Map<String, Any> = mapOf(
        "categoryName" to entity.categoryName.trim().ifBlank { "OTHER" }.take(60),
        "limitAmount" to entity.limitAmount.coerceAtLeast(0.01),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

/**
 * Central Firebase Firestore data store connected to the app's accounts/cards, transactions
 * (including entries created via Voice Entry and Receipt Scan), goals, loans, budgets, and profile.
 *
 * - Strictly connects to the provisioned named database (`R.string.firestore_database_id`).
 * - Always operates asynchronously in the background so local Room storage stays instant and
 *   completely non-blocking offline or in Guest Mode.
 * - Strictly requires a signed-in Google Firebase Auth `uid` (`Firebase.auth.currentUser?.uid`)
 *   and isolates all documents under `/users/{uid}/...`.
 */
class FirestoreFinanceRepository(context: Context) {

    private val appContext = context.applicationContext
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private fun getFirestoreOrNull(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            if (FirebaseApp.getApps(appContext).isEmpty()) return null
            val databaseId = appContext.getString(R.string.firestore_database_id).trim()
            if (databaseId.isBlank()) return null
            FirebaseFirestore.getInstance(databaseId)
        } catch (e: Exception) {
            Log.d(TAG, "Firestore instance unavailable in current environment: ${e.localizedMessage}")
            null
        }
    }

    fun currentAuthenticatedUid(): String? {
        return try {
            if (FirebaseApp.getApps(appContext).isEmpty()) return null
            Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Detaches all active Firestore snapshot listeners cleanly inside `NonCancellable`
     * before signing out of Firebase Auth to prevent teardown PERMISSION_DENIED races.
     */
    suspend fun detachAllListeners() = withContext(NonCancellable) {
        synchronized(activeListeners) {
            activeListeners.forEach { reg -> runCatching { reg.remove() } }
            activeListeners.clear()
        }
    }

    /**
     * Observes the authenticated user's accounts/cards collection in real time.
     * Call ONLY when `uid` is verified from `Firebase.auth.currentUser?.uid`.
     */
    fun observeAccounts(uid: String): Flow<List<FirestoreAccountRecord>> = callbackFlow {
        val db = getFirestoreOrNull()
        if (db == null || uid.isBlank()) {
            close()
            return@callbackFlow
        }
        val path = "users/$uid/accounts"
        val registration = db.collection("users")
            .document(uid)
            .collection("accounts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val wrapped = handleFirestoreError(error, OperationType.LIST, path)
                    close(wrapped)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(
                        FirestoreAccountRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.orEmpty()
                trySend(items)
            }
        synchronized(activeListeners) { activeListeners.add(registration) }
        awaitClose {
            registration.remove()
            synchronized(activeListeners) { activeListeners.remove(registration) }
        }
    }

    /**
     * Pushes the complete current central Room data store (cards/accounts, transactions, goals,
     * loans, category budgets, and profile preferences) to Firestore under `/users/{uid}/...`
     * in a non-blocking background pass.
     */
    suspend fun pushCentralStoreSnapshot(
        roomDb: AppDatabase,
        prefs: UserPrefs
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentAuthenticatedUid() ?: return@withContext Result.success(Unit)
        val fs = getFirestoreOrNull() ?: return@withContext Result.success(Unit)
        val userDoc = fs.collection("users").document(uid)
        val userPath = "users/$uid"

        try {
            // 1. User profile & currency summary
            upsertPreservingCreatedAt(
                docRef = userDoc,
                createMap = FirestorePayloadMapper.profileCreateMap(uid, prefs),
                updateMap = FirestorePayloadMapper.profileUpdateMap(prefs),
                path = userPath
            )

            // 2. Accounts / Cards
            val accounts = roomDb.accountDao().getAllAccountsOnce()
            accounts.forEach { acc ->
                val docId = FirestorePayloadMapper.accountDocId(acc.id)
                upsertPreservingCreatedAt(
                    docRef = userDoc.collection("accounts").document(docId),
                    createMap = FirestorePayloadMapper.accountCreateMap(uid, acc),
                    updateMap = FirestorePayloadMapper.accountUpdateMap(acc),
                    path = "$userPath/accounts/$docId"
                )
            }

            // 3. Transactions (including Voice Entry, Receipt Scan, manual entries & transfers)
            val transactions = roomDb.transactionDao().getAllTransactionsOnce()
            transactions.forEach { tx ->
                val docId = FirestorePayloadMapper.transactionDocId(tx.id)
                upsertPreservingCreatedAt(
                    docRef = userDoc.collection("transactions").document(docId),
                    createMap = FirestorePayloadMapper.transactionCreateMap(uid, tx),
                    updateMap = FirestorePayloadMapper.transactionUpdateMap(tx),
                    path = "$userPath/transactions/$docId"
                )
            }

            // 4. Savings Goals
            val goals = roomDb.savingsGoalDao().getAllGoalsOnce()
            goals.forEach { goal ->
                val docId = FirestorePayloadMapper.goalDocId(goal.id)
                upsertPreservingCreatedAt(
                    docRef = userDoc.collection("goals").document(docId),
                    createMap = FirestorePayloadMapper.goalCreateMap(uid, goal),
                    updateMap = FirestorePayloadMapper.goalUpdateMap(goal),
                    path = "$userPath/goals/$docId"
                )
            }

            // 5. Loans
            val loans = roomDb.loanDao().getAllLoansOnce()
            loans.forEach { loan ->
                val docId = FirestorePayloadMapper.loanDocId(loan.id)
                upsertPreservingCreatedAt(
                    docRef = userDoc.collection("loans").document(docId),
                    createMap = FirestorePayloadMapper.loanCreateMap(uid, loan),
                    updateMap = FirestorePayloadMapper.loanUpdateMap(loan),
                    path = "$userPath/loans/$docId"
                )
            }

            // 6. Category Budgets
            val budgets = roomDb.categoryBudgetDao().getAllCategoryBudgetsOnce()
            budgets.forEach { budget ->
                val docId = FirestorePayloadMapper.budgetDocId(budget.categoryName)
                upsertPreservingCreatedAt(
                    docRef = userDoc.collection("budgets").document(docId),
                    createMap = FirestorePayloadMapper.budgetCreateMap(uid, budget),
                    updateMap = FirestorePayloadMapper.budgetUpdateMap(budget),
                    path = "$userPath/budgets/$docId"
                )
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val wrapped = handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(wrapped)
        }
    }

    suspend fun deleteAccountDoc(localAccountId: Long): Result<Unit> =
        deleteSubcollectionDoc("accounts", FirestorePayloadMapper.accountDocId(localAccountId))

    suspend fun deleteTransactionDoc(localTransactionId: Long): Result<Unit> =
        deleteSubcollectionDoc("transactions", FirestorePayloadMapper.transactionDocId(localTransactionId))

    suspend fun deleteGoalDoc(localGoalId: Long): Result<Unit> =
        deleteSubcollectionDoc("goals", FirestorePayloadMapper.goalDocId(localGoalId))

    suspend fun deleteLoanDoc(localLoanId: Long): Result<Unit> =
        deleteSubcollectionDoc("loans", FirestorePayloadMapper.loanDocId(localLoanId))

    suspend fun clearAllUserSubcollections(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentAuthenticatedUid() ?: return@withContext Result.success(Unit)
        val fs = getFirestoreOrNull() ?: return@withContext Result.success(Unit)
        val userDoc = fs.collection("users").document(uid)
        val subcollections = listOf("accounts", "transactions", "goals", "loans", "budgets")
        try {
            subcollections.forEach { sub ->
                val snap = userDoc.collection(sub).get().awaitTask()
                snap.documents.forEach { doc ->
                    doc.reference.delete().awaitTask()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val wrapped = handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(wrapped)
        }
    }

    /**
     * Reconciles Firestore and the local Room central data store upon Google Sign-In:
     * - If local Room DB is empty and Firestore has saved cards/entries, hydrates Room from Firestore.
     * - Otherwise pushes the local central data store up to Firestore so cloud & device stay in sync.
     */
    suspend fun reconcileOnSignIn(
        roomDb: AppDatabase,
        prefsRepo: UserPreferencesRepository,
        currentPrefs: UserPrefs
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentAuthenticatedUid() ?: return@withContext Result.success(false)
        val fs = getFirestoreOrNull() ?: return@withContext Result.success(false)
        val userDoc = fs.collection("users").document(uid)
        val userPath = "users/$uid"

        try {
            val localAccounts = roomDb.accountDao().getAllAccountsOnce()
            val localTransactions = roomDb.transactionDao().getAllTransactionsOnce()
            val localGoals = roomDb.savingsGoalDao().getAllGoalsOnce()
            val localLoans = roomDb.loanDao().getAllLoansOnce()
            val localHasData = localAccounts.isNotEmpty() ||
                localTransactions.isNotEmpty() ||
                localGoals.isNotEmpty() ||
                localLoans.isNotEmpty()

            if (!localHasData) {
                val cloudAccounts = userDoc.collection("accounts").get().awaitTask().documents.mapNotNull {
                    it.toObject(FirestoreAccountRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val cloudTransactions = userDoc.collection("transactions").get().awaitTask().documents.mapNotNull {
                    it.toObject(FirestoreTransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val cloudGoals = userDoc.collection("goals").get().awaitTask().documents.mapNotNull {
                    it.toObject(FirestoreSavingsGoalRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val cloudLoans = userDoc.collection("loans").get().awaitTask().documents.mapNotNull {
                    it.toObject(FirestoreLoanRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val cloudBudgets = userDoc.collection("budgets").get().awaitTask().documents.mapNotNull {
                    it.toObject(FirestoreCategoryBudgetRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val cloudProfile = userDoc.get().awaitTask().toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )

                val cloudHasData = cloudAccounts.isNotEmpty() ||
                    cloudTransactions.isNotEmpty() ||
                    cloudGoals.isNotEmpty() ||
                    cloudLoans.isNotEmpty() ||
                    cloudBudgets.isNotEmpty()

                if (cloudHasData) {
                    roomDb.withTransaction {
                        if (cloudAccounts.isNotEmpty()) {
                            roomDb.accountDao().insertAccounts(
                                cloudAccounts.map { rec ->
                                    AccountEntity(
                                        id = rec.localId.coerceAtLeast(1L),
                                        name = rec.name,
                                        type = runCatching { AccountType.valueOf(rec.type) }.getOrDefault(AccountType.CASH),
                                        balance = rec.balance,
                                        creditLimit = rec.creditLimit,
                                        bankOrProvider = rec.bankOrProvider,
                                        accountSubtype = rec.accountSubtype,
                                        cardNetwork = rec.cardNetwork,
                                        accountNumberLast4 = rec.accountNumberLast4,
                                        colorArgb = rec.colorArgb,
                                        currencyCode = rec.currencyCode.ifBlank { "BDT" },
                                        createdAt = rec.localCreatedAtMillis.takeIf { it > 0L }
                                            ?: System.currentTimeMillis()
                                    )
                                }
                            )
                        }
                        if (cloudTransactions.isNotEmpty()) {
                            roomDb.transactionDao().insertTransactions(
                                cloudTransactions.map { rec ->
                                    TransactionEntity(
                                        id = rec.localId.coerceAtLeast(1L),
                                        title = rec.title,
                                        amount = rec.amount,
                                        type = runCatching { TransactionType.valueOf(rec.type) }
                                            .getOrDefault(TransactionType.EXPENSE),
                                        category = runCatching { ExpenseCategory.valueOf(rec.category) }
                                            .getOrDefault(ExpenseCategory.OTHER),
                                        subcategory = rec.subcategory,
                                        accountId = rec.accountLocalId.coerceAtLeast(1L),
                                        toAccountId = rec.toAccountLocalId,
                                        note = rec.note,
                                        currencyCode = rec.currencyCode.ifBlank { "BDT" },
                                        timestamp = rec.timestampMillis.takeIf { it > 0L }
                                            ?: System.currentTimeMillis()
                                    )
                                }
                            )
                        }
                        if (cloudGoals.isNotEmpty()) {
                            roomDb.savingsGoalDao().insertGoals(
                                cloudGoals.map { rec ->
                                    SavingsGoalEntity(
                                        id = rec.localId.coerceAtLeast(1L),
                                        name = rec.name,
                                        targetAmount = rec.targetAmount,
                                        savedAmount = rec.savedAmount,
                                        dueDateMillis = rec.dueDateMillis,
                                        colorArgb = rec.colorArgb,
                                        createdAt = rec.localCreatedAtMillis.takeIf { it > 0L }
                                            ?: System.currentTimeMillis()
                                    )
                                }
                            )
                        }
                        if (cloudLoans.isNotEmpty()) {
                            roomDb.loanDao().insertLoans(
                                cloudLoans.map { rec ->
                                    LoanEntity(
                                        id = rec.localId.coerceAtLeast(1L),
                                        personName = rec.personName,
                                        loanType = runCatching { LoanType.valueOf(rec.loanType) }
                                            .getOrDefault(LoanType.I_OWE),
                                        totalAmount = rec.totalAmount,
                                        paidAmount = rec.paidAmount,
                                        note = rec.note,
                                        dueDateMillis = rec.dueDateMillis,
                                        createdAt = rec.localCreatedAtMillis.takeIf { it > 0L }
                                            ?: System.currentTimeMillis(),
                                        currencyCode = rec.currencyCode.ifBlank { "BDT" }
                                    )
                                }
                            )
                        }
                        if (cloudBudgets.isNotEmpty()) {
                            roomDb.categoryBudgetDao().insertCategoryBudgets(
                                cloudBudgets.map { rec ->
                                    CategoryBudgetEntity(
                                        categoryName = rec.categoryName,
                                        limitAmount = rec.limitAmount
                                    )
                                }
                            )
                        }
                    }

                    if (cloudProfile != null) {
                        if (cloudProfile.userCountryCode.isNotBlank()) {
                            prefsRepo.setUserCountryCode(cloudProfile.userCountryCode)
                        }
                        if (cloudProfile.mainCurrencyCode.isNotBlank()) {
                            prefsRepo.setMainCurrency(cloudProfile.mainCurrencyCode)
                        }
                        prefsRepo.setSecondaryCurrency(cloudProfile.secondaryCurrencyCode)
                        if (cloudProfile.monthlyBudget > 0.0) {
                            prefsRepo.updateProfileAndBudget(
                                cloudProfile.displayName.ifBlank { currentPrefs.userName },
                                cloudProfile.monthlyBudget
                            )
                        }
                    }
                    return@withContext Result.success(true)
                }
            }

            // Local has records (or cloud was empty): push central data store up cleanly
            pushCentralStoreSnapshot(roomDb, currentPrefs).map { false }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val wrapped = handleFirestoreError(e, OperationType.GET, userPath)
            Result.failure(wrapped)
        }
    }

    private suspend fun deleteSubcollectionDoc(subcollection: String, docId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentAuthenticatedUid() ?: return@withContext Result.success(Unit)
            val fs = getFirestoreOrNull() ?: return@withContext Result.success(Unit)
            val path = "users/$uid/$subcollection/$docId"
            try {
                fs.collection("users").document(uid).collection(subcollection).document(docId).delete().awaitTask()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val wrapped = handleFirestoreError(e, OperationType.DELETE, path)
                Result.failure(wrapped)
            }
        }

    private suspend fun upsertPreservingCreatedAt(
        docRef: DocumentReference,
        createMap: Map<String, Any>,
        updateMap: Map<String, Any>,
        path: String
    ) {
        try {
            val existing = docRef.get().awaitTask()
            if (existing.exists()) {
                docRef.update(updateMap).awaitTask()
            } else {
                docRef.set(createMap).awaitTask()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exc ->
            if (cont.isActive) cont.resumeWithException(exc)
        }
        addOnCanceledListener {
            cont.cancel()
        }
    }
}
