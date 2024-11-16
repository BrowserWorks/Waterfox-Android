/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.preference

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.waterfox.android.ext.readBooleanPreference
import net.waterfox.android.ext.writeBooleanPreference
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.R
import net.waterfox.android.ext.readStringPreference
import net.waterfox.android.ext.writeStringPreference

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
        val onValueChange: (Boolean, RadioGroupItem) -> Unit = { _, item ->
            items.forEach {
                context.writeBooleanPreference(
                    it.key,
                    it.key == item.key,
                )
            }
            item.onClick?.invoke()
            setSelected(item)
        }
        items.forEach { item ->
            if (item.visible) {
                if (item.editable) {
                    val key = stringResource(R.string.pref_key_new_tab_web_address_value)
                    RadioButtonWithInputPreference(
                        value = context.readStringPreference(key, "")!!,
                        selected = selected?.key == item.key,
                        onValueChange = { onValueChange(it, item) },
                        onInputValueChange = { context.writeStringPreference(key, it) },
                    )
                } else {
                    RadioButtonPreference(
                        title = item.title,
                        selected = selected?.key == item.key,
                        onValueChange = { onValueChange(it, item) },
                    )
                }
            }
        }
    }
}

data class RadioGroupItem(
    val title: String,
    val key: String,
    val defaultValue: Boolean,
    val visible: Boolean = true,
    val editable: Boolean = false,
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


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RadioButtonWithInputPreference(
    value: String,
    selected: Boolean,
    onValueChange: (Boolean) -> Unit,
    onInputValueChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = { newValue ->
                    if (enabled) {
                        onValueChange(newValue)
                    }
                },
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

        TextField(
            text = value,
            onValueChange = {
                onInputValueChange(it)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
            selected = selected,
            modifier = Modifier
                .align(Alignment.CenterVertically),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun TextField(
    text: String,
    onValueChange: (String) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(text) }
    val interactionSource = remember { MutableInteractionSource() }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = WaterfoxTheme.colors.formSelected,
        backgroundColor = WaterfoxTheme.colors.formSelected.copy(alpha = 0.4f),
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 16.dp,
                )
                .indicatorLine(
                    enabled = selected,
                    false,
                    interactionSource,
                    TextFieldDefaults.textFieldColors(
                        unfocusedIndicatorColor = WaterfoxTheme.colors.formDisabled,
                        focusedIndicatorColor = WaterfoxTheme.colors.formSelected,
                    ),
                ),
            singleLine = true,
            enabled = selected,
            textStyle = WaterfoxTheme.typography.subtitle1.merge(
                TextStyle(color = WaterfoxTheme.colors.textPrimary),
            ),
            cursorBrush = SolidColor(WaterfoxTheme.colors.formSelected),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.None,
            ),
            keyboardActions = KeyboardActions(onDone = { onValueChange(value) }),
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = value,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.preferences_open_new_tab_web_address),
                        modifier = Modifier.fillMaxWidth(),
                        color = WaterfoxTheme.colors.textSecondary,
                        style = WaterfoxTheme.typography.subtitle1,
                    )
                },
                singleLine = true,
                enabled = selected,
                interactionSource = interactionSource,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp, end = 0.dp,
                ),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = WaterfoxTheme.colors.textPrimary,
                    cursorColor = WaterfoxTheme.colors.formSelected,
                ),
            )
        }
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RadioButtonWithInputPreferenceOnPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        RadioButtonWithInputPreference(
            value = "example.com",
            selected = true,
            onValueChange = {},
            onInputValueChange = {},
            enabled = true,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RadioButtonWithInputPreferenceOffPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        RadioButtonWithInputPreference(
            value = "example.com",
            selected = false,
            onValueChange = {},
            onInputValueChange = {},
            enabled = true,
        )
    }
}
