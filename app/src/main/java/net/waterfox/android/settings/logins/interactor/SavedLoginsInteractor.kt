/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.logins.interactor

import net.waterfox.android.settings.logins.SavedLogin
import net.waterfox.android.settings.logins.SortingStrategy
import net.waterfox.android.settings.logins.controller.LoginsListController
import net.waterfox.android.settings.logins.controller.SavedLoginsStorageController

/**
 * Interactor for the saved logins screen
 *
 * @param loginsListController [LoginsListController] which will be delegated for all
 * user interactions.
 * @param savedLoginsStorageController [SavedLoginsStorageController] which will be delegated
 * for all calls to the password storage component
 */
class SavedLoginsInteractor(
    private val loginsListController: LoginsListController,
    private val savedLoginsStorageController: SavedLoginsStorageController
) {
    fun onItemClicked(item: SavedLogin) {
        loginsListController.handleItemClicked(item)
    }

    fun onLearnMoreClicked() {
        loginsListController.handleLearnMoreClicked()
    }

    fun onSortingStrategyChanged(sortingStrategy: SortingStrategy) {
        loginsListController.handleSort(sortingStrategy)
    }

    fun loadAndMapLogins() {
        savedLoginsStorageController.handleLoadAndMapLogins()
    }

    fun onAddLoginClick() {
        loginsListController.handleAddLoginClicked()
    }
}
