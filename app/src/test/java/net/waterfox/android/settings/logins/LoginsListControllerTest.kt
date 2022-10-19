/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.logins

import androidx.navigation.NavController
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.BrowserDirection
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.settings.SupportUtils
import net.waterfox.android.settings.logins.controller.LoginsListController
import net.waterfox.android.settings.logins.fragment.SavedLoginsFragmentDirections
import net.waterfox.android.utils.Settings

@RunWith(WaterfoxRobolectricTestRunner::class)
class LoginsListControllerTest {

    private val store: LoginsFragmentStore = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val sortingStrategy: SortingStrategy = SortingStrategy.Alphabetically
    private val navController: NavController = mockk(relaxed = true)
    private val browserNavigator: (String, Boolean, BrowserDirection) -> Unit = mockk(relaxed = true)
    private val controller =
        LoginsListController(
            loginsFragmentStore = store,
            navController = navController,
            browserNavigator = browserNavigator,
            settings = settings,
        )

    @Test
    fun `handle selecting the sorting strategy and save pref`() {
        controller.handleSort(sortingStrategy)

        verifyAll {
            store.dispatch(LoginsAction.SortLogins(SortingStrategy.Alphabetically))
            settings.savedLoginsSortingStrategy = sortingStrategy
        }
    }

    @Test
    fun `handle login item clicked`() {
        val login: SavedLogin = mockk(relaxed = true)

        controller.handleItemClicked(login)

        verifyAll {
            store.dispatch(LoginsAction.LoginSelected(login))
            navController.navigate(
                SavedLoginsFragmentDirections.actionSavedLoginsFragmentToLoginDetailFragment(login.guid)
            )
        }
    }

    @Test
    fun `Open the correct support webpage when Learn More is clicked`() {
        controller.handleLearnMoreClicked()

        verifyAll {
            browserNavigator.invoke(
                SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.SYNC_SETUP),
                true,
                BrowserDirection.FromSavedLoginsFragment
            )
        }
    }

    @Test
    fun `handle add login clicked`() {
        controller.handleAddLoginClicked()

        verifyAll {
            navController.navigate(
                SavedLoginsFragmentDirections.actionSavedLoginsFragmentToAddLoginFragment()
            )
        }
    }
}
