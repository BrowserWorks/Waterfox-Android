/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.preference

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.waterfox.android.ext.readBooleanPreference
import net.waterfox.android.ext.writeBooleanPreference
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@Composable
fun RadioGroupPreference(
    items: List<RadioGroupItem>,
) {
    val context = LocalContext.current
    val (selected, setSelected) = remember {
        mutableStateOf(
            items.firstOrNull { context.readBooleanPreference(it.key, it.defaultValue) },
        )
    }

    return Column {
        items.forEach { item ->
            if (item.visible) {
                RadioButtonPreference(
                    title = item.title,
                    selected = selected?.key == item.key,
                    onValueChange = {
                        items.forEach { context.writeBooleanPreference(it.key, it.key == item.key) }
                        item.onClick?.invoke()
                        setSelected(item)
                    },
                )
            }
        }
    }
}

data class RadioGroupItem(
    val title: String,
    val key: String,
    val defaultValue: Boolean,
    val visible: Boolean = true,
    val onClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RadioButtonPreference(
    title: String,
    selected: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    return Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = if (enabled) onValueChange else { _ -> },
                role = Role.RadioButton,
            )
            .alpha(if (enabled) 1f else 0.5f)
            .semantics {
                testTagsAsResourceId = true
                testTag = "radio.button.preference"
            },
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier
                .size(48.dp)
                .padding(start = 16.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = WaterfoxTheme.colors.formSelected,
                unselectedColor = WaterfoxTheme.colors.formDefault,
            ),
        )

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(
                    start = 24.dp,
                    end = 16.dp,
                ),
            color = WaterfoxTheme.colors.textPrimary,
            style = WaterfoxTheme.typography.subtitle1,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RadioButtonPreferenceOnPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        RadioButtonPreference(
            title = "List",
            selected = true,
            onValueChange = {},
            enabled = true,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RadioButtonPreferenceOffPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        RadioButtonPreference(
            title = "List",
            selected = false,
            onValueChange = {},
            enabled = true,
        )
    }
}
