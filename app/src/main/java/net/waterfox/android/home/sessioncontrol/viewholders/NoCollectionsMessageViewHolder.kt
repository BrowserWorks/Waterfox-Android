/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import kotlinx.coroutines.flow.distinctUntilChanged
import net.waterfox.android.R
import net.waterfox.android.databinding.NoCollectionsMessageBinding
import net.waterfox.android.ext.increaseTapArea
import net.waterfox.android.home.sessioncontrol.CollectionInteractor
import net.waterfox.android.utils.view.ViewHolder

@OptIn(ExperimentalCoroutinesApi::class)
open class NoCollectionsMessageViewHolder(
    view: View,
    viewLifecycleOwner: LifecycleOwner,
    store: BrowserStore,
    interactor: CollectionInteractor
) : ViewHolder(view) {

    init {
        val binding = NoCollectionsMessageBinding.bind(view)

        binding.addTabsToCollectionsButton.apply {

            setOnClickListener {
                interactor.onAddTabsToCollectionTapped()
            }
            isVisible = store.state.normalTabs.isNotEmpty()
        }

        binding.removeCollectionPlaceholder.apply {
            increaseTapArea(
                view.resources.getDimensionPixelSize(R.dimen.tap_increase_16)
            )
            setOnClickListener {
                interactor.onRemoveCollectionsPlaceholder()
            }
        }

        store.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.normalTabs.size }
                .distinctUntilChanged()
                .collect { tabs ->
                    binding.addTabsToCollectionsButton.isVisible = tabs > 0
                }
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.no_collections_message
    }
}
