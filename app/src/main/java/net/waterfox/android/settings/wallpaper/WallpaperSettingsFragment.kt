/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.wallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import net.waterfox.android.R
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.wallpapers.Wallpaper
import net.waterfox.android.wallpapers.WallpaperManager

class WallpaperSettingsFragment : Fragment() {
    private val wallpaperManager by lazy {
        requireComponents.wallpaperManager
    }

    private val settings by lazy {
        requireComponents.settings
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WaterfoxTheme {
                    var currentWallpaper by remember { mutableStateOf(wallpaperManager.currentWallpaper) }
                    var wallpapersSwitchedByLogo by remember { mutableStateOf(settings.wallpapersSwitchedByLogoTap) }
                    WallpaperSettings(
                        wallpapers = wallpaperManager.wallpapers,
                        defaultWallpaper = WallpaperManager.defaultWallpaper,
                        loadWallpaperResource = { wallpaper ->
                            with(wallpaperManager) { wallpaper.load(context) }
                        },
                        selectedWallpaper = currentWallpaper,
                        onSelectWallpaper = { selectedWallpaper: Wallpaper ->
                            currentWallpaper = selectedWallpaper
                            wallpaperManager.currentWallpaper = selectedWallpaper
                        },
                        onViewWallpaper = { findNavController().navigate(R.id.homeFragment) },
                        tapLogoSwitchChecked = wallpapersSwitchedByLogo,
                        onTapLogoSwitchCheckedChange = {
                            settings.wallpapersSwitchedByLogoTap = it
                            wallpapersSwitchedByLogo = it
                        }
                    )
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.customize_wallpapers))
    }
}
