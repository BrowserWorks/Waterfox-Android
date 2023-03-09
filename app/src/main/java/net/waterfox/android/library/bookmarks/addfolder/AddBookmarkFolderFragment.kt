/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.bookmarks.addfolder

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.showKeyboard
import net.waterfox.android.R
import net.waterfox.android.databinding.FragmentEditBookmarkBinding
import net.waterfox.android.ext.nav
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.library.bookmarks.BookmarksSharedViewModel
import net.waterfox.android.library.bookmarks.friendlyRootTitle

/**
 * Menu to create a new bookmark folder.
 */
class AddBookmarkFolderFragment : Fragment(R.layout.fragment_edit_bookmark) {
    private var _binding: FragmentEditBookmarkBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: BookmarksSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * Hides fields for bookmark items present in the shared layout file.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentEditBookmarkBinding.bind(view)

        binding.bookmarkContent.bookmarkUrlVisible = false
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.bookmark_add_folder_fragment_label))

        viewLifecycleOwner.lifecycleScope.launch(Main) {
            val context = requireContext()
            sharedViewModel.selectedFolder = withContext(IO) {
                sharedViewModel.selectedFolder
                    ?: requireComponents.core.bookmarksStorage.getBookmark(BookmarkRoot.Mobile.id)
            }

            binding.bookmarkContent.bookmarkParentFolder =
                friendlyRootTitle(context, sharedViewModel.selectedFolder!!)
            binding.bookmarkContent.onBookmarkParentFolderClick = {
                nav(
                    R.id.bookmarkAddFolderFragment,
                    AddBookmarkFolderFragmentDirections
                        .actionBookmarkAddFolderFragmentToBookmarkSelectFolderFragment(
                            allowCreatingNewFolder = true
                        )
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.bookmarkContent.hideKeyboard()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmarks_add_folder, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.confirm_add_folder_button -> {
                if (binding.bookmarkContent.bookmarkName.isNullOrBlank()) {
                    binding.bookmarkContent.bookmarkNameErrorMessage =
                        getString(R.string.bookmark_empty_title_error)
                    return true
                }
                binding.bookmarkContent.hideKeyboard()
                viewLifecycleOwner.lifecycleScope.launch(IO) {
                    val newGuid = requireComponents.core.bookmarksStorage.addFolder(
                        sharedViewModel.selectedFolder!!.guid,
                        binding.bookmarkContent.bookmarkName.toString(),
                        null
                    )
                    sharedViewModel.selectedFolder =
                        requireComponents.core.bookmarksStorage.getTree(newGuid)
                    withContext(Main) {
                        Navigation.findNavController(requireActivity(), R.id.container)
                            .popBackStack()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
