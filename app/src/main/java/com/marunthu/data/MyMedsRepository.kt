package com.marunthu.data

import android.content.Context

/**
 * The user's own regular medicines ("My Meds"), persisted locally via SharedPreferences —
 * fully offline, nothing leaves the phone. Stores only canonical medicine ids; the catalog
 * resolves them back to [com.marunthu.core.model.Medicine].
 *
 * This powers Marunthu's PROACTIVE safety: when the user scans a NEW medicine, it is checked
 * against everything already in My Meds — the "pharmacist who knows your regimen" behaviour
 * that lookup apps don't have.
 */
class MyMedsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("marunthu_my_meds", Context.MODE_PRIVATE)

    fun ids(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty()

    fun add(canonicalId: String) {
        prefs.edit().putStringSet(KEY, ids() + canonicalId).apply()
    }

    fun remove(canonicalId: String) {
        prefs.edit().putStringSet(KEY, ids() - canonicalId).apply()
    }

    fun contains(canonicalId: String) = canonicalId in ids()

    private companion object { const val KEY = "ids" }
}
