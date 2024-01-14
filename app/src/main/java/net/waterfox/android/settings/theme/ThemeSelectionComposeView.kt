/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.theme

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.waterfox.android.R
import net.waterfox.android.ext.settings
import net.waterfox.android.theme.WaterfoxPredefinedTheme
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.theme.darkColorPalette
import net.waterfox.android.theme.darkGreyColorPalette
import net.waterfox.android.theme.lightColorPalette
import net.waterfox.android.theme.lightGreyColorPalette

class ThemeSelectionComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    val lightThemes = mutableStateListOf<WaterfoxPredefinedTheme>()
    val darkThemes = mutableStateListOf<WaterfoxPredefinedTheme>()
    var onThemeSelectedClick by mutableStateOf({ _: WaterfoxPredefinedTheme -> })

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        WaterfoxTheme {
            ThemeSelection(lightThemes, darkThemes, onThemeSelectedClick)
        }
    }
}

@Composable
private fun ThemeSelection(
    lightThemes: List<WaterfoxPredefinedTheme>,
    darkThemes: List<WaterfoxPredefinedTheme>,
    onThemeSelectedClick: (WaterfoxPredefinedTheme) -> Unit,
) {
    val context = LocalContext.current
    var lightTheme by remember {
        mutableStateOf(context.settings().lightTheme)
    }
    var darkTheme by remember {
        mutableStateOf(context.settings().darkTheme)
    }
    Row(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp),
    ) {
        ThemesColumn(
            modifier = Modifier.weight(1f),
            title = "Light Themes",
            themes = lightThemes,
            selectedThemeId = lightTheme,
            onThemeSelectedClick = {
                lightTheme = it.id
                onThemeSelectedClick(it)
            },
        )
        ThemesColumn(
            modifier = Modifier.weight(1f),
            title = "Dark Themes",
            themes = darkThemes,
            selectedThemeId = darkTheme,
            onThemeSelectedClick = {
                darkTheme = it.id
                onThemeSelectedClick(it)
            },
        )
    }
}

@Composable
private fun ThemesColumn(
    modifier: Modifier,
    title: String,
    themes: List<WaterfoxPredefinedTheme>,
    selectedThemeId: String?,
    onThemeSelectedClick: (WaterfoxPredefinedTheme) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = WaterfoxTheme.colors.textPrimary,
            style = WaterfoxTheme.typography.subtitle1,
        )
        Spacer(modifier = Modifier.height(8.dp))
        themes.forEach { theme ->
            ThemeItem(
                theme = theme,
                selected = selectedThemeId == theme.id,
                onValueChange = { selected ->
                    if (selected) {
                        onThemeSelectedClick(theme)
                    }
                },
            )
        }
    }
}

@Composable
private fun ThemeItem(
    theme: WaterfoxPredefinedTheme,
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
                painter = painterResource(id = theme.thumbnail),
                contentDescription = stringResource(id = theme.name),
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
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
            text = stringResource(id = theme.name),
            textAlign = TextAlign.Left,
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

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
private fun ThemeSelectionComposeViewPreview() {
    WaterfoxTheme {
        ThemeSelection(
            lightThemes = listOf(
                WaterfoxPredefinedTheme(
                    id = "light_default",
                    name = R.string.theme_light_default,
                    colors = lightColorPalette,
                    resourceId = 0,
                    isLight = true,
                    thumbnail = R.drawable.onboarding_light_theme,
                ),
                WaterfoxPredefinedTheme(
                    id = "light_grey",
                    name = R.string.theme_light_grey,
                    colors = lightGreyColorPalette,
                    resourceId = 0,
                    isLight = true,
                    thumbnail = R.drawable.onboarding_light_theme,
                ),
            ),
            darkThemes = listOf(
                WaterfoxPredefinedTheme(
                    id = "dark_default",
                    name = R.string.theme_dark_default,
                    colors = darkColorPalette,
                    resourceId = 0,
                    isLight = false,
                    thumbnail = R.drawable.onboarding_dark_theme,
                ),
                WaterfoxPredefinedTheme(
                    id = "dark_grey",
                    name = R.string.theme_dark_grey,
                    colors = darkGreyColorPalette,
                    resourceId = 0,
                    isLight = false,
                    thumbnail = R.drawable.onboarding_dark_theme,
                ),
            ),
            onThemeSelectedClick = {},
        )
    }
}
