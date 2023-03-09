/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.about

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.ui.colors.PhotonColors
import net.waterfox.android.R
import net.waterfox.android.theme.ThemeManager
import net.waterfox.android.theme.WaterfoxTheme

class AboutComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var producedByText by mutableStateOf("")
    var buildVersionsText by mutableStateOf("")
    var buildDateText by mutableStateOf("")
    var pageItems by mutableStateOf<List<AboutPageItem>>(emptyList())
    var onPageItemClick by mutableStateOf<(AboutItem) -> Unit>({})
    var onLogoClick by mutableStateOf({})

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        WaterfoxTheme {
            LazyColumn(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                    testTag = "about.list"
                }
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(36.dp))

                        Image(
                            painter = painterResource(
                                when (isSystemInDarkTheme()) {
                                    true -> R.drawable.ic_logo_wordmark_private
                                    false -> R.drawable.ic_logo_wordmark_normal
                                }
                            ),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { onLogoClick() },
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = producedByText,
                            modifier = Modifier
                                .padding(horizontal = dimensionResource(R.dimen.about_header_title_padding_start_end)),
                            color = WaterfoxTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = buildVersionsText,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = WaterfoxTheme.colors.textPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = buildDateText,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .semantics {
                                    testTagsAsResourceId = true
                                    testTag = "about.build.date"
                                },
                            color = WaterfoxTheme.colors.textPrimary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(36.dp))

                        Divider()
                    }
                }

                items(
                    items = pageItems,
                    key = { item -> item.title },
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { onPageItemClick(item.type) },
                    ) {
                        Text(
                            text = item.title,
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF008EA4),
                        )
                    }
                    Divider()
                }
            }
        }
    }

}
