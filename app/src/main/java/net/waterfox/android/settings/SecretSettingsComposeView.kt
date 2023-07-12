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
import net.waterfox.android.compose.preference.SwitchPreference
import net.waterfox.android.theme.WaterfoxTheme

class SecretSettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onAllowThirdPartyRootCertsChange by mutableStateOf<(Boolean) -> Unit>({})

    @Composable
    override fun Content() {
        WaterfoxTheme {
            Column {
                SwitchPreference(
                    title = stringResource(R.string.preferences_debug_settings_allow_third_party_root_certs),
                    summary = stringResource(R.string.preferences_debug_settings_allow_third_party_root_certs_summary),
                    key = stringResource(R.string.pref_key_allow_third_party_root_certs),
                    defaultValue = false,
                    onChange = onAllowThirdPartyRootCertsChange,
                    iconSpaceReserved = false,
                )
                SwitchPreference(
                    title = stringResource(R.string.preferences_debug_settings_task_continuity),
                    key = stringResource(R.string.pref_key_enable_task_continuity),
                    defaultValue = false,
                    iconSpaceReserved = false,
                )
                SwitchPreference(
                    title = stringResource(R.string.preferences_debug_settings_unified_search),
                    key = stringResource(R.string.pref_key_show_unified_search),
                    defaultValue = false,
                    iconSpaceReserved = false,
                )
            }
        }
    }

}
