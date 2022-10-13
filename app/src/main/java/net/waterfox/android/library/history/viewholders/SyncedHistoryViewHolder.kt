/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.history.viewholders

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.waterfox.android.R
import net.waterfox.android.databinding.SyncedHistoryNavItemBinding
import net.waterfox.android.library.history.HistoryAdapter
import net.waterfox.android.library.history.HistoryInteractor
import net.waterfox.android.library.history.HistoryViewItem

/**
 * A view representing a synced history button in the history list.
 * [HistoryAdapter] is responsible for creating and populating the view.
 *
 * @param view that is passed down to the parent's constructor.
 * @param historyInteractor Handles a click even on the item.
 */
class SyncedHistoryViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor
) : RecyclerView.ViewHolder(view) {

    private val binding = SyncedHistoryNavItemBinding.bind(view)

    init {
        binding.root.setOnClickListener {
            historyInteractor.onSyncedHistoryClicked()
        }
        binding.syncedHistoryNav.isVisible = true
    }

    /**
     * Binds data to the view.
     *
     * @param item Data associated with the view.
     */
    fun bind(item: HistoryViewItem.SyncedHistoryItem) {
        binding.syncedHistoryHeader.text = item.title
    }

    companion object {
        const val LAYOUT_ID = R.layout.synced_history_nav_item
    }
}
