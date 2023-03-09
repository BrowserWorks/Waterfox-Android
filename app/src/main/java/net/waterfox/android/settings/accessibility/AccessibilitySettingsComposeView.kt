/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.accessibility

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import net.waterfox.android.R
import net.waterfox.android.compose.preference.SwitchPreference
import net.waterfox.android.theme.WaterfoxTheme

class AccessibilitySettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onAutoFontSizingChange by mutableStateOf<(Boolean) -> Unit>({})
    var onFontSizeChange by mutableStateOf<(Float) -> Unit>({})
    var onForceZoomChange by mutableStateOf<(Boolean) -> Unit>({})
    var fontSizePreferenceEnabled by mutableStateOf(false)

    @Composable
    override fun Content() {
        WaterfoxTheme {
            LazyColumn {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.preference_accessibility_auto_size_2),
                        summary = stringResource(R.string.preference_accessibility_auto_size_summary),
                        key = stringResource(R.string.pref_key_accessibility_auto_size),
                        defaultValue = true,
                        onChange = onAutoFontSizingChange,
                    )
                }

                item {
                    FontSizePreference(
                        key = stringResource(R.string.pref_key_accessibility_font_scale),
                        defaultValue = 10f,
                        onChange = onFontSizeChange,
                        enabled = fontSizePreferenceEnabled,
                    )
                }

                item {
                    SwitchPreference(
                        title = stringResource(R.string.preference_accessibility_force_enable_zoom),
                        summary = stringResource(R.string.preference_accessibility_force_enable_zoom_summary),
                        key = stringResource(R.string.pref_key_accessibility_force_enable_zoom),
                        defaultValue = false,
                        onChange = onForceZoomChange,
                        allowDividerAbove = true,
                    )
                }
            }
        }
    }

}
