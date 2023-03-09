/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.preference

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.waterfox.android.compose.Divider
import net.waterfox.android.ext.readBooleanPreference
import net.waterfox.android.ext.writeBooleanPreference
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@Composable
fun SwitchPreference(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    onChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    allowDividerAbove: Boolean = false,
    iconSpaceReserved: Boolean = true,
) {
    val context = LocalContext.current
    val (checked, setChecked) = remember {
        mutableStateOf(context.readBooleanPreference(key, defaultValue))
    }

    val stored = context.readBooleanPreference(key, defaultValue)
    if (stored != checked) {
        setChecked(stored)
    }
    return SwitchPreference(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = { value ->
            context.writeBooleanPreference(key, value)
            onChange?.invoke(value)
            setChecked(value)
        },
        enabled = enabled,
        allowDividerAbove = allowDividerAbove,
        iconSpaceReserved = iconSpaceReserved,
    )
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    allowDividerAbove: Boolean = false,
    iconSpaceReserved: Boolean = true,
) {
    return Column {
        if (allowDividerAbove) {
            Divider()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = 8.dp)
                .toggleable(
                    value = checked,
                    onValueChange = if (enabled) onCheckedChange else { _ -> },
                    role = Role.Switch,
                )
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (iconSpaceReserved) 72.dp else 16.dp,
                        end = 8.dp,
                    ),
            ) {
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

            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(end = 16.dp),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = WaterfoxTheme.colors.formSurface,
                    checkedThumbColor = WaterfoxTheme.colors.formSelected,
                    uncheckedTrackColor = WaterfoxTheme.colors.formDefault,
                    uncheckedThumbColor = WaterfoxTheme.colors.indicatorInactive,
                ),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SwitchPreferenceOnPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        SwitchPreference(
            title = "Tabs you haven't viewed for two weeks get moved to the inactive section.",
            checked = true,
            onCheckedChange = {},
            enabled = true,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SwitchPreferenceOffPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        SwitchPreference(
            title = "Autofill in Waterfox",
            summary = "Fill and save usernames and passwords in websites while using Waterfox.",
            checked = false,
            onCheckedChange = {},
            enabled = true,
        )
    }
}
