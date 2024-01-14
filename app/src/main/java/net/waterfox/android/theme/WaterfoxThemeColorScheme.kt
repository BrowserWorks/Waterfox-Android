/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.theme

import androidx.annotation.StyleRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class WaterfoxThemeColorScheme(
    val id: Long,
    val lightColors: WaterfoxColors,
    val darkColors: WaterfoxColors,
    @StyleRes val resourceId: Int,
    val lightPrimaryColor: Color,
    val darkPrimaryColor: Color,
    val brush: Brush? = null,
)
