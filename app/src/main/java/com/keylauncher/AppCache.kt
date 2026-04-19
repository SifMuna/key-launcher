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
    private const val PREF_FILE = "app_cache_v1"
    private const val KEY_DATA = "apps"

    fun get(): List<AppInfo>? = entries

    /** Schedule a background refresh if the cache is absent or older than TTL. */
    fun warm(context: Context) {
        val age = System.currentTimeMillis() - loadedAt
        if (loading.get() || (entries != null && age < TTL_MS)) return
        if (!loading.compareAndSet(false, true)) return
        executor.execute {
            val apps = buildList(context.applicationContext)
            entries = apps
            loadedAt = System.currentTimeMillis()
            saveToDisk(context.applicationContext, apps)
            loading.set(false)
        }
    }

    /** Force a full cache rebuild, save to disk, then invoke onDone on a background thread. */
    fun forceRefresh(context: Context, onDone: (() -> Unit)? = null) {
        // Reset so we always run even if another refresh is in progress
        loading.set(true)
        executor.execute {
            val apps = buildList(context.applicationContext)
            entries = apps
            loadedAt = System.currentTimeMillis()
            saveToDisk(context.applicationContext, apps)
            loading.set(false)
            onDone?.invoke()
        }
    }

    /**
     * Return cached entries immediately, or load from the on-disk cache (fast), or as a
     * last resort query Android directly (slow). Blocks the calling thread.
     */
    fun getOrLoad(context: Context): List<AppInfo> {
        entries?.let { return it }
        // Disk cache is much faster than querying Android
        loadFromDisk(context.applicationContext)?.let { disk ->
            entries = disk
            loadedAt = System.currentTimeMillis()
            return disk
        }
        val result = buildList(context.applicationContext)
        entries = result
        loadedAt = System.currentTimeMillis()
        saveToDisk(context.applicationContext, result)
        loading.set(false)
        return result
    }

    private fun saveToDisk(context: Context, apps: List<AppInfo>) {
        val serialized = apps.joinToString("\n") { "${it.name}\t${it.packageName}" }
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_DATA, serialized).apply()
    }

    private fun loadFromDisk(context: Context): List<AppInfo>? {
        val raw = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_DATA, null)?.takeIf { it.isNotBlank() } ?: return null
        val parsed = raw.lines().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) AppInfo(line.substring(0, tab), line.substring(tab + 1)) else null
        }
        return parsed.ifEmpty { null }
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
