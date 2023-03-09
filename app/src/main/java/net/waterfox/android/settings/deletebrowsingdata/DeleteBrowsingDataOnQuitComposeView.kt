/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.deletebrowsingdata

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
import net.waterfox.android.compose.preference.CheckboxPreference
import net.waterfox.android.compose.preference.SwitchPreference
import net.waterfox.android.theme.WaterfoxTheme

class DeleteBrowsingDataOnQuitComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onDeleteBrowsingDataOnQuitChange by mutableStateOf<(Boolean) -> Unit>({})
    var onTabsChange by mutableStateOf<(Boolean) -> Unit>({})
    var onHistoryChange by mutableStateOf<(Boolean) -> Unit>({})
    var onCookiesChange by mutableStateOf<(Boolean) -> Unit>({})
    var onCacheChange by mutableStateOf<(Boolean) -> Unit>({})
    var onPermissionsChange by mutableStateOf<(Boolean) -> Unit>({})
    var onDownloadsChange by mutableStateOf<(Boolean) -> Unit>({})
    var checkboxesEnabled by mutableStateOf(false)

    @Composable
    override fun Content() {
        WaterfoxTheme {
            Column {
                SwitchPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_on_quit),
                    summary = stringResource(R.string.preference_summary_delete_browsing_data_on_quit_2),
                    key = stringResource(R.string.pref_key_delete_browsing_data_on_quit),
                    defaultValue = checkboxesEnabled,
                    onChange = onDeleteBrowsingDataOnQuitChange,
                )
                CheckboxPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_tabs_title_2),
                    key = stringResource(R.string.pref_key_delete_open_tabs_on_quit),
                    defaultValue = false,
                    onChange = onTabsChange,
                    enabled = checkboxesEnabled,
                )
                CheckboxPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_browsing_data_title),
                    key = stringResource(R.string.pref_key_delete_browsing_history_on_quit),
                    defaultValue = false,
                    onChange = onHistoryChange,
                    enabled = checkboxesEnabled,
                )
                CheckboxPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_cookies),
                    summary = stringResource(R.string.preferences_delete_browsing_data_cookies_subtitle),
                    key = stringResource(R.string.pref_key_delete_cookies_on_quit),
                    defaultValue = false,
                    onChange = onCookiesChange,
                    enabled = checkboxesEnabled,
                )
                CheckboxPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_cached_files),
                    summary = stringResource(R.string.preferences_delete_browsing_data_cached_files_subtitle),
                    key = stringResource(R.string.pref_key_delete_caches_on_quit),
                    defaultValue = false,
                    onChange = onCacheChange,
                    enabled = checkboxesEnabled,
                )
                CheckboxPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_site_permissions),
                    key = stringResource(R.string.pref_key_delete_permissions_on_quit),
                    defaultValue = false,
                    onChange = onPermissionsChange,
                    enabled = checkboxesEnabled,
                )
                CheckboxPreference(
                    title = stringResource(R.string.preferences_delete_browsing_data_downloads),
                    key = stringResource(R.string.pref_key_delete_downloads_on_quit),
                    defaultValue = false,
                    onChange = onDownloadsChange,
                    enabled = checkboxesEnabled,
                )
            }
        }
    }

}
