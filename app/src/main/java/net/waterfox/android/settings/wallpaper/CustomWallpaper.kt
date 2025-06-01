/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.wallpaper

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.waterfox.android.R
import net.waterfox.android.compose.button.Button
import net.waterfox.android.compose.ext.dashedBorder
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@Composable
fun CustomWallpaper(
    currentPortraitImageUri: Uri?,
    currentLandscapeImageUri: Uri?,
    onSaveClick: (Uri?, Uri?, Boolean) -> Unit,
) {
    val configuration = LocalConfiguration.current
    var portraitImageUri by rememberSaveable {
        mutableStateOf(currentPortraitImageUri)
    }
    var landscapeImageUri by rememberSaveable {
        mutableStateOf(currentLandscapeImageUri)
    }
    var useSingleImage by rememberSaveable {
        mutableStateOf(false)
    }
    val portraitImageUriLauncher =
        rememberLauncherForActivityResult(PickVisualMedia()) {
            portraitImageUri = it
        }
    val landscapeImageUriLauncher =
        rememberLauncherForActivityResult(PickVisualMedia()) {
            landscapeImageUri = it
        }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CustomWallpaperContent(
                    portraitImageUri = portraitImageUri,
                    landscapeImageUri = landscapeImageUri,
                    useSingleImage = useSingleImage,
                    onSelectPortraitClick = {
                        portraitImageUriLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    onSelectLandscapeClick = {
                        landscapeImageUriLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CustomWallpaperContent(
                    portraitImageUri = portraitImageUri,
                    landscapeImageUri = landscapeImageUri,
                    useSingleImage = useSingleImage,
                    onSelectPortraitClick = {
                        portraitImageUriLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    onSelectLandscapeClick = {
                        landscapeImageUriLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SingleImageSwitch(
            checked = useSingleImage,
            onCheckedChange = { useSingleImage = it },
        )
        Button(
            text = stringResource(id = R.string.wallpaper_save_custom),
            textColor = WaterfoxTheme.colors.textActionPrimary,
            backgroundColor = WaterfoxTheme.colors.actionPrimary,
            tint = WaterfoxTheme.colors.iconActionPrimary,
        ) {
            onSaveClick(portraitImageUri, landscapeImageUri, useSingleImage)
        }
    }
}

@Composable
fun CustomWallpaperContent(
    portraitImageUri: Uri?,
    landscapeImageUri: Uri?,
    useSingleImage: Boolean,
    onSelectPortraitClick: () -> Unit,
    onSelectLandscapeClick: () -> Unit,
    modifier: Modifier,
) {
    WallpaperSelector(
        imageUri = portraitImageUri,
        text = stringResource(id = R.string.wallpaper_select_portrait),
        modifier = modifier,
        action = {
            onSelectPortraitClick()
        },
    )
    if (!useSingleImage) {
        WallpaperSelector(
            imageUri = landscapeImageUri,
            text = stringResource(id = R.string.wallpaper_select_landscape),
            modifier = modifier,
            action = {
                onSelectLandscapeClick()
            },
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp,orientation=portrait")
@Preview(device = "spec:width=411dp,height=891dp,orientation=landscape")
@Composable
fun CustomWallpaperPreview() {
    WaterfoxTheme(theme = Theme.getTheme()) {
        CustomWallpaper(
            currentPortraitImageUri = null,
            currentLandscapeImageUri = null,
            onSaveClick = { _, _, _ -> },
        )
    }
}

@Composable
private fun WallpaperSelector(
    imageUri: Uri?,
    text: String,
    modifier: Modifier = Modifier,
    action: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .dashedBorder(
                    color = WaterfoxTheme.colors.borderPrimary,
                    cornerRadius = 8.dp,
                    dashHeight = 2.dp,
                    dashWidth = 4.dp
                ),
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(id = R.string.wallpaper_image_content_description),
                modifier = Modifier
                    .padding(1.dp)
                    .fillMaxSize(),
            )
        }
        Button(
            text = text,
            textColor = WaterfoxTheme.colors.textActionSecondary,
            backgroundColor = WaterfoxTheme.colors.actionSecondary,
            tint = WaterfoxTheme.colors.iconActionSecondary,
        ) {
            action()
        }
    }
}

@Composable
private fun SingleImageSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.wallpaper_use_single_image),
            color = WaterfoxTheme.colors.textPrimary,
            fontSize = 18.sp,
            modifier = Modifier
                .weight(0.8f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WaterfoxTheme.colors.formSelected,
                checkedTrackColor = WaterfoxTheme.colors.formSurface,
                uncheckedTrackColor = WaterfoxTheme.colors.formSurface,
            ),
        )
    }
}
