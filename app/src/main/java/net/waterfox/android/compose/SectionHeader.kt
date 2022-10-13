/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import net.waterfox.android.theme.WaterfoxTheme

/**
 * Default layout for the header of a screen section.
 *
 * @param text [String] to be styled as header and displayed.
 * @param modifier [Modifier] to be applied to the [Text].
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = WaterfoxTheme.colors.textPrimary,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        style = WaterfoxTheme.typography.headline7
    )
}

@Composable
@Preview
private fun HeadingTextPreview() {
    SectionHeader(text = "Section title")
}
