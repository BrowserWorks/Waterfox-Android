/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.preference

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.waterfox.android.R
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@Composable
fun TextOnlyPreference(
    title: String,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    key: String,
    onClick: () -> Unit,
) {
    return Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            Box(
                modifier = Modifier.width(72.dp)
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 16.dp),
                        tint = WaterfoxTheme.colors.textPrimary,
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    color = WaterfoxTheme.colors.textPrimary,
                    style = WaterfoxTheme.typography.subtitle1,
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        color = WaterfoxTheme.colors.textSecondary,
                        style = WaterfoxTheme.typography.body2,
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TextOnlyPreferencePreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        TextOnlyPreference(
            title = "Wallpapers",
            key = "pref_key_wallpapers",
            onClick = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TextOnlyPreferenceWithIconPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        TextOnlyPreference(
            title = "Exceptions",
            icon = R.drawable.ic_internet,
            key = "pref_key_login_exceptions",
            onClick = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TextOnlyPreferenceWithSummaryPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        TextOnlyPreference(
            title = "Save logins and passwords",
            summary = "Ask to save",
            key = "pref_key_save_logins_settings",
            onClick = {},
        )
    }
}
