/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.tabstray

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import net.waterfox.android.R
import net.waterfox.android.compose.TabThumbnail
import net.waterfox.android.ext.toShortUrl
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

/**
 * List item used to display a tab that supports clicks,
 * long clicks, multiselection, and media controls.
 *
 * @param tab The given tab to be render as view a list item.
 * @param isSelected Indicates if the item should be render as selected.
 * @param multiSelectionEnabled Indicates if the item should be render with multi selection options,
 * enabled.
 * @param multiSelectionSelected Indicates if the item should be render as multi selection selected
 * option.
 * @param onCloseClick Callback to handle the click event of the close button.
 * @param onMediaClick Callback to handle when the media item is clicked.
 * @param onClick Callback to handle when item is clicked.
 * @param onLongClick Callback to handle when item is long clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("MagicNumber")
fun TabListItem(
    tab: TabSessionState,
    storage: ThumbnailStorage,
    thumbnailSize: Int,
    isSelected: Boolean = false,
    multiSelectionEnabled: Boolean = false,
    multiSelectionSelected: Boolean = false,
    onCloseClick: (tab: TabSessionState) -> Unit,
    onMediaClick: (tab: TabSessionState) -> Unit,
    onClick: (tab: TabSessionState) -> Unit,
    onLongClick: (tab: TabSessionState) -> Unit,
) {

    val contentBackgroundColor = if (isSelected) {
        WaterfoxTheme.colors.layerAccentNonOpaque
    } else {
        WaterfoxTheme.colors.layer1
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(contentBackgroundColor)
            .combinedClickable(
                onLongClick = { onLongClick(tab) },
                onClick = { onClick(tab) }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail(
            tab = tab,
            size = thumbnailSize,
            storage = storage,
            multiSelectionEnabled = multiSelectionEnabled,
            isSelected = multiSelectionSelected,
            onMediaIconClicked = { onMediaClick(it) }
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(weight = 1f)
        ) {
            Text(
                text = tab.content.title,
                fontSize = 16.sp,
                maxLines = 2,
                color = WaterfoxTheme.colors.textPrimary,
            )

            Text(
                text = tab.content.url.toShortUrl(),
                fontSize = 12.sp,
                color = WaterfoxTheme.colors.textSecondary,
            )
        }

        if (!multiSelectionEnabled) {
            IconButton(
                onClick = { onCloseClick(tab) },
                modifier = Modifier.size(size = 24.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mozac_ic_cross_20),
                    contentDescription = stringResource(
                        id = R.string.close_tab_title,
                        tab.content.title
                    ),
                    tint = WaterfoxTheme.colors.iconPrimary
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(
    tab: TabSessionState,
    size: Int,
    storage: ThumbnailStorage,
    multiSelectionEnabled: Boolean,
    isSelected: Boolean,
    onMediaIconClicked: ((TabSessionState) -> Unit)
) {
    Box {
        TabThumbnail(
            tab = tab,
            size = size,
            storage = storage,
            modifier = Modifier.size(width = 92.dp, height = 72.dp),
            contentDescription = stringResource(id = R.string.mozac_browser_tabstray_open_tab),
        )

        if (isSelected) {
            Card(
                modifier = Modifier
                    .size(size = 40.dp)
                    .align(alignment = Alignment.Center),
                shape = CircleShape,
                backgroundColor = WaterfoxTheme.colors.layerAccent,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mozac_ic_checkmark_24),
                    modifier = Modifier
                        .matchParentSize()
                        .padding(all = 8.dp),
                    contentDescription = null,
                    tint = colorResource(id = R.color.mozac_ui_icons_fill)
                )
            }
        }

        if (!multiSelectionEnabled) {
            MediaImage(
                tab = tab,
                onMediaIconClicked = onMediaIconClicked,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun TabListItemPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        TabListItem(
            tab = createTab(url = "www.mozilla.com", title = "Mozilla"),
            thumbnailSize = 108,
            storage = ThumbnailStorage(LocalContext.current),
            onCloseClick = {},
            onMediaClick = {},
            onClick = {},
            onLongClick = {},
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun SelectedTabListItemPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        TabListItem(
            tab = createTab(url = "www.mozilla.com", title = "Mozilla"),
            thumbnailSize = 108,
            storage = ThumbnailStorage(LocalContext.current),
            onCloseClick = {},
            onMediaClick = {},
            onClick = {},
            onLongClick = {},
            multiSelectionEnabled = true,
            multiSelectionSelected = true,
        )
    }
}
