package com.prism.studio.editor

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.prism.studio.data.WidgetRepository
import com.prism.studio.data.catalog.PrismCatalog
import com.prism.studio.design.PrismTheme
import com.prism.studio.model.WidgetSpec
import com.prism.studio.render.PrismRenderer
import com.prism.studio.render.WidgetData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Launched by the widget host when a Prism widget is dropped, and again from "Edit" on a placed one.
 *
 * This activity is exported — the host has to be able to start it — so it treats its own intent as
 * untrusted. Three guards, in order:
 *
 * 1. `RESULT_CANCELED` is set *first*. If the user backs out, or anything below fails, the host
 *    removes the half-placed widget instead of leaving a dead cell on the home screen. Forgetting
 *    this is the single most common bug in widget configuration activities.
 * 2. The incoming `EXTRA_APPWIDGET_ID` is validated against `AppWidgetManager` rather than trusted.
 *    An arbitrary app can start an exported activity with any extras it likes; an id we do not own
 *    finishes immediately.
 * 3. Nothing is persisted until the user confirms.
 */
@AndroidEntryPoint
class WidgetConfigureActivity : ComponentActivity() {

    @Inject lateinit var repository: WidgetRepository
    @Inject lateinit var catalog: PrismCatalog
    @Inject lateinit var renderer: PrismRenderer

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guard 1.
        setResult(Activity.RESULT_CANCELED, resultIntent())

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Guard 2.
        if (!isOurWidget(appWidgetId)) {
            finish()
            return
        }

        val family = catalog.families.first()
        val variant = family.variants.first()
        val initial = com.prism.studio.model.ResolvedWidget(
            family = family,
            variant = variant,
            style = variant.styleDelta.applyTo(family.base),
            options = emptyMap(),
        )

        setContent {
            PrismTheme {
                var state by remember { mutableStateOf(EditorState(initial)) }
                EditorScreen(
                    state = state,
                    sampleData = WidgetData.Clock(LocalDateTime.now(), is24Hour = false),
                    renderer = renderer,
                    wallpaperSchemes = emptyList(),
                    userPresets = emptyList(),
                    onState = { state = it },
                    onSavePreset = { /* Preset persistence lands with the shell. */ },
                    onApply = { commit(state) },
                )
            }
        }
    }

    /**
     * An id is ours only if the manager knows it and its provider is one of ours. Checking the
     * provider matters: an id can be valid and belong to a different app's widget entirely.
     */
    private fun isOurWidget(id: Int): Boolean {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id) ?: return false
        return info.provider?.packageName == packageName
    }

    private fun commit(state: EditorState) {
        val spec = WidgetSpec(
            family = state.widget.family.id,
            variant = state.widget.variant.id,
            userDelta = state.delta,
        )
        lifecycleScope.launch {
            repository.save(appWidgetId, spec)
            setResult(Activity.RESULT_OK, resultIntent())
            // The host broadcasts APPWIDGET_UPDATE on RESULT_OK, which is what draws the widget.
            // Doing it ourselves here would be a second redraw for no benefit, and would require
            // this feature module to depend on :widget.
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
