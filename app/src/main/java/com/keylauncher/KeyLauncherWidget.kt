package com.keylauncher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class KeyLauncherWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_CACHE = "com.keylauncher.ACTION_REFRESH_CACHE"
    }

    override fun onEnabled(context: Context) {
        AppCache.warm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_CACHE) {
            AppCache.forceRefresh(context.applicationContext)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        AppCache.warm(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val widgetHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
        setupKeyListeners(context, views, widgetHeightDp)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setupKeyListeners(context: Context, views: RemoteViews, widgetHeightDp: Int) {
        val keyButtonMap = mapOf(
            'Q' to R.id.btn_q, 'W' to R.id.btn_w, 'E' to R.id.btn_e,
            'R' to R.id.btn_r, 'T' to R.id.btn_t, 'Y' to R.id.btn_y,
            'U' to R.id.btn_u, 'I' to R.id.btn_i, 'O' to R.id.btn_o,
            'P' to R.id.btn_p, 'A' to R.id.btn_a, 'S' to R.id.btn_s,
            'D' to R.id.btn_d, 'F' to R.id.btn_f, 'G' to R.id.btn_g,
            'H' to R.id.btn_h, 'J' to R.id.btn_j, 'K' to R.id.btn_k,
            'L' to R.id.btn_l, 'Z' to R.id.btn_z, 'X' to R.id.btn_x,
            'C' to R.id.btn_c, 'V' to R.id.btn_v, 'B' to R.id.btn_b,
            'N' to R.id.btn_n, 'M' to R.id.btn_m
        )

        for ((letter, buttonId) in keyButtonMap) {
            val intent = Intent(context, AppListActivity::class.java).apply {
                putExtra(AppListActivity.EXTRA_LETTER, letter.toString())
                putExtra(AppListActivity.EXTRA_WIDGET_HEIGHT_DP, widgetHeightDp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                letter.code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(buttonId, pendingIntent)
        }

        // Refresh button: broadcasts to this receiver to force a cache rebuild
        val refreshIntent = Intent(context, KeyLauncherWidget::class.java).apply {
            action = ACTION_REFRESH_CACHE
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
    }
}
