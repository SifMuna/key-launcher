package com.keylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LETTER = "letter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        val letter = intent.getStringExtra(EXTRA_LETTER) ?: run { finish(); return }

        findViewById<View>(R.id.backdrop).setOnClickListener { finish() }
        // Panel swallows clicks so they don't reach the backdrop and close the sheet.
        findViewById<View>(R.id.panel).setOnClickListener { }

        findViewById<TextView>(R.id.title_text).text = letter

        val pm = packageManager

        val apps = pm.getInstalledApplications(0)
            .filter { appInfo -> pm.getLaunchIntentForPackage(appInfo.packageName) != null }
            .mapNotNull { appInfo ->
                try {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    if (appName.startsWith(letter, ignoreCase = true)) {
                        AppInfo(
                            name = appName,
                            packageName = appInfo.packageName,
                            icon = pm.getApplicationIcon(appInfo)
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.name.lowercase() }

        val noAppsText = findViewById<TextView>(R.id.no_apps_text)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        if (apps.isEmpty()) {
            recyclerView.visibility = View.GONE
            noAppsText.visibility = View.VISIBLE
            noAppsText.text = getString(R.string.no_apps_found, letter)
        } else {
            noAppsText.visibility = View.GONE
            recyclerView.layoutManager = GridLayoutManager(this, 4)
            recyclerView.adapter = AppAdapter(apps) { app ->
                pm.getLaunchIntentForPackage(app.packageName)?.let { launch ->
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                    finish()
                }
            }
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
