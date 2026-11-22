package com.example.notifications

import org.json.JSONArray
import org.json.JSONObject

/**
 * Deep-link routes triggered when tapping either a system status-bar notification
 * or an in-app Notification Center card.
 */
enum class NotificationActionRoute {
    QUICK_ADD_TX,
    OPEN_PLAN_LOANS,
    OPEN_INSIGHTS_BUDGET,
    OPEN_ACCOUNTS,
    OPEN_HOME;

    companion object {
        fun fromWireName(raw: String?): NotificationActionRoute? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

enum class NotificationCategory {
    DAILY_REMINDER,
    DUE_LOAN,
    BUDGET_THRESHOLD,
    HIGH_SPEND_CARD
}

/**
 * Persisted in-app notification history item created whenever a scheduled reminder
 * or immediate threshold alert fires, keeping the system notification bar and the
 * in-app Notification Center synchronized.
 */
data class NotificationLogItem(
    val id: String,
    val title: String,
    val body: String,
    val category: NotificationCategory,
    val route: NotificationActionRoute,
    val timestampMillis: Long,
    val isWarning: Boolean
) {
    companion object {
        const val MAX_STORED_ITEMS = 25

        fun encodeListToJson(items: List<NotificationLogItem>): String {
            val arr = JSONArray()
            items.take(MAX_STORED_ITEMS).forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("category", item.category.name)
                    put("route", item.route.name)
                    put("timestampMillis", item.timestampMillis)
                    put("isWarning", item.isWarning)
                }
                arr.put(obj)
            }
            return arr.toString()
        }

        fun decodeListFromJson(raw: String): List<NotificationLogItem> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                        val title = obj.optString("title").takeIf { it.isNotBlank() } ?: continue
                        val body = obj.optString("body")
                        val category = runCatching {
                            NotificationCategory.valueOf(obj.optString("category"))
                        }.getOrDefault(NotificationCategory.BUDGET_THRESHOLD)
                        val route = NotificationActionRoute.fromWireName(obj.optString("route"))
                            ?: NotificationActionRoute.OPEN_HOME
                        val ts = obj.optLong("timestampMillis", 0L)
                        val isWarning = obj.optBoolean("isWarning", false)
                        add(
                            NotificationLogItem(
                                id = id,
                                title = title,
                                body = body,
                                category = category,
                                route = route,
                                timestampMillis = ts,
                                isWarning = isWarning
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
