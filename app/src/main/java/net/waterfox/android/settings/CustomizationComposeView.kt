/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import net.waterfox.android.R
import net.waterfox.android.compose.preference.PreferenceCategory
import net.waterfox.android.compose.preference.RadioGroupItem
import net.waterfox.android.compose.preference.RadioGroupPreference
import net.waterfox.android.compose.preference.SwitchPreference
import net.waterfox.android.theme.WaterfoxTheme

class CustomizationComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onLightThemeClick by mutableStateOf({})
    var onDarkThemeClick by mutableStateOf({})
    var onSetByBatterySaverClick by mutableStateOf({})
    var onFollowDeviceThemeClick by mutableStateOf({})

    @Composable
    override fun Content() {
        WaterfoxTheme {
            Column {
                PreferenceCategory(
                    title = stringResource(R.string.preferences_theme),
                    allowDividerAbove = false,
                ) {
                    RadioGroupPreference(
                        items = listOf(
                            RadioGroupItem(
                                title = stringResource(R.string.preference_light_theme),
                                key = stringResource(R.string.pref_key_light_theme),
                                defaultValue = booleanResource(R.bool.underAPI28),
                                onClick = onLightThemeClick,
                            ),
                            RadioGroupItem(
                                title = stringResource(R.string.preference_dark_theme),
                                key = stringResource(R.string.pref_key_dark_theme),
                                defaultValue = false,
                                onClick = onDarkThemeClick,
                            ),
                            RadioGroupItem(
                                title = stringResource(R.string.preference_auto_battery_theme),
                                key = stringResource(R.string.pref_key_auto_battery_theme),
                                defaultValue = false,
                                visible = booleanResource(R.bool.underAPI28),
                                onClick = onSetByBatterySaverClick,
                            ),
                            RadioGroupItem(
                                title = stringResource(R.string.preference_follow_device_theme),
                                key = stringResource(R.string.pref_key_follow_device_theme),
                                defaultValue = booleanResource(R.bool.API28),
                                visible = booleanResource(R.bool.API28),
                                onClick = onFollowDeviceThemeClick,
                            ),
                        ),
                    )
                }

                PreferenceCategory(title = stringResource(R.string.preferences_toolbar)) {
                    RadioGroupPreference(
                        items = listOf(
                            RadioGroupItem(
                                title = stringResource(R.string.preference_top_toolbar),
                                key = stringResource(R.string.pref_key_toolbar_top),
                                defaultValue = false,
                            ),
                            RadioGroupItem(
                                title = stringResource(R.string.preference_bottom_toolbar),
                                key = stringResource(R.string.pref_key_toolbar_bottom),
                                defaultValue = true,
                            ),
                        ),
                    )
                }

                PreferenceCategory(title = stringResource(R.string.preferences_gestures)) {
                    SwitchPreference(
                        title = stringResource(R.string.preference_gestures_dynamic_toolbar),
                        key = stringResource(R.string.pref_key_dynamic_toolbar),
                        defaultValue = true,
                    )
                    SwitchPreference(
                        title = stringResource(R.string.preference_gestures_swipe_toolbar_switch_tabs),
                        key = stringResource(R.string.pref_key_swipe_toolbar_switch_tabs),
                        defaultValue = true,
                    )
                }
            }
        }
    }

}
