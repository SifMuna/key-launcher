package com.keylauncher

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AppCache {

    @Volatile private var entries: List<AppInfo>? = null
    @Volatile private var loadedAt: Long = 0
    private val loading = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private const val TTL_MS = 10 * 60 * 1000L

    fun get(): List<AppInfo>? = entries

    /** Schedule a background refresh if the cache is absent or older than TTL. */
    fun warm(context: Context) {
        val age = System.currentTimeMillis() - loadedAt
        if (loading.get() || (entries != null && age < TTL_MS)) return
        if (!loading.compareAndSet(false, true)) return
        executor.execute {
            entries = buildList(context.applicationContext)
            loadedAt = System.currentTimeMillis()
            loading.set(false)
        }
    }

    /** Return cached entries immediately, or block the calling thread to build them. */
    fun getOrLoad(context: Context): List<AppInfo> {
        entries?.let { return it }
        val result = buildList(context.applicationContext)
        entries = result
        loadedAt = System.currentTimeMillis()
        loading.set(false)
        return result
    }

    private fun buildList(context: Context): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .mapNotNull { appInfo ->
                try {
                    AppInfo(pm.getApplicationLabel(appInfo).toString(), appInfo.packageName)
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.name.lowercase() }
    }
}
