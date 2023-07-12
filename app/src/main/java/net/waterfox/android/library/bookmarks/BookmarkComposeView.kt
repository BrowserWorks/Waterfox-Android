/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.bookmarks

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.base.feature.UserInteractionHandler
import net.waterfox.android.R
import net.waterfox.android.library.LibrarySiteItem
import net.waterfox.android.library.LibrarySiteItemMenuItem
import net.waterfox.android.selection.SelectionInteractor
import net.waterfox.android.theme.WaterfoxTheme

/**
 * Interface for the Bookmarks view.
 * This interface is implemented by objects that want to respond to user interaction on the bookmarks management UI.
 */
@SuppressWarnings("TooManyFunctions")
interface BookmarkViewInteractor : SelectionInteractor<BookmarkNode> {

    /**
     * Swaps the head of the bookmarks tree, replacing it with a new, updated bookmarks tree.
     *
     * @param node the head node of the new bookmarks tree
     */
    fun onBookmarksChanged(node: BookmarkNode)

    /**
     * Switches the current bookmark multi-selection mode.
     *
     * @param mode the multi-select mode to switch to
     */
    fun onSelectionModeSwitch(mode: BookmarkFragmentState.Mode)

    /**
     * Opens up an interface to edit a bookmark node.
     *
     * @param node the bookmark node to edit
     */
    fun onEditPressed(node: BookmarkNode)

    /**
     * De-selects all bookmark nodes, clearing the multi-selection mode.
     *
     */
    fun onAllBookmarksDeselected()

    /**
     * Copies the URL of a bookmark item to the copy-paste buffer.
     *
     * @param item the bookmark item to copy the URL from
     */
    fun onCopyPressed(item: BookmarkNode)

    /**
     * Opens the share sheet for a bookmark item.
     *
     * @param item the bookmark item to share
     */
    fun onSharePressed(item: BookmarkNode)

    /**
     * Opens a bookmark item in a new tab.
     *
     * @param item the bookmark item to open in a new tab
     */
    fun onOpenInNormalTab(item: BookmarkNode)

    /**
     * Opens a bookmark item in a private tab.
     *
     * @param item the bookmark item to open in a private tab
     */
    fun onOpenInPrivateTab(item: BookmarkNode)

    /**
     * Deletes a set of bookmark nodes.
     *
     * @param nodes the bookmark nodes to delete
     */
    fun onDelete(nodes: Set<BookmarkNode>)

    /**
     * Handles back presses for the bookmark screen, so navigation up the tree is possible.
     *
     */
    fun onBackPressed()

    /**
     * Handles user requested sync of bookmarks.
     *
     */
    fun onRequestSync()

    /**
     * Handles when search is tapped
     */
    fun onSearch()
}

class BookmarkComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr), UserInteractionHandler {

    var bookmarkList by mutableStateOf<List<BookmarkNode>>(emptyList())
    var mode by mutableStateOf<BookmarkFragmentState.Mode>(BookmarkFragmentState.Mode.Normal())
    var progressIndicatorVisible by mutableStateOf(true)
    var swipeRefreshEnabled by mutableStateOf(true)
    var swipeRefreshRefreshing by mutableStateOf(false)
    var signInButtonVisible by mutableStateOf(false)
    var onSignInButtonClick by mutableStateOf({})

    lateinit var interactor: BookmarkViewInteractor

    fun updateState(state: BookmarkFragmentState) {
        if (state.mode != mode) {
            mode = state.mode
            if (mode is BookmarkFragmentState.Mode.Normal || mode is BookmarkFragmentState.Mode.Selecting) {
                interactor.onSelectionModeSwitch(mode)
            }
        }

        updateBookmarkList(state.tree)

        progressIndicatorVisible = state.isLoading
        swipeRefreshEnabled =
            mode is BookmarkFragmentState.Mode.Normal || mode is BookmarkFragmentState.Mode.Syncing
        swipeRefreshRefreshing = mode is BookmarkFragmentState.Mode.Syncing
    }

