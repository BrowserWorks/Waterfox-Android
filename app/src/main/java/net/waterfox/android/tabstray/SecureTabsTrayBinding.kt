/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray

import android.view.WindowManager
import androidx.fragment.app.Fragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import net.waterfox.android.ext.removeSecure
import net.waterfox.android.ext.secure
import net.waterfox.android.utils.Settings

/**
 * Sets TabsTrayFragment flags to secure when private tabs list is selected.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecureTabsTrayBinding(
    store: TabsTrayStore,
    private val settings: Settings,
    private val fragment: Fragment,
    private val dialog: TabsTrayDialog
) : AbstractBinding<TabsTrayState>(store) {

    override suspend fun onState(flow: Flow<TabsTrayState>) {
        flow.map { it }
            .ifAnyChanged { state ->
                arrayOf(
                    state.selectedPage
                )
            }
            .collect { state ->
                if (state.selectedPage == Page.PrivateTabs &&
                    !settings.allowScreenshotsInPrivateMode
                ) {
                    fragment.secure()
                    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else if (!settings.lastKnownMode.isPrivate) {
                    fragment.removeSecure()
                    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
    }
}
