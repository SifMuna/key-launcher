package com.keylauncher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class KeyLauncherWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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
        setupKeyListeners(context, views)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setupKeyListeners(context: Context, views: RemoteViews) {
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
    }
}
