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
import androidx.compose.ui.res.stringResource
import net.waterfox.android.R
import net.waterfox.android.compose.preference.*
import net.waterfox.android.theme.WaterfoxTheme

class HomeSettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onWallpapersClick by mutableStateOf({})

    @Composable
    override fun Content() {
        WaterfoxTheme {
            Column {
                SwitchPreference(
                    title = stringResource(R.string.top_sites_toggle_top_recent_sites_4),
                    key = stringResource(R.string.pref_key_show_top_sites),
                    defaultValue = true,
                )
                SwitchPreference(
                    title = stringResource(R.string.customize_toggle_jump_back_in),
                    key = stringResource(R.string.pref_key_recent_tabs),
                    defaultValue = true,
                )
                SwitchPreference(
                    title = stringResource(R.string.customize_toggle_recent_bookmarks),
                    key = stringResource(R.string.pref_key_recent_bookmarks),
                    defaultValue = true,
                )
                SwitchPreference(
                    title = stringResource(R.string.customize_toggle_recently_visited),
                    key = stringResource(R.string.pref_key_history_metadata_feature),
                    defaultValue = true,
                )
                TextOnlyPreference(
                    title = stringResource(R.string.customize_wallpapers),
                    key = stringResource(R.string.pref_key_wallpapers),
                    onClick = onWallpapersClick,
                )

                PreferenceCategory(title = stringResource(R.string.preferences_opening_screen)) {
                    RadioGroupPreference(
                        items = listOf(
                            RadioGroupItem(
                                title = stringResource(R.string.opening_screen_homepage),
                                key = stringResource(R.string.pref_key_start_on_home_always),
                                defaultValue = false,
                            ),
                            RadioGroupItem(
                                title = stringResource(R.string.opening_screen_last_tab),
                                key = stringResource(R.string.pref_key_start_on_home_never),
                                defaultValue = false,
                            ),
                            RadioGroupItem(
                                title = stringResource(R.string.opening_screen_after_four_hours_of_inactivity),
                                key = stringResource(R.string.pref_key_start_on_home_after_four_hours),
                                defaultValue = true,
                            ),
                        ),
                    )
                }
            }
        }
    }

}
