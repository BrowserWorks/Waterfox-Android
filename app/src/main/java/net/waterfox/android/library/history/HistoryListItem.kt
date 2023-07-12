/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.waterfox.android.R
import net.waterfox.android.library.LibrarySiteItem
import net.waterfox.android.selection.SelectionHolder
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.utils.Do

@Composable
fun HistoryListItem(
    item: History,
    timeGroup: HistoryItemTimeGroup?,
    mode: HistoryFragmentState.Mode,
    isPendingDeletion: Boolean,
    groupPendingDeletionCount: Int,
    holder: SelectionHolder<History>,
    interactor: HistoryInteractor,
) {
    val subtitleText = Do exhaustive when (item) {
        is History.Regular -> item.url
        is History.Metadata -> item.url
        is History.Group -> {
            val numChildren = item.items.size - groupPendingDeletionCount
            val stringId = if (numChildren == 1) {
                R.string.history_search_group_site
            } else {
                R.string.history_search_group_sites
            }
            String.format(LocalContext.current.getString(stringId), numChildren)
        }
    }

    val shouldHideMenu = mode is HistoryFragmentState.Mode.Editing
    val menuIcon = if (shouldHideMenu) null else R.drawable.ic_close
    var onMenuClick: ((History) -> Unit)? = { interactor.onDeleteSome(setOf(item)) }
    onMenuClick = if (shouldHideMenu) null else onMenuClick

    Column {
        if (timeGroup != null) {
            Text(
                timeGroup.humanReadable(LocalContext.current),
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 32.dp,
                    bottom = 8.dp,
                ),
                color = WaterfoxTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
        }

        if (!isPendingDeletion) {
            LibrarySiteItem(
                favicon = if (item is History.Group) R.drawable.ic_multiple_tabs else null,
                title = item.title,
                url = subtitleText,
                selected = item in holder.selectedItems,
                menuIcon = menuIcon,
                menuContentDescription = stringResource(R.string.history_delete_item),
                onMenuClick = onMenuClick,
                item = item,
                holder = holder,
                interactor = interactor,
            )
        }
    }
}
