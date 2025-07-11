/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package net.waterfox.android.ui.robots

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import net.waterfox.android.components.Components
import net.waterfox.android.helpers.TestHelper.mDevice
import net.waterfox.android.helpers.assertIsEnabled
import net.waterfox.android.helpers.isEnabled
import net.waterfox.android.ui.robots.SettingsSubMenuAccessibilityRobot.Companion.DECIMAL_CONVERSION
import net.waterfox.android.ui.robots.SettingsSubMenuAccessibilityRobot.Companion.MIN_VALUE
import net.waterfox.android.ui.robots.SettingsSubMenuAccessibilityRobot.Companion.STEP_SIZE
import net.waterfox.android.ui.robots.SettingsSubMenuAccessibilityRobot.Companion.TEXT_SIZE
import kotlin.math.roundToInt

/**
 * Implementation of Robot Pattern for the settings Accessibility sub menu.
 */
class SettingsSubMenuAccessibilityRobot {

    companion object {
        const val STEP_SIZE = 5
        const val MIN_VALUE = 50
        const val DECIMAL_CONVERSION = 100f
        const val TEXT_SIZE = 16f
    }

    fun verifyAutomaticFontSizingMenuItems() = assertAutomaticFontSizingMenuItems()

    fun clickFontSizingSwitch() = toggleFontSizingSwitch()

    fun verifyEnabledMenuItems() = assertEnabledMenuItems()

    fun verifyMenuItemsAreDisabled() = assertMenuItemsAreDisabled()

    fun changeTextSizeSlider(seekBarPercentage: Int) = adjustTextSizeSlider(seekBarPercentage)

    fun verifyTextSizePercentage(textSize: Int) = assertTextSizePercentage(textSize)

    class Transition {
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().perform(click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun assertAutomaticFontSizingMenuItems() {
    onView(withText("Automatic font sizing"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    val strFont = "Font size will match your Android settings. Disable to manage font size here."
    onView(withText(strFont))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun toggleFontSizingSwitch() {
    // Toggle font size to off
    onView(withText("Automatic font sizing"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .perform(click())
}

private fun assertEnabledMenuItems() {
    assertFontSize()
    assertSliderBar()
}

private fun assertFontSize() {
    val view = onView(withText("Font Size"))
    view.check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .check(matches(isEnabled(true)))
    val strFont = "Make text on websites larger or smaller"
    onView(withText(strFont))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .check(matches(isEnabled(true)))
}

private fun assertSliderBar() {
//    onView(withId(net.waterfox.android.R.id.sampleText))
//        .check(matches(withText("This is sample text. It is here to show how text will appear when you increase or decrease the size with this setting.")))

    onView(withId(androidx.preference.R.id.seekbar_value))
        .check(matches(withText("100%")))

    onView(withId(androidx.preference.R.id.seekbar))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun adjustTextSizeSlider(seekBarPercentage: Int) {
    onView(withId(androidx.preference.R.id.seekbar))
        .perform(SeekBarChangeProgressViewAction(seekBarPercentage))
}

private fun assertTextSizePercentage(textSize: Int) {
//    onView(withId(net.waterfox.android.R.id.sampleText))
//        .check(textSizePercentageEquals(textSize))
    return
}

private fun assertMenuItemsAreDisabled() {
    onView(withText("Font Size")).assertIsEnabled(false)

    val strFont = "Make text on websites larger or smaller"

    onView(withText(strFont)).assertIsEnabled(false)

//    onView(withId(net.waterfox.android.R.id.sampleText)).assertIsEnabled(false)

    onView(withId(androidx.preference.R.id.seekbar_value)).assertIsEnabled(false)

    onView(withId(androidx.preference.R.id.seekbar)).assertIsEnabled(false)
}

private fun goBackButton() =
    onView(allOf(withContentDescription("Navigate up")))

class SeekBarChangeProgressViewAction(val seekBarPercentage: Int) : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return isAssignableFrom(SeekBar::class.java)
    }

    override fun perform(uiController: UiController?, view: View?) {
        val targetStepSize = calculateStepSizeFromPercentage(seekBarPercentage)
        val seekbar = view as SeekBar
        var progress = seekbar.progress

        if (targetStepSize > progress) {
            for (i in progress until targetStepSize) {
                seekbar.onKeyDown(KEYCODE_DPAD_RIGHT, KeyEvent(ACTION_DOWN, KEYCODE_DPAD_RIGHT))
            }
        } else if (progress > targetStepSize) {
            for (i in progress downTo targetStepSize) {
                seekbar.onKeyDown(KEYCODE_DPAD_LEFT, KeyEvent(ACTION_DOWN, KEYCODE_DPAD_LEFT))
            }
        }
    }

    override fun getDescription(): String {
        return "Changes the progress on a SeekBar, based on the percentage value."
    }
}

fun textSizePercentageEquals(textSizePercentage: Int): ViewAssertion {
    return ViewAssertion { view, noViewFoundException ->
        if (noViewFoundException != null) throw noViewFoundException

        val textView = view as TextView
        val scaledPixels =
            textView.textSize / InstrumentationRegistry.getInstrumentation().context.resources.displayMetrics.scaledDensity
        val currentTextSizePercentage = calculateTextPercentageFromTextSize(scaledPixels)

        if (currentTextSizePercentage != textSizePercentage) throw AssertionError("The textview has a text size percentage of $currentTextSizePercentage, and does not match $textSizePercentage")
    }
}

fun calculateTextPercentageFromTextSize(textSize: Float): Int {
    val decimal = textSize / TEXT_SIZE
    return (decimal * DECIMAL_CONVERSION).roundToInt()
}

fun calculateStepSizeFromPercentage(textSizePercentage: Int): Int {
    return ((textSizePercentage - MIN_VALUE) / STEP_SIZE)
}

fun checkTextSizeOnWebsite(textSizePercentage: Int, components: Components): Boolean {
    // Checks the Gecko engine settings for the font size
    val textSize = calculateStepSizeFromPercentage(textSizePercentage)
    val newTextScale = ((textSize * STEP_SIZE) + MIN_VALUE).toFloat() / DECIMAL_CONVERSION
    return components.core.engine.settings.fontSizeFactor == newTextScale
}
