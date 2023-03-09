/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.about

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import net.waterfox.android.compose.LinkifyText
import net.waterfox.android.theme.WaterfoxTheme

class AboutLibrariesComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var libraries by mutableStateOf<List<LibraryItem>>(emptyList())

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        WaterfoxTheme {
            LazyColumn(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                    testTag = "about.library.list"
                },
            ) {
                itemsIndexed(
                    items = libraries,
                    key = { _, library -> library.name },
                ) { index, library ->
                    var showLicenseDialog by rememberSaveable { mutableStateOf(false) }
                    if (showLicenseDialog) {
                        Dialog(
                            onDismissRequest = { showLicenseDialog = false },
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colors.surface,
                                contentColor = contentColorFor(MaterialTheme.colors.surface),
                            ) {
                                Column {
                                    Text(
                                        text = library.name,
                                        modifier = Modifier.padding(16.dp),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.subtitle1,
                                    )

                                    LazyColumn(
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp,
                                        ),
                                    ) {
                                        item {
                                            LinkifyText(
                                                text = library.license,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily(Typeface.MONOSPACE),
                                                style = MaterialTheme.typography.body2,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = library.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { showLicenseDialog = true },
                    )

                    if (index < libraries.lastIndex)
                        Divider()
                }
            }
        }
    }

}
