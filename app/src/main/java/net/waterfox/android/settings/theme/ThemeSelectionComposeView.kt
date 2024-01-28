/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.theme

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.waterfox.android.R
import net.waterfox.android.theme.COLOR_SCHEMES
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.theme.WaterfoxThemeColorScheme

class ThemeSelectionComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    val colorSchemes = mutableStateListOf<WaterfoxThemeColorScheme>()
    var selectedColorScheme by mutableStateOf<WaterfoxThemeColorScheme?>(null)
    var onColorSchemeSelected by mutableStateOf({ _: WaterfoxThemeColorScheme -> })
    var selectedMode by mutableIntStateOf(AppCompatDelegate.getDefaultNightMode())
    var onLightThemeClick by mutableStateOf({})
    var onDarkThemeClick by mutableStateOf({})
    var onSetByBatterySaverClick by mutableStateOf({})
    var onFollowDeviceThemeClick by mutableStateOf({})

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        WaterfoxTheme {
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                ColorSchemeGrid(
                    colorSchemes = colorSchemes,
                    selectedColorScheme = selectedColorScheme,
                    onColorSchemeSelected = {
                        selectedColorScheme = it
                        onColorSchemeSelected(it)
                    },
                )
                SchemesList(
                    selectedMode = selectedMode,
                    onLightThemeClick = onLightThemeClick,
                    onDarkThemeClick = onDarkThemeClick,
                    onSetByBatterySaverClick = onSetByBatterySaverClick,
                    onFollowDeviceThemeClick = onFollowDeviceThemeClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSchemeGrid(
    colorSchemes: List<WaterfoxThemeColorScheme>,
    selectedColorScheme: WaterfoxThemeColorScheme?,
    onColorSchemeSelected: (WaterfoxThemeColorScheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(10.dp)
            .background(
                color = WaterfoxTheme.colors.borderPrimary,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Text(
            text = stringResource(id = R.string.preference_color),
            modifier = Modifier
                .padding(
                    start = 24.dp,
                    end = 16.dp,
                    top = 10.dp,
                ),
            color = WaterfoxTheme.colors.textPrimary,
            style = WaterfoxTheme.typography.subtitle1,
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            maxItemsInEachRow = 5,
        ) {
            colorSchemes.forEach {
                ColorSchemeItem(
                    colorScheme = it,
                    isSelected = selectedColorScheme?.id == it.id,
                    onColorSchemeSelected = onColorSchemeSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun ColorSchemeItem(
    colorScheme: WaterfoxThemeColorScheme,
    isSelected: Boolean,
    onColorSchemeSelected: (WaterfoxThemeColorScheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = WaterfoxTheme.colors.borderInverted,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(color = colorScheme.primaryColor)
                .align(Alignment.Center)
                .clickable { onColorSchemeSelected(colorScheme) },
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
private fun ColorSchemeGridPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        ColorSchemeGrid(
            colorSchemes = COLOR_SCHEMES,
            selectedColorScheme = COLOR_SCHEMES[0],
            onColorSchemeSelected = {},
        )
    }
}

@Composable
fun SchemesList(
    selectedMode: Int,
    onLightThemeClick: () -> Unit,
    onDarkThemeClick: () -> Unit,
    onSetByBatterySaverClick: () -> Unit,
    onFollowDeviceThemeClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(10.dp)
            .background(
                color = WaterfoxTheme.colors.borderPrimary,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Text(
            text = stringResource(id = R.string.preference_scheme),
            modifier = Modifier
                .padding(
                    start = 24.dp,
                    end = 16.dp,
                    top = 10.dp,
                ),
            color = WaterfoxTheme.colors.textPrimary,
            style = WaterfoxTheme.typography.subtitle1,
        )
        SchemeItem(
            name = stringResource(R.string.preference_light_theme),
            thumbnail = R.drawable.onboarding_light_theme,
            selected = selectedMode == AppCompatDelegate.MODE_NIGHT_NO,
            onValueChange = { onLightThemeClick() },
        )
        SchemeItem(
            name = stringResource(R.string.preference_dark_theme),
            thumbnail = R.drawable.onboarding_dark_theme,
            selected = selectedMode == AppCompatDelegate.MODE_NIGHT_YES,
            onValueChange = { onDarkThemeClick() },
        )
        if (booleanResource(R.bool.underAPI28)) {
            SchemeItem(
                name = stringResource(R.string.preference_auto_battery_theme),
                thumbnail = R.drawable.onboarding_dark_theme,
                selected = selectedMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
                onValueChange = { onSetByBatterySaverClick() },
            )
        }
        if (booleanResource(R.bool.API28)) {
            SchemeItem(
                name = stringResource(R.string.preference_follow_device_theme),
                thumbnail = R.drawable.onboarding_dark_theme,
                selected = selectedMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                onValueChange = { onFollowDeviceThemeClick() },
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
private fun SchemesListPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        SchemesList(
            selectedMode = AppCompatDelegate.MODE_NIGHT_NO,
            onLightThemeClick = {},
            onDarkThemeClick = {},
            onSetByBatterySaverClick = {},
            onFollowDeviceThemeClick = {},
        )
    }
}

@Composable
fun SchemeItem(
    name: String,
    thumbnail: Int,
    selected: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = onValueChange,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                painter = painterResource(id = thumbnail),
                contentDescription = name,
                modifier = Modifier
                    .padding(horizontal = 3.dp, vertical = 4.dp),
            )
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier,
                colors = RadioButtonDefaults.colors(
                    selectedColor = WaterfoxTheme.colors.formSelected,
                    unselectedColor = WaterfoxTheme.colors.formDefault,
                ),
            )
        }
        Text(
            text = name,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            color = WaterfoxTheme.colors.textPrimary,
            style = WaterfoxTheme.typography.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
