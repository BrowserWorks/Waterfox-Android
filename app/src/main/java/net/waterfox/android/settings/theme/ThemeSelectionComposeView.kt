/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.theme

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        WaterfoxTheme {
            ColorSchemeGrid(
                colorSchemes = colorSchemes,
                selectedColorScheme = selectedColorScheme,
                onColorSchemeSelected = {
                    selectedColorScheme = it
                    onColorSchemeSelected(it)
                },
            )
        }
    }
}

@Composable
fun ColorSchemeGrid(
    colorSchemes: List<WaterfoxThemeColorScheme>,
    selectedColorScheme: WaterfoxThemeColorScheme?,
    onColorSchemeSelected: (WaterfoxThemeColorScheme) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(colorSchemes) {
            ColorSchemeItem(
                colorScheme = it,
                isSelected = selectedColorScheme?.id == it.id,
                onColorSchemeSelected = onColorSchemeSelected,
            )
        }
    }
}

@Composable
fun ColorSchemeItem(
    colorScheme: WaterfoxThemeColorScheme,
    isSelected: Boolean,
    onColorSchemeSelected: (WaterfoxThemeColorScheme) -> Unit,
) {
    Box(
        modifier = Modifier,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = WaterfoxTheme.colors.formSelected,
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
            selectedColorScheme = null,
            onColorSchemeSelected = {},
        )
    }
}
