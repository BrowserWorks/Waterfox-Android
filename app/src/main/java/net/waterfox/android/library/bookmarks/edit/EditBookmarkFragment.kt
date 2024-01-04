/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.bookmarks.edit

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.uniffi.PlacesApiException
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.ktx.android.content.getColorFromAttr
import net.waterfox.android.NavHostActivity
import net.waterfox.android.R
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.databinding.FragmentEditBookmarkBinding
import net.waterfox.android.ext.*
import net.waterfox.android.library.bookmarks.BookmarksSharedViewModel
import net.waterfox.android.library.bookmarks.friendlyRootTitle

/**
 * Menu to edit the name, URL, and location of a bookmark item.
 */
class EditBookmarkFragment : Fragment(R.layout.fragment_edit_bookmark) {
    private var _binding: FragmentEditBookmarkBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<EditBookmarkFragmentArgs>()
    private val sharedViewModel: BookmarksSharedViewModel by activityViewModels()
    private var bookmarkNode: BookmarkNode? = null
    private var bookmarkParent: BookmarkNode? = null
    private var initialParentGuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditBookmarkBinding.bind(view)

        initToolbar()

        viewLifecycleOwner.lifecycleScope.launch(Main) {
            val context = requireContext()
            val bookmarkNodeBeforeReload = bookmarkNode
            val bookmarksStorage = context.components.core.bookmarksStorage

            bookmarkNode = withContext(IO) {
                bookmarksStorage.getBookmark(args.guidToEdit)
            }

            if (initialParentGuid == null) {
                initialParentGuid = bookmarkNode?.parentGuid
            }

            bookmarkParent = withContext(IO) {
                // Use user-selected parent folder if it's set, or node's current parent otherwise.
                if (sharedViewModel.selectedFolder != null) {
                    sharedViewModel.selectedFolder
                } else {
                    bookmarkNode?.parentGuid?.let { bookmarksStorage.getBookmark(it) }
                }
            }

            when (bookmarkNode?.type) {
                BookmarkNodeType.FOLDER -> {
                    activity?.title = getString(R.string.edit_bookmark_folder_fragment_title)
                    binding.bookmarkContent.bookmarkUrlVisible = false
                }
                BookmarkNodeType.ITEM -> {
                    activity?.title = getString(R.string.edit_bookmark_fragment_title)
                }
                else -> throw IllegalArgumentException()
            }

            val currentBookmarkNode = bookmarkNode
            if (currentBookmarkNode != null && currentBookmarkNode != bookmarkNodeBeforeReload) {
                binding.bookmarkContent.bookmarkName = currentBookmarkNode.title
                binding.bookmarkContent.bookmarkUrl = currentBookmarkNode.url
            }

            bookmarkParent?.let { node ->
                binding.bookmarkContent.bookmarkParentFolder = friendlyRootTitle(context, node)
            }

            binding.bookmarkContent.onBookmarkParentFolderClick = {
                sharedViewModel.selectedFolder = null
                nav(
                    R.id.bookmarkEditFragment,
                    EditBookmarkFragmentDirections
                        .actionBookmarkEditFragmentToBookmarkSelectFolderFragment(
                            allowCreatingNewFolder = false,
                            // Don't allow moving folders into themselves.
                            hideFolderGuid = when (bookmarkNode!!.type) {
                                BookmarkNodeType.FOLDER -> bookmarkNode!!.guid
                                else -> null
                            }
                        )
                )
            }
        }
    }

    private fun initToolbar() {
        val activity = activity as AppCompatActivity
        val actionBar = (activity as NavHostActivity).getSupportActionBarAndInflateIfNecessary()
        val toolbar = activity.findViewById<Toolbar>(R.id.navigationToolbar)
        toolbar?.setToolbarColors(
            foreground = activity.getColorFromAttr(R.attr.textPrimary),
            background = activity.getColorFromAttr(R.attr.layer1)
        )
        actionBar.show()
    }

    override fun onPause() {
        super.onPause()
        binding.bookmarkContent.hideKeyboard()
        binding.bookmarkContent.progressBarVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmarks_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_bookmark_button -> {
                displayDeleteBookmarkDialog()
                true
            }
            R.id.save_bookmark_button -> {
                updateBookmarkFromTextChanges()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayDeleteBookmarkDialog() {
        activity?.let { activity ->
            AlertDialog.Builder(activity).apply {
                setMessage(R.string.bookmark_deletion_confirmation)
                setNegativeButton(R.string.bookmark_delete_negative) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.tab_collection_dialog_positive) { dialog: DialogInterface, _ ->
                    // Use fragment's lifecycle; the view may be gone by the time dialog is interacted with.
                    lifecycleScope.launch(IO) {
                        requireComponents.core.bookmarksStorage.deleteNode(args.guidToEdit)

                        launch(Main) {
                            Navigation.findNavController(requireActivity(), R.id.container)
                                .popBackStack()

                            bookmarkNode?.let { bookmark ->
                                WaterfoxSnackbar.make(
                                    view = activity.getRootView()!!,
                                    isDisplayedWithBrowserToolbar = args.requiresSnackbarPaddingForToolbar
                                )
                                    .setText(
                                        getString(
                                            R.string.bookmark_deletion_snackbar_message,
                                            bookmark.url?.toShortUrl(context.components.publicSuffixList)
                                                ?: bookmark.title
                                        )
                                    )
                                    .show()
                            }
                        }
                    }
                    dialog.dismiss()
                }
                create()
            }.show()
        }
    }

    private fun updateBookmarkFromTextChanges() {
        binding.bookmarkContent.progressBarVisible = true
        val nameText = binding.bookmarkContent.bookmarkName
        val urlText = binding.bookmarkContent.bookmarkUrl
        updateBookmarkNode(nameText, urlText)
    }

    private fun updateBookmarkNode(title: String?, url: String?) {
        viewLifecycleOwner.lifecycleScope.launch(IO) {
            try {
                requireComponents.let { components ->
                    val parentGuid = sharedViewModel.selectedFolder?.guid ?: bookmarkNode!!.parentGuid
                    val parentChanged = initialParentGuid != parentGuid
                    components.core.bookmarksStorage.updateNode(
                        args.guidToEdit,
                        BookmarkInfo(
                            parentGuid,
                            // Setting position to 'null' is treated as a 'move to the end' by the storage API.
                            if (parentChanged) null else bookmarkNode?.position,
                            title,
                            if (bookmarkNode?.type == BookmarkNodeType.ITEM) url else null
                        )
                    )
                }
                withContext(Main) {
                    binding.bookmarkContent.bookmarkUrlErrorMessage = null
                    binding.bookmarkContent.bookmarkUrlErrorDrawable = null

                    findNavController().popBackStack()
                }
            } catch (e: PlacesApiException.UrlParseFailed) {
                withContext(Main) {
                    binding.bookmarkContent.bookmarkUrlErrorMessage = getString(R.string.bookmark_invalid_url_error)
                    binding.bookmarkContent.bookmarkUrlErrorDrawable = R.drawable.mozac_ic_warning_fill_24
                }
            }
        }
        binding.bookmarkContent.progressBarVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
