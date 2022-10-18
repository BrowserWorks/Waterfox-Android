/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.observeAsComposableState
import net.waterfox.android.components.StoreProvider
import net.waterfox.android.ext.components
import net.waterfox.android.settings.address.controller.DefaultAddressManagementController
import net.waterfox.android.settings.address.interactor.AddressManagementInteractor
import net.waterfox.android.settings.address.interactor.DefaultAddressManagementInteractor
import net.waterfox.android.settings.address.view.AddressList
import net.waterfox.android.settings.autofill.AutofillAction
import net.waterfox.android.settings.autofill.AutofillFragmentState
import net.waterfox.android.settings.autofill.AutofillFragmentStore
import net.waterfox.android.theme.WaterfoxTheme

/**
 * Displays a list of saved addresses.
 */
class AddressManagementFragment : Fragment() {

    private lateinit var store: AutofillFragmentStore
    private lateinit var interactor: AddressManagementInteractor

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        store = StoreProvider.get(this) {
            AutofillFragmentStore(AutofillFragmentState())
        }

        interactor = DefaultAddressManagementInteractor(
            controller = DefaultAddressManagementController(
                navController = findNavController()
            )
        )

        loadAddresses()

        return ComposeView(requireContext()).apply {
            setContent {
                WaterfoxTheme {
                    val addresses = store.observeAsComposableState { state -> state.addresses }

                    AddressList(
                        addresses = addresses.value ?: emptyList(),
                        onAddressClick = {
                            interactor.onSelectAddress(it)
                        },
                        onAddAddressButtonClick = {
                            interactor.onAddAddressButtonClick()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(store) { state ->
            if (!state.isLoading && state.addresses.isEmpty()) {
                findNavController().popBackStack()
                return@consumeFrom
            }
        }
    }

    /**
     * Fetches all the addresses from the autofill storage and updates the
     * [AutofillFragmentStore] with the list of addresses.
     */
    private fun loadAddresses() {
        lifecycleScope.launch {
            val addresses = requireContext().components.core.autofillStorage.getAllAddresses()

            lifecycleScope.launch(Dispatchers.Main) {
                store.dispatch(AutofillAction.UpdateAddresses(addresses))
            }
        }
    }
}
