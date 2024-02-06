/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.accessibility

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.waterfox.android.R
import net.waterfox.android.ext.readFloatPreference
import net.waterfox.android.ext.writeFloatPreference
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme
import java.text.NumberFormat
import kotlin.math.round

private const val PERCENT_TO_DECIMAL = 100f
private const val BASE_TEXT_SIZE = 16f

@Composable
fun FontSizePreference(
    key: String,
    defaultValue: Float,
    onChange: ((Float) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    var sliderValue by remember {
        mutableFloatStateOf(context.readFloatPreference(key, defaultValue))
    }

    return FontSizePreference(
        sliderValue = sliderValue,
        onSliderValueChange = { value ->
            sliderValue = (5 * round(value * PERCENT_TO_DECIMAL / 5) / PERCENT_TO_DECIMAL)
        },
        onSliderValueChangeFinished = {
            context.writeFloatPreference(key, sliderValue)
            onChange?.invoke(sliderValue)
        },
        enabled = enabled,
    )
}

@Composable
fun FontSizePreference(
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    return Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.preference_accessibility_font_size_title),
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            color = WaterfoxTheme.colors.textPrimary,
            style = WaterfoxTheme.typography.subtitle1,
        )

        Text(
            text = stringResource(R.string.preference_accessibility_text_size_summary),
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            color = WaterfoxTheme.colors.textSecondary,
            style = WaterfoxTheme.typography.body2,
        )

        val percentage = sliderValue * PERCENT_TO_DECIMAL
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = sliderValue,
                enabled = enabled,
                onValueChange = if (enabled) onSliderValueChange else { _ -> },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .alpha(if (enabled) 1f else 0.5f),
                valueRange = 0.5f..2.0f,
                steps = 30,
                onValueChangeFinished = onSliderValueChangeFinished,
                colors = SliderDefaults.colors(
                    thumbColor = WaterfoxTheme.colors.formSelected,
                    activeTrackColor = WaterfoxTheme.colors.formSelected,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )

            val percentageDecimalValue = (percentage / PERCENT_TO_DECIMAL).toDouble()
            val percentageText = NumberFormat.getPercentInstance().format(percentageDecimalValue)
            Text(
                text = percentageText,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
                color = WaterfoxTheme.colors.textPrimary,
                style = WaterfoxTheme.typography.subtitle1,
            )
        }

        val textSizeDecimalValue = percentage / PERCENT_TO_DECIMAL
        val textSize = BASE_TEXT_SIZE * textSizeDecimalValue
        Text(
            text = stringResource(R.string.accessibility_text_size_sample_text_1),
            modifier = Modifier
                .background(colorResource(R.color.photonViolet05))
                .padding(16.dp),
            color = colorResource(R.color.text_scale_example_text_color).copy(
                alpha = if (enabled) 1f else 0.5f,
            ),
            fontSize = textSize.sp,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FontSizePreferenceEnabledPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        var sliderValue by remember { mutableStateOf(10f) }
        val onSliderValueChange = { newValue: Float -> sliderValue = round(newValue) }
        FontSizePreference(
            sliderValue = sliderValue,
            onSliderValueChange = onSliderValueChange,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FontSizePreferenceDisabledPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        var sliderValue by remember { mutableStateOf(10f) }
        val onSliderValueChange = { newValue: Float -> sliderValue = round(newValue) }
        FontSizePreference(
            sliderValue = sliderValue,
            onSliderValueChange = onSliderValueChange,
            enabled = false,
        )
    }
}
