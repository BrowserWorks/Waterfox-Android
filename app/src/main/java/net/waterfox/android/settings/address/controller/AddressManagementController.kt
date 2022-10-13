/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.address.controller

import androidx.navigation.NavController
import mozilla.components.concept.storage.Address
import net.waterfox.android.settings.address.AddressManagementFragment
import net.waterfox.android.settings.address.AddressManagementFragmentDirections
import net.waterfox.android.settings.address.interactor.AddressManagementInteractor

/**
 * [AddressManagementFragment] controller. An interface that handles the view manipulation of
 * the address manager triggered by the interactor.
 */
interface AddressManagementController {

    /**
     * @see [AddressManagementInteractor.onSelectAddress]
     */
    fun handleAddressClicked(address: Address)

    /**
     * @see [AddressManagementInteractor.onAddAddressClick]
     */
    fun handleAddAddressButtonClicked()
}

/**
 * The default implementation of [AddressManagementController].
 *
 * @param navController [NavController] used for navigation.
 */
class DefaultAddressManagementController(
    private val navController: NavController
) : AddressManagementController {

    override fun handleAddressClicked(address: Address) {
        navigateToAddressEditor(address)
    }

    override fun handleAddAddressButtonClicked() {
        navigateToAddressEditor()
    }

    private fun navigateToAddressEditor(address: Address? = null) {
        navController.navigate(
            AddressManagementFragmentDirections
                .actionAddressManagementFragmentToAddressEditorFragment(
                    address = address
                )
        )
    }
}
