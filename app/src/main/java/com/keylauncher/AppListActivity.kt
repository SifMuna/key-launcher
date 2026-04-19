package com.keylauncher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LETTER = "letter"
        const val EXTRA_WIDGET_HEIGHT_DP = "widget_height_dp"
    }

    private var currentLetter = "A"
    private var cachedApps: List<AppInfo>? = null

    private lateinit var titleText: TextView
    private lateinit var noAppsText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_app_list)

        currentLetter = intent.getStringExtra(EXTRA_LETTER) ?: run { finish(); return }

        titleText = findViewById(R.id.title_text)
        noAppsText = findViewById(R.id.no_apps_text)
        recyclerView = findViewById(R.id.recycler_view)

        // Tap the transparent backdrop to dismiss
        findViewById<View>(R.id.backdrop).setOnClickListener { finish() }

        setupKeyboardListeners()

        // Load and display initial results
        val cached = AppCache.get()
        if (cached != null) {
            cachedApps = cached
            showResults(cached)
            AppCache.warm(this)
        } else {
            Thread {
                val apps = AppCache.getOrLoad(this)
                cachedApps = apps
                runOnUiThread { showResults(apps) }
            }.start()
        }
    }

    private fun showResults(all: List<AppInfo>) {
        titleText.text = currentLetter
        val filtered = all.filter { it.name.startsWith(currentLetter, ignoreCase = true) }
        if (filtered.isEmpty()) {
            recyclerView.visibility = View.GONE
            noAppsText.visibility = View.VISIBLE
            noAppsText.text = getString(R.string.no_apps_found, currentLetter)
        } else {
            noAppsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = AppAdapter(filtered) { app ->
                packageManager.getLaunchIntentForPackage(app.packageName)?.let { launch ->
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                    finish()
                }
            }
        }
    }

    private fun setupKeyboardListeners() {
        val keyButtonMap = mapOf(
            "Q" to R.id.btn_q, "W" to R.id.btn_w, "E" to R.id.btn_e,
            "R" to R.id.btn_r, "T" to R.id.btn_t, "Y" to R.id.btn_y,
            "U" to R.id.btn_u, "I" to R.id.btn_i, "O" to R.id.btn_o,
            "P" to R.id.btn_p, "A" to R.id.btn_a, "S" to R.id.btn_s,
            "D" to R.id.btn_d, "F" to R.id.btn_f, "G" to R.id.btn_g,
            "H" to R.id.btn_h, "J" to R.id.btn_j, "K" to R.id.btn_k,
            "L" to R.id.btn_l, "Z" to R.id.btn_z, "X" to R.id.btn_x,
            "C" to R.id.btn_c, "V" to R.id.btn_v, "B" to R.id.btn_b,
            "N" to R.id.btn_n, "M" to R.id.btn_m
        )

        for ((letter, buttonId) in keyButtonMap) {
            findViewById<View>(buttonId).setOnClickListener {
                currentLetter = letter
                cachedApps?.let { showResults(it) }
            }
        }

        // Refresh: force a full rebuild of the app list, then redisplay
        findViewById<View>(R.id.btn_refresh).setOnClickListener {
            AppCache.forceRefresh(this) {
                val apps = AppCache.get() ?: return@forceRefresh
                cachedApps = apps
                runOnUiThread { showResults(apps) }
            }
        }
    }
}
