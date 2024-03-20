/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray

import androidx.navigation.NavController
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.service.fxa.manager.FxaAccountManager
import net.waterfox.android.components.accounts.WaterfoxFxAEntryPoint
import net.waterfox.android.home.HomeFragment
import net.waterfox.android.tabstray.ext.isActiveDownload

/**
 * An interactor that helps with navigating to different parts of the app from the tabs tray.
 */
@Suppress("TooManyFunctions")
interface NavigationInteractor {

    /**
     * Called when tab tray should be dismissed.
     */
    fun onTabTrayDismissed()

    /**
     * Called when clicking the account settings button.
     */
    fun onAccountSettingsClicked()

    /**
     * Called when clicking the share tabs button.
     */
    fun onShareTabsOfTypeClicked(private: Boolean)

    /**
     * Called when clicking the tab settings button.
     */
    fun onTabSettingsClicked()

    /**
     * Called when clicking the close all tabs button.
     */
    fun onCloseAllTabsClicked(private: Boolean)

    /**
     * Called when cancelling private downloads confirmed.
     */
    fun onCloseAllPrivateTabsWarningConfirmed(private: Boolean)

    /**
     * Called when opening the recently closed tabs menu button.
     */
    fun onOpenRecentlyClosedClicked()
}

/**
 * A default implementation of [NavigationInteractor].
 */
@Suppress("TooManyFunctions")
class DefaultNavigationInteractor(
    private val browserStore: BrowserStore,
    private val navController: NavController,
    private val dismissTabTray: () -> Unit,
    private val dismissTabTrayAndNavigateHome: (sessionId: String) -> Unit,
    private val showCancelledDownloadWarning: (downloadCount: Int, tabId: String?, source: String?) -> Unit,
    private val accountManager: FxaAccountManager,
) : NavigationInteractor {

    override fun onTabTrayDismissed() {
        dismissTabTray()
    }

    override fun onAccountSettingsClicked() {
        val isSignedIn = accountManager.authenticatedAccount() != null

        val direction = if (isSignedIn) {
            TabsTrayFragmentDirections.actionGlobalAccountSettingsFragment()
        } else {
            TabsTrayFragmentDirections.actionGlobalTurnOnSync(entrypoint = WaterfoxFxAEntryPoint.NavigationInteraction)
        }
        navController.navigate(direction)
    }

    override fun onTabSettingsClicked() {
        navController.navigate(
            TabsTrayFragmentDirections.actionGlobalTabSettingsFragment(),
        )
    }

    override fun onOpenRecentlyClosedClicked() {
        navController.navigate(
            TabsTrayFragmentDirections.actionGlobalRecentlyClosed(),
        )
    }

    override fun onShareTabsOfTypeClicked(private: Boolean) {
        val tabs = browserStore.state.getNormalOrPrivateTabs(private)
        val data = tabs.map {
            ShareData(url = it.content.url, title = it.content.title)
        }
        val directions = TabsTrayFragmentDirections.actionGlobalShareFragment(
            data = data.toTypedArray(),
        )
        navController.navigate(directions)
    }

    override fun onCloseAllTabsClicked(private: Boolean) {
        closeAllTabs(private, isConfirmed = false)
    }

    override fun onCloseAllPrivateTabsWarningConfirmed(private: Boolean) {
        closeAllTabs(private, isConfirmed = true)
    }

    private fun closeAllTabs(private: Boolean, isConfirmed: Boolean) {
        val sessionsToClose = if (private) {
            HomeFragment.ALL_PRIVATE_TABS
        } else {
            HomeFragment.ALL_NORMAL_TABS
        }

        if (private && !isConfirmed) {
            val privateDownloads = browserStore.state.downloads.filter {
                it.value.private && it.value.isActiveDownload()
            }
            if (privateDownloads.isNotEmpty()) {
                showCancelledDownloadWarning(privateDownloads.size, null, null)
                return
            }
        }
        dismissTabTrayAndNavigateHome(sessionsToClose)
    }
}
