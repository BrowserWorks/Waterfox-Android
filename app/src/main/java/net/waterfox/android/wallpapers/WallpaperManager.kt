/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.wallpapers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import coil3.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.support.base.log.logger.Logger
import net.waterfox.android.R
import net.waterfox.android.components.AppStore
import net.waterfox.android.components.appstate.AppAction
import net.waterfox.android.perf.runBlockingIncrement
import net.waterfox.android.utils.Settings
import java.io.File
import java.util.Date


const val LANDSCAPE = "landscape"
const val PORTRAIT = "portrait"

/**
 * Provides access to available wallpapers and manages their states.
 */
@Suppress("TooManyFunctions")
class WallpaperManager(
    private val settings: Settings,
    private val appStore: AppStore,
    private val downloader: WallpaperDownloader,
    private val fileManager: WallpaperFileManager,
    private val currentLocale: String,
    allWallpapers: List<Wallpaper> = availableWallpapers
) {
    val logger = Logger("WallpaperManager")

    val wallpapers = allWallpapers
        .filter(::filterExpiredRemoteWallpapers)
        .filter(::filterPromotionalWallpapers)
        .also {
            appStore.dispatch(AppAction.WallpaperAction.UpdateAvailableWallpapers(it))
        }

    var currentWallpaper: Wallpaper = getCurrentWallpaperFromSettings()
        set(value) {
            settings.currentWallpaper = value.name
            appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(value))
            field = value
        }

    init {
        fileManager.clean(currentWallpaper, wallpapers.filterIsInstance<Wallpaper.Remote>())
    }

    /**
     * Download all known remote wallpapers.
     */
    suspend fun downloadAllRemoteWallpapers() {
        for (wallpaper in wallpapers.filterIsInstance<Wallpaper.Remote>()) {
            downloader.downloadWallpaper(wallpaper)
        }
    }

    /**
     * Returns the next available [Wallpaper], the [currentWallpaper] is the last one then
     * the first available [Wallpaper] will be returned.
     */
    fun switchToNextWallpaper(): Wallpaper {
        val values = wallpapers
        val index = values.indexOf(currentWallpaper) + 1

        return if (index >= values.size) {
            values.first()
        } else {
            values[index]
        }.also {
            currentWallpaper = it
        }
    }

    private fun filterExpiredRemoteWallpapers(wallpaper: Wallpaper): Boolean = when (wallpaper) {
        is Wallpaper.Remote -> {
            val notExpired = wallpaper.expirationDate?.let { Date().before(it) } ?: true
            notExpired || wallpaper.name == settings.currentWallpaper
        }
        else -> true
    }

    private fun filterPromotionalWallpapers(wallpaper: Wallpaper): Boolean =
        if (wallpaper is Wallpaper.Promotional) {
            wallpaper.isAvailableInLocale(currentLocale)
        } else {
            true
        }

    private fun getCurrentWallpaperFromSettings(): Wallpaper {
        return if (isDefaultTheCurrentWallpaper(settings)) {
            defaultWallpaper
        } else if (isCustomTheCurrentWallpaper(settings)) {
            customWallpaper
        } else {
            val currentWallpaper = settings.currentWallpaper
            wallpapers.find { it.name == currentWallpaper }
                ?: fileManager.lookupExpiredWallpaper(currentWallpaper)
                ?: defaultWallpaper
        }.also {
            appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(it))
        }
    }

    /**
     * Load a wallpaper that is saved locally.
     */
    fun Wallpaper.load(context: Context): Bitmap? =
        when (this) {
            is Wallpaper.Local -> loadWallpaperFromDrawables(context, this)
            is Wallpaper.Remote -> loadWallpaperFromDisk(context, this)
            else -> null
        }

    fun Wallpaper.set(view: ImageView) {
        if (this is Wallpaper.Custom) {
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.load(getCustomWallpaperFile(view.context))
        } else {
            load(view.context)?.scaleBitmapToBottomOfView(view)
        }
    }

    private fun loadWallpaperFromDrawables(context: Context, wallpaper: Wallpaper.Local): Bitmap? = Result.runCatching {
        BitmapFactory.decodeResource(context.resources, wallpaper.drawableId)
    }.getOrNull()

    /**
     * Load a wallpaper from app-specific storage.
     */
    private fun loadWallpaperFromDisk(context: Context, wallpaper: Wallpaper.Remote): Bitmap? = Result.runCatching {
        val path = wallpaper.getLocalPathFromContext(context)
        runBlockingIncrement {
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, path)
                BitmapFactory.decodeStream(file.inputStream())
            }
        }
    }.getOrNull()

    /**
     * This will scale the received [Bitmap] to the size of the [view]. It retains the bitmap's
     * original aspect ratio, but will shrink or enlarge it to fit the viewport. If bitmap does not
     * correctly fit the aspect ratio of the view, it will be shifted to prioritize the bottom-left
     * of the bitmap.
     */
    fun Bitmap.scaleBitmapToBottomOfView(view: ImageView) {
        val bitmap = this
        view.setImageBitmap(bitmap)
        view.scaleType = ImageView.ScaleType.MATRIX
        val matrix = Matrix()
        view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                val viewWidth: Float = view.width.toFloat()
                val viewHeight: Float = view.height.toFloat()
                val bitmapWidth = bitmap.width
                val bitmapHeight = bitmap.height
                val widthScale = viewWidth / bitmapWidth
                val heightScale = viewHeight / bitmapHeight
                val scale = widthScale.coerceAtLeast(heightScale)
                matrix.postScale(scale, scale)
                // The image is translated to its bottom such that any pertinent information is
                // guaranteed to be shown.
                // Majority of this math borrowed from // https://medium.com/@tokudu/how-to-whitelist-strictmode-violations-on-android-based-on-stacktrace-eb0018e909aa
                // except that there is no need to translate horizontally in our case.
                matrix.postTranslate(0f, (viewHeight - bitmapHeight * scale))
                view.imageMatrix = matrix
                view.removeOnLayoutChangeListener(this)
            }
        })
    }

    /**
     * Get the expected local path on disk for a wallpaper. This will differ depending
     * on orientation and app theme.
     */
    private fun Wallpaper.Remote.getLocalPathFromContext(context: Context): String {
        val orientation = if (context.isLandscape()) LANDSCAPE else PORTRAIT
        val theme = if (context.isDark()) "dark" else "light"
        return Wallpaper.getBaseLocalPath(orientation, theme, name)
    }

    private fun Context.isDark(): Boolean {
        return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Animates the Waterfox logo, if it hasn't been animated before, otherwise nothing will happen.
     * After animating the first time, the [Settings.shouldAnimateWaterfoxLogo] setting
     * will be updated.
     */
    @Suppress("MagicNumber")
    fun animateLogoIfNeeded(logo: View) {
        if (!settings.shouldAnimateWaterfoxLogo) {
            return
        }
        Handler(Looper.getMainLooper()).postDelayed(
            {
                val animator1 = ObjectAnimator.ofFloat(logo, "rotation", 0f, 10f)
                val animator2 = ObjectAnimator.ofFloat(logo, "rotation", 10f, 0f)
                val animator3 = ObjectAnimator.ofFloat(logo, "rotation", 0f, 10f)
                val animator4 = ObjectAnimator.ofFloat(logo, "rotation", 10f, 0f)

                animator1.duration = 200
                animator2.duration = 200
                animator3.duration = 200
                animator4.duration = 200

                val set = AnimatorSet()

                set.play(animator1).before(animator2).after(animator3).before(animator4)
                set.start()

                settings.shouldAnimateWaterfoxLogo = false
            },
            ANIMATION_DELAY_MS
        )
    }

    private fun copyWallpaperImage(
        context: Context,
        orientation: String,
        uri: Uri,
    ) {
        if (uri.scheme == "file") {
            return
        }
        fileManager.copyWallpaperImage(context, orientation, uri)
    }

    fun applyCustomWallpaper(
        context: Context,
        portraitImageUri: Uri?,
        landscapeImageUri:Uri?,
        useSingleImage:Boolean,
    ) {
        if (portraitImageUri != null) {
            copyWallpaperImage(context, PORTRAIT, portraitImageUri)
        }
        if (landscapeImageUri != null && !useSingleImage) {
            copyWallpaperImage(context, LANDSCAPE, landscapeImageUri)
        } else {
            getWallpaperFile(context, LANDSCAPE, Wallpaper.Custom.name).delete()
        }
        if (portraitImageUri != null || landscapeImageUri != null) {
            currentWallpaper = customWallpaper
        }
    }

    fun getWallpaperFile(context: Context, orientation: String, name: String): File {
        return File(
            context.filesDir,
            Wallpaper.getBaseLocalPath(orientation, name),
        )
    }

    companion object {
        /**
         *  Get whether the default wallpaper should be used.
         */
        fun isDefaultTheCurrentWallpaper(settings: Settings): Boolean = with(settings.currentWallpaper) {
            return isEmpty() || equals(defaultWallpaper.name)
        }

        /**
         * Get whether the wallpaper set by a user should be used.
         */
        fun isCustomTheCurrentWallpaper(settings: Settings): Boolean = with(settings.currentWallpaper) {
            return equals(customWallpaper.name)
        }

        fun getCustomWallpaperFile(context: Context): File? = Result.runCatching {
            val orientation = if (context.isLandscape()) LANDSCAPE else PORTRAIT
            val path = Wallpaper.getBaseLocalPath(orientation, Wallpaper.Custom.name)
            runBlockingIncrement {
                withContext(Dispatchers.IO) {
                    val file = File(context.filesDir, path)
                    if (file.exists()) {
                        file
                    } else {
                        File(
                            context.filesDir,
                            Wallpaper.getBaseLocalPath(PORTRAIT, Wallpaper.Custom.name),
                        )
                    }
                }
            }
        }.getOrNull()

        private fun Context.isLandscape(): Boolean {
            return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        val defaultWallpaper = Wallpaper.Default
        val customWallpaper = Wallpaper.Custom
        private val localWallpapers: List<Wallpaper.Local> = listOf(
            Wallpaper.Local.Waterfox("amethyst", R.drawable.amethyst),
            Wallpaper.Local.Waterfox("cerulean", R.drawable.cerulean),
            Wallpaper.Local.Waterfox("sunrise", R.drawable.sunrise),
        )
        private val remoteWallpapers: List<Wallpaper.Remote> = listOf(
//            Wallpaper.Remote.Firefox(
//                "twilight-hills"
//            ),
//            Wallpaper.Remote.Firefox(
//                "beach-vibe"
//            ),
        )
        private val availableWallpapers = listOf(defaultWallpaper) + localWallpapers + remoteWallpapers +
            listOf(customWallpaper)
        private const val ANIMATION_DELAY_MS = 1500L
    }
}
