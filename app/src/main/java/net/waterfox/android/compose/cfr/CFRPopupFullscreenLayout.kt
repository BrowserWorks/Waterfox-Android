/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.cfr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginStart
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.support.ktx.android.util.dpToPx
import mozilla.components.support.ktx.android.view.toScope
import net.waterfox.android.compose.cfr.CFRPopup.IndicatorDirection.DOWN
import net.waterfox.android.compose.cfr.CFRPopup.IndicatorDirection.UP
import net.waterfox.android.compose.cfr.CFRPopup.PopupAlignment.BODY_TO_ANCHOR_CENTER
import net.waterfox.android.compose.cfr.CFRPopup.PopupAlignment.BODY_TO_ANCHOR_START
import net.waterfox.android.compose.cfr.CFRPopup.PopupAlignment.INDICATOR_CENTERED_IN_ANCHOR
import net.waterfox.android.compose.cfr.helper.DisplayOrientationListener
import net.waterfox.android.compose.cfr.helper.ViewDetachedListener
import net.waterfox.android.theme.WaterfoxTheme
import org.mozilla.gecko.GeckoScreenOrientation
import kotlin.math.roundToInt

@VisibleForTesting
internal const val SHOW_AFTER_SCREEN_ORIENTATION_CHANGE_DELAY = 500L

/**
 * Value class allowing to easily reason about what an `Int` represents.
 * This is compiled to the underlying `Int` type so incurs no performance penalty.
 */
@JvmInline
private value class Pixels(val value: Int)

/**
 * Simple wrapper over the absolute x-coordinates of the popup. Includes any paddings.
 */
private data class PopupHorizontalBounds(
    val startCoord: Pixels,
    val endCoord: Pixels
)

/**
 * [AbstractComposeView] that can be added or removed dynamically in the current window to display
 * a [Composable] based popup anywhere on the screen.
 *
 * @param text [String] shown as the popup content.
 * @param anchor [View] that will serve as the anchor of the popup and serve as lifecycle owner
 * for this popup also.
 * @param properties [CFRPopupProperties] allowing to customize the popup behavior.
 * @param onDismiss Callback for when the popup is dismissed indicating also if the dismissal
 * was explicit - by tapping the "X" button or not.
 * @param action Optional other composable to show just below the popup text.
 */
