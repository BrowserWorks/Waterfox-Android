/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.wallpaper

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import net.waterfox.android.R
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.showToolbar
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.wallpapers.LANDSCAPE
import net.waterfox.android.wallpapers.PORTRAIT
import net.waterfox.android.wallpapers.Wallpaper

class CustomWallpaperFragment : Fragment() {

    private val wallpaperManager by lazy { requireComponents.wallpaperManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val portraitImageUri = Uri.fromFile(
            wallpaperManager.getWallpaperFile(requireContext(), PORTRAIT, Wallpaper.Custom.name),
        )
        val landscapeImageUri = Uri.fromFile(
            wallpaperManager.getWallpaperFile(requireContext(), LANDSCAPE, Wallpaper.Custom.name),
        )
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WaterfoxTheme {
                    CustomWallpaper(
                        currentPortraitImageUri = portraitImageUri,
                        currentLandscapeImageUri = landscapeImageUri,
                        onSaveClick = { portraitImageUri, landscapeImageUri, useSingleImage ->
                            wallpaperManager.applyCustomWallpaper(
                                requireContext(),
                                portraitImageUri,
                                landscapeImageUri,
                                useSingleImage,
                            )
                            findNavController().popBackStack()
                        },
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
