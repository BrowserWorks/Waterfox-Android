/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.downloads

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import mozilla.components.browser.state.state.BrowserState
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.UserInteractionHandler
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.components.StoreProvider
import net.waterfox.android.databinding.FragmentDownloadsBinding
import net.waterfox.android.ext.components
import net.waterfox.android.ext.filterNotExistsOnDisk
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.setTextColor
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.ext.getRootView
import net.waterfox.android.library.LibraryPageFragment
import net.waterfox.android.utils.allowUndo

@SuppressWarnings("TooManyFunctions", "LargeClass")
class DownloadFragment : LibraryPageFragment<DownloadItem>(), UserInteractionHandler {
    private lateinit var downloadStore: DownloadFragmentStore
    private lateinit var downloadInteractor: DownloadInteractor

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)

        val items = provideDownloads(requireComponents.core.store.state)

        downloadStore = StoreProvider.get(this) {
            DownloadFragmentStore(
                DownloadFragmentState(
                    items = items,
                    mode = DownloadFragmentState.Mode.Normal,
                    pendingDeletionIds = emptySet(),
                    isDeletingItems = false
                )
            )
        }
        val downloadController: DownloadController = DefaultDownloadController(
            downloadStore,
            ::openItem,
            ::invalidateOptionsMenu,
            ::deleteDownloadItems
        )
        downloadInteractor = DownloadInteractor(downloadController)
        binding.downloadContent.interactor = downloadInteractor

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Returns a list of available downloads to be displayed to the user.
     * Downloads must be COMPLETED and existent on disk.
     */
    @VisibleForTesting
    internal fun provideDownloads(state: BrowserState): List<DownloadItem> {
        return state.downloads.values
            .distinctBy { it.fileName }
            .sortedByDescending { it.createdTime } // sort from newest to oldest
            .map {
                DownloadItem(
                    id = it.id,
                    url = it.url,
                    fileName = it.fileName,
                    filePath = it.filePath,
                    size = it.contentLength?.toString() ?: "0",
                    contentType = it.contentType,
                    status = it.status
                )
            }.filter {
                it.status == DownloadState.Status.COMPLETED
            }.filterNotExistsOnDisk()
    }

    override val selectedItems get() = downloadStore.state.mode.selectedItems

    private fun invalidateOptionsMenu() {
        activity?.invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Schedules [items] for deletion.
     * Note: When tapping on a download item's "trash" button
     * (itemView.overflow_menu) this [items].size() will be 1.
     */
    private fun deleteDownloadItems(items: Set<DownloadItem>) {
        updatePendingDownloadToDelete(items)
        MainScope().allowUndo(
            requireActivity().getRootView()!!,
            getMultiSelectSnackBarMessage(items),
            getString(R.string.bookmark_undo_deletion),
            onCancel = {
                undoPendingDeletion(items)
            },
            operation = getDeleteDownloadItemsOperation(items)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFrom(downloadStore) { state ->
            binding.downloadContent.updateState(state)
            when (state.mode) {
                is DownloadFragmentState.Mode.Normal -> setUiForNormalMode(
                    context?.getString(R.string.library_downloads)
                )
                is DownloadFragmentState.Mode.Editing -> setUiForSelectingMode(
                    context?.getString(
                        R.string.download_multi_select_title,
                        state.mode.selectedItems.size,
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.library_downloads))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val menuRes = when (downloadStore.state.mode) {
            is DownloadFragmentState.Mode.Normal -> R.menu.library_menu
            is DownloadFragmentState.Mode.Editing -> R.menu.download_select_multi
        }
        inflater.inflate(menuRes, menu)

        menu.findItem(R.id.delete_downloads_multi_select)?.title =
            SpannableString(getString(R.string.download_delete_item_1)).apply {
                setTextColor(requireContext(), R.attr.textWarning)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.close_history -> {
            close()
            true
        }

        R.id.delete_downloads_multi_select -> {
            deleteDownloadItems(downloadStore.state.mode.selectedItems)
            downloadStore.dispatch(DownloadFragmentAction.ExitEditMode)
            true
        }

        R.id.select_all_downloads_multi_select -> {
            for (items in downloadStore.state.items) {
                downloadInteractor.select(items)
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /**
     * Provides a message to the Undo snackbar.
     */
    private fun getMultiSelectSnackBarMessage(downloadItems: Set<DownloadItem>): String {
        return if (downloadItems.size > 1) {
            getString(R.string.download_delete_multiple_items_snackbar_1)
        } else {
            String.format(
                requireContext().getString(
                    R.string.download_delete_single_item_snackbar
                ),
                downloadItems.first().fileName
            )
        }
    }

    override fun onBackPressed(): Boolean {
        return binding.downloadContent.onBackPressed()
    }

    private fun openItem(item: DownloadItem, mode: BrowsingMode? = null) {

        mode?.let { (activity as HomeActivity).browsingModeManager.mode = it }
        context?.let {
            val contentLength = if (item.size.isNotEmpty()) {
                item.size.toLong()
            } else {
                0L
            }
            AbstractFetchDownloadService.openFile(
                applicationContext = it.applicationContext,
                download = DownloadState(
                    id = item.id,
                    url = item.url,
                    fileName = item.fileName,
                    contentType = item.contentType,
                    status = item.status,
                    contentLength = contentLength
                )
            )
        }
    }

    private fun getDeleteDownloadItemsOperation(
        items: Set<DownloadItem>
    ): (suspend (context: Context) -> Unit) {
        return { context ->
            CoroutineScope(IO).launch {
                downloadStore.dispatch(DownloadFragmentAction.EnterDeletionMode)
                context.let {
                    for (item in items) {
                        it.components.useCases.downloadUseCases.removeDownload(item.id)
                    }
                }
                downloadStore.dispatch(DownloadFragmentAction.ExitDeletionMode)
            }
        }
    }

    private fun updatePendingDownloadToDelete(items: Set<DownloadItem>) {
        val ids = items.map { item -> item.id }.toSet()
        downloadStore.dispatch(DownloadFragmentAction.AddPendingDeletionSet(ids))
    }

    private fun undoPendingDeletion(items: Set<DownloadItem>) {
        val ids = items.map { item -> item.id }.toSet()
        downloadStore.dispatch(DownloadFragmentAction.UndoPendingDeletionSet(ids))
    }
}