@SuppressLint("ViewConstructor") // Intended to be used only in code, don't need a View constructor
internal class CFRPopupFullscreenLayout(
    private val text: String,
    private val anchor: View,
    private val properties: CFRPopupProperties,
    private val onDismiss: (Boolean) -> Unit,
    private val action: @Composable (() -> Unit) = {}
) : AbstractComposeView(anchor.context), ViewRootForInspector {
    private val windowManager = anchor.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Listener for when the anchor is removed from the screen.
     * Useful in the following situations:
     *   - lack of purpose - if there is no anchor the context/action to which this popup refers to disappeared
     *   - leak from WindowManager - if removing the app from task manager while the popup is shown.
     *
     * Will not inform client about this since the user did not expressly dismissed this popup.
     */
    private val anchorDetachedListener = ViewDetachedListener {
        dismiss()
    }

    /**
     * When the screen is rotated the popup may get improperly anchored
     * because of the async nature of insets and screen rotation.
     * To avoid any improper anchorage the popups are automatically dismissed.
     *
     * Will not inform client about this since the user did not expressly dismissed this popup.
     *
     * Since a UX decision has been made here:
     * [link](https://github.com/mozilla-mobile/fenix/issues/27033#issuecomment-1302363014)
     * to redisplay any **implicitly** dismissed CFRs, a short delay will be added,
     * after which the CFR will be shown again.
     *
     */
    @VisibleForTesting
    internal lateinit var orientationChangeListener: DisplayOrientationListener

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    /**
     * Add a new CFR popup to the current window overlaying everything already displayed.
     * This popup will be dismissed when the user clicks on the "x" button or based on other user actions
     * with such behavior set in [CFRPopupProperties].
     */
    fun show() {
        if (!isAttachedToWindow) {
            val anchorViewTreeLifecycleOwner = anchor.findViewTreeLifecycleOwner()
            val anchorViewTreeSavedStateRegistryOwner = anchor.findViewTreeSavedStateRegistryOwner()

            if (anchorViewTreeLifecycleOwner != null && anchorViewTreeSavedStateRegistryOwner != null) {
                setViewTreeLifecycleOwner(anchorViewTreeLifecycleOwner)
                this.setViewTreeSavedStateRegistryOwner(anchorViewTreeSavedStateRegistryOwner)
                anchor.addOnAttachStateChangeListener(anchorDetachedListener)
                orientationChangeListener = getDisplayOrientationListener(anchor.context).also {
                    it.start()
                }
                windowManager.addView(this, createLayoutParams())
            }
        }
    }

    @Composable
    override fun Content() {
        val anchorLocation = IntArray(2).apply {
            anchor.getLocationOnScreen(this)
        }

        val indicatorArrowHeight = Pixels(
            CFRPopup.DEFAULT_INDICATOR_HEIGHT.dp.toPx()
        )

        val popupBounds = computePopupHorizontalBounds(
            arrowIndicatorWidth = Pixels(CFRPopupShape.getIndicatorBaseWidthForHeight(indicatorArrowHeight.value)),
        )
        val indicatorOffset = computeIndicatorArrowStartCoord(
            anchorMiddleXCoord = Pixels(anchorLocation.first() + anchor.width / 2),
            popupStartCoord = popupBounds.startCoord,
            arrowIndicatorWidth = Pixels(
                CFRPopupShape.getIndicatorBaseWidthForHeight(indicatorArrowHeight.value)
            )
        )

        WaterfoxTheme {
            Popup(
                popupPositionProvider = getPopupPositionProvider(
                    anchorLocation = anchorLocation,
                    popupBounds = popupBounds,
                ),
                properties = PopupProperties(
                    focusable = properties.dismissOnBackPress,
                    dismissOnBackPress = properties.dismissOnBackPress,
                    dismissOnClickOutside = properties.dismissOnClickOutside,
                ),
                onDismissRequest = {
                    // For when tapping outside the popup.
                    dismiss()
                    onDismiss(false)
                }
            ) {
                CFRPopupContent(
                    text = text,
                    indicatorDirection = properties.indicatorDirection,
                    indicatorArrowStartOffset = with(LocalDensity.current) {
                        indicatorOffset.value.toDp()
                    },
                    onDismiss = {
                        // For when tapping the "X" button.
                        dismiss()
                        onDismiss(true)
                    },
                    action = action,
                )
            }
        }
    }

    @Composable
    private fun getPopupPositionProvider(
        anchorLocation: IntArray,
        popupBounds: PopupHorizontalBounds,
    ): PopupPositionProvider {
        return object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                // Popup will be anchored such that the indicator arrow will point to the middle of the anchor View
                // but the popup is allowed some space as start padding in which it can be displayed such that the
                // indicator arrow is exactly at the top-start/bottom-start corner but slightly translated to end.
                // Values are in pixels.
                return IntOffset(
                    when (layoutDirection) {
                        Ltr -> popupBounds.startCoord.value
                        else -> popupBounds.endCoord.value
                    },
                    when (properties.indicatorDirection) {
                        UP -> {
                            when (properties.overlapAnchor) {
                                true -> anchorLocation.last() + anchor.height / 2
                                else -> anchorLocation.last() + anchor.height + properties.popupVerticalOffset.toPx()
                            }
                        }
                        DOWN -> {
                            when (properties.overlapAnchor) {
                                true -> anchorLocation.last() - popupContentSize.height + anchor.height / 2
                                else -> anchorLocation.last() - popupContentSize.height -
                                        properties.popupVerticalOffset.toPx()
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * Compute the x-coordinates for the absolute start and end position of the popup, including any padding.
     * This assumes anchoring is indicated with an arrow to the horizontal middle of the anchor with the popup's
     * body potentially extending to the `start` of the arrow indicator.
     *
     * @param arrowIndicatorWidth x-distance the arrow indicator occupies.
     */
    @Composable
    private fun computePopupHorizontalBounds(
        arrowIndicatorWidth: Pixels
    ): PopupHorizontalBounds {
        val arrowIndicatorHalfWidth = arrowIndicatorWidth.value / 2

        return if (LocalConfiguration.current.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            val startCoord = when (properties.popupAlignment) {
                BODY_TO_ANCHOR_START -> {
                    val visibleAnchorStart = anchor.x.roundToInt() + anchor.paddingStart + anchor.marginStart
                    Pixels(visibleAnchorStart - CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx())
                }
                BODY_TO_ANCHOR_CENTER -> {
                    val popupWidth = (properties.popupWidth + CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp * 2).toPx()
                    Pixels(((anchor.x.roundToInt() + anchor.width) - popupWidth) / 2)
                }
                INDICATOR_CENTERED_IN_ANCHOR -> {
                    val anchorMiddleXCoord = Pixels(anchor.x.roundToInt() + anchor.width / 2)
                    // Push the popup as far to the start as needed including any needed paddings.
                    Pixels(
                        (anchorMiddleXCoord.value - arrowIndicatorHalfWidth)
                            .minus(properties.indicatorArrowStartOffset.toPx())
                            .minus(CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx())
                            .coerceAtLeast(getLeftInsets())
                    )
                }
            }

            PopupHorizontalBounds(
                startCoord = startCoord,
                endCoord = Pixels(
                    startCoord.value
                        .plus(properties.popupWidth.toPx())
                        .plus(CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx() * 2)
                )
            )
        } else {
            val startCoord = when (properties.popupAlignment) {
                BODY_TO_ANCHOR_START -> {
                    val visibleAnchorEnd = anchor.x.roundToInt() + anchor.width - anchor.paddingStart
                    Pixels(visibleAnchorEnd + CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx())
                }
                BODY_TO_ANCHOR_CENTER -> {
                    val popupWidth = (properties.popupWidth + CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp * 2).toPx()
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp.toPx()
                    Pixels(screenWidth - ((anchor.x.roundToInt() + anchor.width) - popupWidth) / 2)
                }
                INDICATOR_CENTERED_IN_ANCHOR -> {
                    val anchorMiddleXCoord = Pixels(anchor.x.roundToInt() + anchor.width / 2)
                    Pixels(
                        // Push the popup as far to the start (in RTL) as possible.
                        anchorMiddleXCoord.value
                            .plus(arrowIndicatorHalfWidth)
                            .plus(properties.indicatorArrowStartOffset.toPx())
                            .plus(CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx())
                            .coerceAtMost(LocalConfiguration.current.screenWidthDp.dp.toPx())
                            .plus(getLeftInsets())
                    )
                }
            }
            PopupHorizontalBounds(
                startCoord = startCoord,
                endCoord = Pixels(
                    startCoord.value
                        .minus(properties.popupWidth.toPx())
                        .minus(CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx() * 2)
                )
            )
        }
    }

    /**
     * Compute the x-coordinate for where the popup's indicator arrow should start
     * relative to the available distance between it and the popup's starting x-coordinate.
     *
     * @param anchorMiddleXCoord x-coordinate for the middle of the anchor.
     * @param popupStartCoord x-coordinate for the popup start
     * @param arrowIndicatorWidth Width of the arrow indicator.
     */
    @Composable
    private fun computeIndicatorArrowStartCoord(
        anchorMiddleXCoord: Pixels,
        popupStartCoord: Pixels,
        arrowIndicatorWidth: Pixels
    ): Pixels {
        return when (properties.popupAlignment) {
            BODY_TO_ANCHOR_START,
            BODY_TO_ANCHOR_CENTER -> Pixels(properties.indicatorArrowStartOffset.toPx())
            INDICATOR_CENTERED_IN_ANCHOR -> {
                val arrowIndicatorHalfWidth = arrowIndicatorWidth.value / 2
                if (LocalConfiguration.current.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                    val visiblePopupStartCoord =
                        popupStartCoord.value + CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx()
                    Pixels(anchorMiddleXCoord.value - arrowIndicatorHalfWidth - visiblePopupStartCoord)
                } else {
                    val visiblePopupEndCoord =
                        popupStartCoord.value - CFRPopup.DEFAULT_HORIZONTAL_PADDING.dp.toPx()
                    Pixels(visiblePopupEndCoord - anchorMiddleXCoord.value - arrowIndicatorHalfWidth)
                }
            }
        }
    }

    /**
     * Cleanup and remove the current popup from the screen.
     * Clients are not automatically informed about this. Use a separate call to [onDismiss] if needed.
     */
    internal fun dismiss() {
        if (isAttachedToWindow) {
            anchor.removeOnAttachStateChangeListener(anchorDetachedListener)
            orientationChangeListener.stop()
            disposeComposition()
            setViewTreeLifecycleOwner(null)
            this.setViewTreeSavedStateRegistryOwner(null)
            windowManager.removeViewImmediate(this)
        }
    }

    /**
     * Create fullscreen translucent layout params.
     * This will allow placing the visible popup anywhere on the screen.
     */
    @VisibleForTesting
    internal fun createLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            token = anchor.applicationWindowToken
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }

    private fun getDisplayOrientationListener(context: Context) = DisplayOrientationListener(context) {
        dismiss()
        anchor.toScope().launch {
            delay(SHOW_AFTER_SCREEN_ORIENTATION_CHANGE_DELAY)
            show()
        }
    }

    /**
     * Intended to allow querying the insets of the navigation bar.
     * Value will be `0` except for when the screen is rotated by 90 degrees.
     */
    private fun getLeftInsets() = ViewCompat.getRootWindowInsets(anchor)
        ?.getInsets(WindowInsetsCompat.Type.systemBars())?.left
        ?: 0.coerceAtLeast(0)

    @Px
    internal fun Dp.toPx(): Int {
        return this.value
            .dpToPx(anchor.resources.displayMetrics)
            .roundToInt()
    }
}
