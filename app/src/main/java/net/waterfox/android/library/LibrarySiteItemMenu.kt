/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import net.waterfox.android.theme.WaterfoxTheme

/**
 * Menu shown for a [net.waterfox.android.library.LibrarySiteItem].
 *
 * @see [DropdownMenu]
 *
 * @param showMenu Whether this is currently open and visible to the user.
 * @param menuItems List of options shown.
 * @param onDismissRequest Called when the user chooses a menu option or requests to dismiss the menu.
 */
@Composable
fun LibrarySiteItemMenu(
    showMenu: Boolean,
    menuItems: List<LibrarySiteItemMenuItem>,
    onDismissRequest: () -> Unit,
) {
    DisposableEffect(LocalConfiguration.current.orientation) {
        onDispose { onDismissRequest() }
    }

    MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(8.dp))) {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.background(color = WaterfoxTheme.colors.layer2),
        ) {
            for (item in menuItems) {
                DropdownMenuItem(
                    onClick = {
                        onDismissRequest()
                        item.onClick()
                    },
                ) {
                    Text(
                        text = item.title,
                        color = item.color,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

@Immutable
data class LibrarySiteItemMenuItem(
    val title: String,
    val color: Color,
    val onClick: () -> Unit,
)
