/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.recentlyclosed

import android.os.Bundle
import android.text.SpannableString
import android.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.state.recover.RecoverableTab
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.components.StoreProvider
import net.waterfox.android.databinding.FragmentRecentlyClosedTabsBinding
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.setTextColor
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.library.LibraryPageFragment

@Suppress("TooManyFunctions")
class RecentlyClosedFragment : LibraryPageFragment<RecoverableTab>(), UserInteractionHandler {
    private lateinit var recentlyClosedFragmentStore: RecentlyClosedFragmentStore

    private var _binding: FragmentRecentlyClosedTabsBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentlyClosedInteractor: RecentlyClosedFragmentInteractor
    private lateinit var recentlyClosedController: RecentlyClosedController

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.library_recently_closed_tabs))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (recentlyClosedFragmentStore.state.selectedTabs.isNotEmpty()) {
            inflater.inflate(R.menu.history_select_multi, menu)
            menu.findItem(R.id.delete_history_multi_select)?.let { deleteItem ->
                deleteItem.title = SpannableString(deleteItem.title)
                    .apply { setTextColor(requireContext(), R.attr.textWarning) }
            }
        } else {
            inflater.inflate(R.menu.library_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val selectedTabs = recentlyClosedFragmentStore.state.selectedTabs

        return when (item.itemId) {
            R.id.close_history -> {
                close()
                true
            }
            R.id.share_history_multi_select -> {
                recentlyClosedController.handleShare(selectedTabs)
                true
            }
            R.id.delete_history_multi_select -> {
                recentlyClosedController.handleDelete(selectedTabs)
                true
            }
            R.id.open_history_in_new_tabs_multi_select -> {
                recentlyClosedController.handleOpen(selectedTabs, BrowsingMode.Normal)
                true
            }
            R.id.open_history_in_private_tabs_multi_select -> {
                recentlyClosedController.handleOpen(selectedTabs, BrowsingMode.Private)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentlyClosedTabsBinding.inflate(inflater, container, false)
        recentlyClosedFragmentStore = StoreProvider.get(this) {
            RecentlyClosedFragmentStore(
                RecentlyClosedFragmentState(
                    items = listOf(),
                    selectedTabs = emptySet()
                )
            )
        }
        recentlyClosedController = DefaultRecentlyClosedController(
            navController = findNavController(),
            browserStore = requireComponents.core.store,
            recentlyClosedStore = recentlyClosedFragmentStore,
            activity = activity as HomeActivity,
            tabsUseCases = requireComponents.useCases.tabsUseCases,
            recentlyClosedTabsStorage = requireComponents.core.recentlyClosedTabsStorage.value,
            lifecycleScope = lifecycleScope,
            openToBrowser = ::openItem
        )
        recentlyClosedInteractor = RecentlyClosedFragmentInteractor(recentlyClosedController)
        binding.recentlyClosedContent.interactor = recentlyClosedInteractor
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openItem(url: String, mode: BrowsingMode? = null) {
        mode?.let { (activity as HomeActivity).browsingModeManager.mode = it }

        (activity as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = true,
            from = BrowserDirection.FromRecentlyClosed
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(recentlyClosedFragmentStore) { state ->
            binding.recentlyClosedContent.updateState(state)
            if (state.selectedTabs.isEmpty()) {
                setUiForNormalMode(context?.getString(R.string.library_recently_closed_tabs))
            } else {
                setUiForSelectingMode(
                    context?.getString(
                        R.string.history_multi_select_title,
                        state.selectedTabs.size,
                    ),
                )
            }
            activity?.invalidateOptionsMenu()
        }

        requireComponents.core.store.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.closedTabs }
                .ifChanged()
                .collect { tabs ->
                    recentlyClosedFragmentStore.dispatch(
                        RecentlyClosedFragmentAction.Change(tabs)
                    )
                }
        }
    }

    override val selectedItems: Set<RecoverableTab> = setOf()

    override fun onBackPressed(): Boolean {
        return recentlyClosedController.handleBackPressed()
    }
}
