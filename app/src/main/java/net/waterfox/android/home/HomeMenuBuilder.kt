/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home

import android.content.Context
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.menu.view.MenuButton
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.components.accounts.AccountState
import net.waterfox.android.ext.nav
import net.waterfox.android.ext.settings
import net.waterfox.android.settings.SupportUtils
import net.waterfox.android.settings.deletebrowsingdata.deleteAndQuit
import net.waterfox.android.theme.ThemeManager
import net.waterfox.android.whatsnew.WhatsNew
import java.lang.ref.WeakReference

/**
 * Helper class for building the [HomeMenu].
 *
 * @property view The [View] to attach the snackbar to.
 * @property context  An Android [Context].
 * @property lifecycleOwner [LifecycleOwner] for the view.
 * @property homeActivity [HomeActivity] used to open URLs in a new tab.
 * @property navController [NavController] used for navigation.
 * @property menuButton The [MenuButton] that will be used to create a menu when the button is
 * clicked.
 * @property hideOnboardingIfNeeded Lambda invoked to dismiss onboarding.
 */
@Suppress("LongParameterList")
class HomeMenuBuilder(
    private val view: View,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val homeActivity: HomeActivity,
    private val navController: NavController,
    private val menuButton: WeakReference<MenuButton>,
    private val hideOnboardingIfNeeded: () -> Unit,
) {

    /**
     * Builds the [HomeMenu].
     */
    fun build() {
        HomeMenu(
            lifecycleOwner = lifecycleOwner,
            context = context,
            onItemTapped = ::onItemTapped,
            onHighlightPresent = { menuButton.get()?.setHighlight(it) },
            onMenuBuilderChanged = { menuButton.get()?.menuBuilder = it }
        )

        menuButton.get()?.setColorFilter(
            ContextCompat.getColor(
                context,
                ThemeManager.resolveAttribute(R.attr.textPrimary, context)
            )
        )
    }

    /**
     * Callback invoked when a menu item is tapped on.
     */
    @Suppress("LongMethod")
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun onItemTapped(item: HomeMenu.Item) {
        if (item !is HomeMenu.Item.DesktopMode) {
            hideOnboardingIfNeeded()
        }

        when (item) {
            HomeMenu.Item.Settings -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalSettingsFragment()
                )
            }
            HomeMenu.Item.CustomizeHome -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalHomeSettingsFragment()
                )
            }
            is HomeMenu.Item.SyncAccount -> {
                navController.nav(
                    R.id.homeFragment,
                    when (item.accountState) {
                        AccountState.AUTHENTICATED ->
                            HomeFragmentDirections.actionGlobalAccountSettingsFragment()
                        AccountState.NEEDS_REAUTHENTICATION ->
                            HomeFragmentDirections.actionGlobalAccountProblemFragment()
                        AccountState.NO_ACCOUNT ->
                            HomeFragmentDirections.actionGlobalTurnOnSync()
                    }
                )
            }
            HomeMenu.Item.Bookmarks -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id)
                )
            }
            HomeMenu.Item.History -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalHistoryFragment()
                )
            }
            HomeMenu.Item.Downloads -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalDownloadsFragment()
                )
            }
            HomeMenu.Item.Help -> {
                homeActivity.openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.getSumoURLForTopic(
                        context = context,
                        topic = SupportUtils.SumoTopic.HELP
                    ),
                    newTab = true,
                    from = BrowserDirection.FromHome
                )
            }
            HomeMenu.Item.WhatsNew -> {
                WhatsNew.userViewedWhatsNew(context)

                homeActivity.openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.getWhatsNewUrl(context),
                    newTab = true,
                    from = BrowserDirection.FromHome
                )
            }
            HomeMenu.Item.Quit -> {
                // We need to show the snackbar while the browsing data is deleting (if "Delete
                // browsing data on quit" is activated). After the deletion is over, the snackbar
                // is dismissed.
                deleteAndQuit(
                    activity = homeActivity,
                    coroutineScope = lifecycleOwner.lifecycleScope,
                    snackbar = WaterfoxSnackbar.make(
                        view = view,
                        isDisplayedWithBrowserToolbar = false
                    )
                )
            }
            HomeMenu.Item.ReconnectSync -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalAccountProblemFragment()
                )
            }
            HomeMenu.Item.Extensions -> {
                navController.nav(
                    R.id.homeFragment,
                    HomeFragmentDirections.actionGlobalAddonsManagementFragment()
                )
            }
            is HomeMenu.Item.DesktopMode -> {
                context.settings().openNextTabInDesktopMode = item.checked
            }
        }
    }
}
