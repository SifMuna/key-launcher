package com.keylauncher

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LETTER = "letter"
        const val EXTRA_WIDGET_HEIGHT_DP = "widget_height_dp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw edge-to-edge so we can position the panel behind the nav bar correctly.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_app_list)

        val letter = intent.getStringExtra(EXTRA_LETTER) ?: run { finish(); return }

        val widgetHeightPx = (intent.getIntExtra(EXTRA_WIDGET_HEIGHT_DP, 110)
                * resources.displayMetrics.density).toInt()

        // Shift the panel up by (nav bar height + widget height) so it sits just above the widget.
        val panel = findViewById<View>(R.id.panel)
        ViewCompat.setOnApplyWindowInsetsListener(panel) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = v.layoutParams as FrameLayout.LayoutParams
            lp.bottomMargin = navBar + widgetHeightPx
            v.layoutParams = lp
            insets
        }

        findViewById<View>(R.id.backdrop).setOnClickListener { finish() }
        panel.setOnClickListener { /* consume — prevents backdrop listener from firing */ }

        findViewById<TextView>(R.id.title_text).text = letter

        val noAppsText = findViewById<TextView>(R.id.no_apps_text)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        fun showResults(all: List<AppInfo>) {
            val filtered = all.filter { it.name.startsWith(letter, ignoreCase = true) }
            if (filtered.isEmpty()) {
                recyclerView.visibility = View.GONE
                noAppsText.visibility = View.VISIBLE
                noAppsText.text = getString(R.string.no_apps_found, letter)
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

        val cached = AppCache.get()
        if (cached != null) {
            showResults(cached)
            AppCache.warm(this)  // refresh in background if TTL expired
        } else {
            // Cache cold (first ever tap): load off the main thread, then display.
            Thread {
                val apps = AppCache.getOrLoad(this)
                runOnUiThread { showResults(apps) }
            }.start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val panel = findViewById<View>(R.id.panel)
            val rect = Rect()
            panel.getGlobalVisibleRect(rect)
            if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                finish()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
