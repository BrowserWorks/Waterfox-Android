/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.historymetadata

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.UserInteractionHandler
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.addons.showSnackBar
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.components.StoreProvider
import net.waterfox.android.databinding.FragmentHistoryMetadataGroupBinding
import net.waterfox.android.ext.nav
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.setTextColor
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.ext.components
import net.waterfox.android.ext.runIfFragmentIsAttached
import net.waterfox.android.ext.toShortUrl
import net.waterfox.android.library.LibraryPageFragment
import net.waterfox.android.library.history.History
import net.waterfox.android.library.historymetadata.controller.DefaultHistoryMetadataGroupController
import net.waterfox.android.library.historymetadata.interactor.DefaultHistoryMetadataGroupInteractor
import net.waterfox.android.library.historymetadata.interactor.HistoryMetadataGroupInteractor
import net.waterfox.android.library.historymetadata.view.HistoryMetadataGroupView
import net.waterfox.android.utils.allowUndo

/**
 * Displays a list of history metadata items for a history metadata search group.
 */
@SuppressWarnings("TooManyFunctions")
class HistoryMetadataGroupFragment :
    LibraryPageFragment<History.Metadata>(), UserInteractionHandler {

    private lateinit var historyMetadataGroupStore: HistoryMetadataGroupFragmentStore
    private lateinit var interactor: HistoryMetadataGroupInteractor

    private var _historyMetadataGroupView: HistoryMetadataGroupView? = null
    private val historyMetadataGroupView: HistoryMetadataGroupView
        get() = _historyMetadataGroupView!!
    private var _binding: FragmentHistoryMetadataGroupBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<HistoryMetadataGroupFragmentArgs>()

    override val selectedItems: Set<History.Metadata>
        get() = historyMetadataGroupStore.state.items.filter { it.selected }.toSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryMetadataGroupBinding.inflate(inflater, container, false)

        val historyItems = args.historyMetadataItems.filterIsInstance<History.Metadata>()
        historyMetadataGroupStore = StoreProvider.get(this) {
            HistoryMetadataGroupFragmentStore(
                HistoryMetadataGroupFragmentState(
                    items = historyItems,
                    pendingDeletionItems = requireContext().components.appStore.state.pendingDeletionHistoryItems,
                    isEmpty = historyItems.isEmpty()
                )
            )
        }

        interactor = DefaultHistoryMetadataGroupInteractor(
            controller = DefaultHistoryMetadataGroupController(
                historyStorage = (activity as HomeActivity).components.core.historyStorage,
                browserStore = (activity as HomeActivity).components.core.store,
                appStore = requireContext().components.appStore,
                store = historyMetadataGroupStore,
                selectOrAddUseCase = requireComponents.useCases.tabsUseCases.selectOrAddTab,
                navController = findNavController(),
                scope = CoroutineScope(Dispatchers.IO),
                searchTerm = args.title,
                deleteSnackbar = :: deleteSnackbar,
                promptDeleteAll = :: promptDeleteAll,
                allDeletedSnackbar = ::allDeletedSnackbar,
            )
        )

        _historyMetadataGroupView = HistoryMetadataGroupView(
            container = binding.historyMetadataGroupLayout,
            interactor = interactor,
            title = args.title,
            onEmptyStateChanged = {
                historyMetadataGroupStore.dispatch(
                    HistoryMetadataGroupFragmentAction.ChangeEmptyState(it)
                )
            }
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(historyMetadataGroupStore) { state ->
            historyMetadataGroupView.update(state)
            activity?.invalidateOptionsMenu()
        }

        requireContext().components.appStore.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.pendingDeletionHistoryItems }.collect { items ->
                historyMetadataGroupStore.dispatch(
                    HistoryMetadataGroupFragmentAction.UpdatePendingDeletionItems(
                        pendingDeletionItems = items
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(args.title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _historyMetadataGroupView = null
        _binding = null
    }

    override fun onBackPressed(): Boolean = interactor.onBackPressed(selectedItems)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (selectedItems.isNotEmpty()) {
            inflater.inflate(R.menu.history_select_multi, menu)

            menu.findItem(R.id.delete_history_multi_select)?.let { deleteItem ->
                deleteItem.title = SpannableString(deleteItem.title).apply {
                    setTextColor(requireContext(), R.attr.textWarning)
                }
            }
        } else {
            inflater.inflate(R.menu.history_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_history_multi_select -> {
                interactor.onShareMenuItem(selectedItems)
                true
            }
            R.id.delete_history_multi_select -> {
                interactor.onDelete(selectedItems)
                true
            }
            R.id.open_history_in_new_tabs_multi_select -> {
                openItemsInNewTab { selectedItem ->
                    selectedItem.url
                }

                showTabTray()
                true
            }
            R.id.open_history_in_private_tabs_multi_select -> {
                openItemsInNewTab(private = true) { selectedItem ->
                    selectedItem.url
                }

                (activity as HomeActivity).apply {
                    browsingModeManager.mode = BrowsingMode.Private
                    supportActionBar?.hide()
                }

                showTabTray()
                true
            }
            R.id.history_delete -> {
                interactor.onDeleteAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteSnackbar(
        items: Set<History.Metadata>,
        undo: suspend (items: Set<History.Metadata>) -> Unit,
        delete: (Set<History.Metadata>) -> suspend (context: Context) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).allowUndo(
            requireView(),
            getSnackBarMessage(items),
            getString(R.string.snackbar_deleted_undo),
            {
                undo.invoke(items)
            },
            delete(items)
        )
    }

    private fun promptDeleteAll() {
        if (childFragmentManager.findFragmentByTag(DeleteAllConfirmationDialogFragment.TAG)
            as? DeleteAllConfirmationDialogFragment != null
        ) {
            return
        }

        DeleteAllConfirmationDialogFragment(interactor, args.title).show(
            childFragmentManager, DeleteAllConfirmationDialogFragment.TAG
        )
    }

    private fun allDeletedSnackbar() {
        runIfFragmentIsAttached {
            showSnackBar(
                binding.root,
                getString(R.string.delete_history_group_snackbar)
            )
        }
    }

    private fun showTabTray() {
        findNavController().nav(
            R.id.historyMetadataGroupFragment,
            HistoryMetadataGroupFragmentDirections.actionGlobalTabsTrayFragment()
        )
    }

    private fun getSnackBarMessage(historyItems: Set<History.Metadata>): String {
        val historyItem = historyItems.first()
        return String.format(
            requireContext().getString(R.string.history_delete_single_item_snackbar),
            historyItem.url.toShortUrl(requireComponents.publicSuffixList)
        )
    }

    internal class DeleteAllConfirmationDialogFragment(
        private val interactor: HistoryMetadataGroupInteractor,
        private val groupName: String
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(requireContext())
                .setMessage(
                    String.format(
                        getString(R.string.delete_all_history_group_prompt_message),
                        groupName
                    )
                )
                .setNegativeButton(R.string.delete_history_group_prompt_cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(R.string.delete_history_group_prompt_allow) { dialog: DialogInterface, _ ->
                    interactor.onDeleteAllConfirmed()
                    dialog.dismiss()
                }
                .create()

        companion object {
            const val TAG = "DELETE_CONFIRMATION_DIALOG_FRAGMENT"
        }
    }
}
