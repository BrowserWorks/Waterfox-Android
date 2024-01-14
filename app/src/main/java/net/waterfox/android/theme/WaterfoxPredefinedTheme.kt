/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes

data class WaterfoxPredefinedTheme(
    val id: String,
    @StringRes val name: Int,
    val colors: WaterfoxColors,
    @StyleRes val resourceId: Int,
    val isLight: Boolean,
    @DrawableRes val thumbnail: Int,
)
