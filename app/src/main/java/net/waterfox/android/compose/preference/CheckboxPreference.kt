/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.preference

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
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
import net.waterfox.android.ext.readBooleanPreference
import net.waterfox.android.ext.writeBooleanPreference
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@Composable
fun CheckboxPreference(
    title: String,
    summary: String? = null,
    key: String,
    defaultValue: Boolean,
    onChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val (checked, setChecked) = remember {
        mutableStateOf(context.readBooleanPreference(key, defaultValue))
    }

    val stored = context.readBooleanPreference(key, defaultValue)
    if (stored != checked) {
        setChecked(stored)
    }
    return CheckboxPreference(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = { value ->
            context.writeBooleanPreference(key, value)
            onChange?.invoke(value)
            setChecked(value)
        },
        enabled = enabled,
    )
}

@Composable
fun CheckboxPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    return Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 8.dp)
            .toggleable(
                value = checked,
                onValueChange = if (enabled) onCheckedChange else { _ -> },
                role = Role.Checkbox,
            )
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(72.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 16.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = WaterfoxTheme.colors.formSelected,
                    uncheckedColor = WaterfoxTheme.colors.iconPrimary,
                    checkmarkColor = WaterfoxTheme.colors.layer2,
                ),
            )
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CheckboxPreferenceOnPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        CheckboxPreference(
            title = "Downloads",
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CheckboxPreferenceOffPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        CheckboxPreference(
            title = "Cookies",
            summary = "You'll be logged out of most sites",
            checked = false,
            onCheckedChange = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CheckboxPreferenceDisabledPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        CheckboxPreference(
            title = "Cookies",
            summary = "You'll be logged out of most sites",
            checked = false,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
