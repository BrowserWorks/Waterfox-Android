/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.waterfox.android.R
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@Composable
fun ClearableEditText(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    onClearClicked: () -> Unit,
    errorMessage: String? = null,
    @DrawableRes errorDrawable: Int? = null,
    keyboardType: KeyboardType,
) {
    fun shouldShowClearButton() =
        value.isNotEmpty() && errorMessage == null

    Column(
        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
    ) {
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = label,
            style = TextStyle(
                color = WaterfoxTheme.colors.textPrimary,
                fontSize = 12.sp,
            )
        )

        TextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier),
            textStyle = TextStyle(
                color = WaterfoxTheme.colors.textSecondary,
                fontSize = 15.sp,
            ),
            trailingIcon = {
                if (shouldShowClearButton()) {
                    IconButton(onClick = onClearClicked) {
                        Image(
                            painter = painterResource(id = mozilla.components.ui.icons.R.drawable.mozac_ic_cross_circle_fill_24),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(WaterfoxTheme.colors.textPrimary),
                        )
                    }
                } else if (errorMessage != null && errorDrawable != null) {
                    Icon(
                        painter = painterResource(id = errorDrawable),
                        contentDescription = null,
                        tint = WaterfoxTheme.colors.iconWarning,
                    )
                }
            },
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
            ),
            singleLine = true,
            maxLines = 1,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = WaterfoxTheme.colors.layer1,
                cursorColor = WaterfoxTheme.colors.formSelected,
                focusedIndicatorColor = WaterfoxTheme.colors.formSelected,
                unfocusedIndicatorColor = WaterfoxTheme.colors.formDefault,
                errorIndicatorColor = WaterfoxTheme.colors.borderWarning
            ),
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = TextStyle(
                    color = WaterfoxTheme.colors.textWarning,
                    fontSize = 12.sp,
                )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ClearableEditTextPreview() {
    val value = remember { mutableStateOf("Clearable Text") }
    WaterfoxTheme(theme = Theme.getTheme()) {
        ClearableEditText(
            modifier = Modifier.height(dimensionResource(id = R.dimen.bookmark_edit_text_height)),
            label = stringResource(id = R.string.bookmark_url_label),
            value = value.value,
            onValueChanged = { value.value = it },
            onClearClicked = { value.value = "" },
            errorMessage = stringResource(id = R.string.bookmark_invalid_url_error),
            errorDrawable = mozilla.components.ui.icons.R.drawable.mozac_ic_warning_fill_24,
            keyboardType = KeyboardType.Uri,
        )
    }
}
