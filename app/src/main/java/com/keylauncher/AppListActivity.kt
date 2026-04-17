package com.keylauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
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

        val letter = intent.getStringExtra(EXTRA_LETTER) ?: run {
            finish()
            return
        }

        findViewById<TextView>(R.id.title_text).text = letter

        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        @Suppress("DEPRECATION")
        val resolvedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val apps = resolvedApps
            .map { info ->
                AppInfo(
                    name = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    icon = info.loadIcon(pm)
                )
            }
            .filter { it.name.startsWith(letter, ignoreCase = true) }
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
}
