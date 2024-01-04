/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import net.waterfox.android.R
import net.waterfox.android.compose.Favicon
import net.waterfox.android.selection.SelectionHolder
import net.waterfox.android.selection.SelectionInteractor
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun <T> LibrarySiteItem(
    modifier: Modifier = Modifier,
    @DrawableRes favicon: Int? = null,
    title: String?,
    url: String? = null,
    selected: Boolean = false,
    menuItems: List<LibrarySiteItemMenuItem>? = null,
    menuIcon: Int? = null,
    menuContentDescription: String? = null,
    onMenuClick: ((T) -> Unit)? = null,
    item: T,
    holder: SelectionHolder<T>? = null,
    interactor: SelectionInteractor<T>? = null,
    onItemClick: ((T) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = dimensionResource(R.dimen.library_item_height))
            .combinedClickable(
                onClick = {
                    onItemClick?.invoke(item)

                    holder?.let {
                        when {
                            holder.selectedItems.isEmpty() -> interactor?.open(item)
                            item in holder.selectedItems -> interactor?.deselect(item)
                            else -> interactor?.select(item)
                        }
                    }
                },
                onLongClick = {
                    if (holder != null && holder.selectedItems.isEmpty()) {
                        interactor?.select(item)
                    }
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = WaterfoxTheme.colors.textSecondary),
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                holder?.let {
                    if (item in holder.selectedItems) {
                        interactor?.deselect(item)
                    } else {
                        interactor?.select(item)
                    }
                }
            },
            modifier = Modifier.size(dimensionResource(R.dimen.history_favicon_width_height)),
        ) {
            AnimatedVisibility(
                selected,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when (isSystemInDarkTheme()) {
                                true -> Color(0xFF00FEFF)
                                false -> WaterfoxTheme.colors.iconButton
                            },
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mozac_ic_checkmark_24),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .semantics {
                                testTagsAsResourceId = true
                                testTag = "library.site.item.checkmark"
                            },
                        tint = colorResource(R.color.mozac_ui_icons_fill),
                    )
                }
            }

            if (!selected) {
                if (favicon != null) {
                    Icon(
                        painter = painterResource(favicon),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .semantics {
                                testTagsAsResourceId = true
                                testTag = "library.site.item.favicon"
                            },
                        tint = WaterfoxTheme.colors.textPrimary,
                    )
                } else if (url != null && url.startsWith("http")) {
                    Favicon(
                        url = url,
                        size = 24.dp,
                        modifier = Modifier.semantics {
                            testTagsAsResourceId = true
                            testTag = "library.site.item.favicon"
                        },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 16.dp),
        ) {
            Text(
                if (title.isNullOrBlank()) (url ?: "") else title,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        testTagsAsResourceId = true
                        testTag = "library.site.item.title"
                    },
                fontSize = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TextStyle(color = WaterfoxTheme.colors.textPrimary),
            )

            if (url != null) {
                Text(
                    url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "library.site.item.url"
                        },
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TextStyle(color = WaterfoxTheme.colors.textSecondary),
                )
            }
        }

        if (!menuItems.isNullOrEmpty()) {
            var expanded by rememberSaveable { mutableStateOf(false) }

            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "library.site.item.overflow.menu"
                        },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.content_description_menu),
                        tint = WaterfoxTheme.colors.iconPrimary,
                    )
                }

                LibrarySiteItemMenu(
                    showMenu = expanded,
                    menuItems = menuItems,
                    onDismissRequest = { expanded = false },
                )
            }
        } else if (menuIcon != null && onMenuClick != null) {
            IconButton(
                onClick = { onMenuClick(item) },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .semantics {
                        testTagsAsResourceId = true
                        testTag = "library.site.item.trailing.icon"
                    },
            ) {
                Icon(
                    painter = painterResource(menuIcon),
                    contentDescription = menuContentDescription,
                    tint = WaterfoxTheme.colors.iconPrimary,
                )
            }
        }
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun LibrarySiteItemSitePreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        LibrarySiteItem(
            favicon = R.drawable.ic_folder_icon,
            title = "Example site",
            url = "https://example.com/",
            selected = true,
            item = BookmarkNode(
                type = BookmarkNodeType.ITEM,
                guid = "",
                parentGuid = null,
                position = null,
                title = null,
                url = null,
                dateAdded = 0L,
                children = null,
            ),
        )
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun LibrarySiteItemFolderPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        LibrarySiteItem(
            favicon = R.drawable.ic_folder_icon,
            title = "Example folder",
            item = BookmarkNode(
                type = BookmarkNodeType.FOLDER,
                guid = "",
                parentGuid = null,
                position = null,
                title = null,
                url = null,
                dateAdded = 0L,
                children = null,
            ),
        )
    }
}