    private fun updateBookmarkList(tree: BookmarkNode?) {
        val allNodes = tree?.children.orEmpty()
        val folders = mutableListOf<BookmarkNode>()
        val notFolders = mutableListOf<BookmarkNode>()
        val separators = mutableListOf<BookmarkNode>()
        allNodes.forEach {
            when (it.type) {
                BookmarkNodeType.SEPARATOR -> separators.add(it)
                BookmarkNodeType.FOLDER -> folders.add(it)
                else -> notFolders.add(it)
            }
        }

        bookmarkList = folders + notFolders - separators
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        WaterfoxTheme {
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = swipeRefreshRefreshing)
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { interactor.onRequestSync() },
                modifier = Modifier.fillMaxSize(),
                swipeEnabled = swipeRefreshEnabled,
            ) {
                LazyColumn(
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                        testTag = "bookmark.list"
                    },
                    contentPadding = PaddingValues(top = 8.dp),
                ) {
                    items(
                        items = bookmarkList,
                        key = { bookmark -> bookmark.guid },
                    ) { bookmark ->
                        val shouldHideMenu =
                            (bookmark.type == BookmarkNodeType.FOLDER && bookmark.inRoots()) ||
                                    mode is BookmarkFragmentState.Mode.Selecting
                        val menuItems = if (shouldHideMenu) null else getMenuItems(
                            bookmark = bookmark,
                            onEditClick = interactor::onEditPressed,
                            onCopyClick = interactor::onCopyPressed,
                            onShareClick = interactor::onSharePressed,
                            onOpenInNormalTabClick = interactor::onOpenInNormalTab,
                            onOpenInPrivateTabClick = interactor::onOpenInPrivateTab,
                            onDeleteClick = interactor::onDelete,
                        )

                        LibrarySiteItem(
                            favicon = if (bookmark.type == BookmarkNodeType.FOLDER) R.drawable.ic_folder_icon else null,
                            title = bookmark.title,
                            url = bookmark.url,
                            selected = bookmark in mode.selectedItems,
                            menuItems = menuItems,
                            item = bookmark,
                            holder = mode,
                            interactor = interactor,
                        )
                    }

                    if (signInButtonVisible) {
                        item { SignInButton() }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bookmarkList.isEmpty()) {
                        EmptyView()
                    }

                    if (progressIndicatorVisible) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    private fun getMenuItems(
        bookmark: BookmarkNode,
        onEditClick: (BookmarkNode) -> Unit,
        onCopyClick: (BookmarkNode) -> Unit,
        onShareClick: (BookmarkNode) -> Unit,
        onOpenInNormalTabClick: (BookmarkNode) -> Unit,
        onOpenInPrivateTabClick: (BookmarkNode) -> Unit,
        onDeleteClick: (Set<BookmarkNode>) -> Unit,
    ): List<LibrarySiteItemMenuItem> {
        return listOfNotNull(
            if (bookmark.type != BookmarkNodeType.SEPARATOR) {
                LibrarySiteItemMenuItem(
                    title = stringResource(R.string.bookmark_menu_edit_button),
                    color = WaterfoxTheme.colors.textPrimary,
                ) {
                    onEditClick(bookmark)
                }
            } else {
                null
            },
            if (bookmark.type == BookmarkNodeType.ITEM) {
                LibrarySiteItemMenuItem(
                    title = stringResource(R.string.bookmark_menu_copy_button),
                    color = WaterfoxTheme.colors.textPrimary,
                ) {
                    onCopyClick(bookmark)
                }
            } else {
                null
            },
            if (bookmark.type == BookmarkNodeType.ITEM) {
                LibrarySiteItemMenuItem(
                    title = stringResource(R.string.bookmark_menu_share_button),
                    color = WaterfoxTheme.colors.textPrimary,
                ) {
                    onShareClick(bookmark)
                }
            } else {
                null
            },
            if (bookmark.type == BookmarkNodeType.ITEM) {
                LibrarySiteItemMenuItem(
                    title = stringResource(R.string.bookmark_menu_open_in_new_tab_button),
                    color = WaterfoxTheme.colors.textPrimary,
                ) {
                    onOpenInNormalTabClick(bookmark)
                }
            } else {
                null
            },
            if (bookmark.type == BookmarkNodeType.ITEM) {
                LibrarySiteItemMenuItem(
                    title = stringResource(R.string.bookmark_menu_open_in_private_tab_button),
                    color = WaterfoxTheme.colors.textPrimary,
                ) {
                    onOpenInPrivateTabClick(bookmark)
                }
            } else {
                null
            },
            LibrarySiteItemMenuItem(
                title = stringResource(R.string.bookmark_menu_delete_button),
                color = WaterfoxTheme.colors.textWarning,
            ) {
                onDeleteClick(setOf(bookmark))
            },
        )
    }

    @Composable
    private fun SignInButton() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onSignInButtonClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.fx_mobile_action_color_secondary),
                    contentColor = colorResource(R.color.fx_mobile_text_color_action_secondary),
                ),
            ) {
                Text(
                    stringResource(R.string.bookmark_sign_in_button),
                    fontWeight = FontWeight(integerResource(R.integer.font_weight_medium)),
                    letterSpacing = 0.sp,
                )
            }
        }
    }

    @Composable
    private fun EmptyView() {
        Text(
            stringResource(R.string.bookmarks_empty_message),
            color = WaterfoxTheme.colors.textSecondary,
            fontSize = 16.sp,
        )
    }

    override fun onBackPressed(): Boolean {
        return when (mode) {
            is BookmarkFragmentState.Mode.Selecting -> {
                interactor.onAllBookmarksDeselected()
                true
            }
            else -> {
                interactor.onBackPressed()
                true
            }
        }
    }

}

fun BookmarkNode.inRoots() = enumValues<BookmarkRoot>().any { it.id == guid }
