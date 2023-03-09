/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.about

import android.content.Context
import android.widget.Toast
import io.mockk.*
import io.mockk.impl.annotations.MockK
import net.waterfox.android.R
import net.waterfox.android.utils.Settings
import org.junit.After
import org.junit.Before
import org.junit.Test

class SecretDebugMenuTriggerTest {

    @MockK private lateinit var logoView: AboutComposeView
    @MockK private lateinit var context: Context
    @MockK private lateinit var settings: Settings
    @MockK(relaxUnitFun = true) private lateinit var toast: Toast
    private lateinit var clickListener: CapturingSlot<() -> Unit>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(Toast::class)
        clickListener = slot()

        every { logoView.onLogoClick = capture(clickListener) } just Runs
        every { logoView.context } returns context
        every {
            context.getString(R.string.about_debug_menu_toast_progress, any())
        } returns "Debug menu: x click(s) left to enable"
        every { settings.showSecretDebugMenuThisSession } returns false
        every { settings.showSecretDebugMenuThisSession = any() } just Runs
        every { Toast.makeText(context, any<Int>(), any()) } returns toast
        every { Toast.makeText(context, any<String>(), any()) } returns toast
    }

    @After
    fun teardown() {
        unmockkStatic(Toast::class)
    }

    @Test
    fun `toast is not displayed on first click`() {
        SecretDebugMenuTrigger(logoView, settings)
        clickListener.captured.invoke()

        verify(inverse = true) { Toast.makeText(context, any<String>(), any()) }
        verify(inverse = true) { toast.show() }
    }

    @Test
    fun `toast is displayed on second click`() {
        SecretDebugMenuTrigger(logoView, settings)
        clickListener.captured.invoke()
        clickListener.captured.invoke()

        verify { context.getString(R.string.about_debug_menu_toast_progress, 3) }
        verify { Toast.makeText(context, any<String>(), Toast.LENGTH_SHORT) }
        verify { toast.show() }
    }

    @Test
    fun `clearClickCounter resets counter`() {
        val trigger = SecretDebugMenuTrigger(logoView, settings)

        clickListener.captured.invoke()
        trigger.onResume(mockk())

        clickListener.captured.invoke()

        verify(inverse = true) { Toast.makeText(context, any<String>(), any()) }
        verify(inverse = true) { toast.show() }
    }

    @Test
    fun `toast is displayed on fifth click`() {
        SecretDebugMenuTrigger(logoView, settings)
        clickListener.captured.invoke()
        clickListener.captured.invoke()
        clickListener.captured.invoke()
        clickListener.captured.invoke()
        clickListener.captured.invoke()

        verify {
            Toast.makeText(
                context,
                R.string.about_debug_menu_toast_done,
                Toast.LENGTH_LONG
            )
        }
        verify { toast.show() }
        verify { settings.showSecretDebugMenuThisSession = true }
    }

    @Test
    fun `don't register click listener if menu is already shown`() {
        every { settings.showSecretDebugMenuThisSession } returns true
        SecretDebugMenuTrigger(logoView, settings)

        verify(inverse = true) { logoView.onLogoClick = any() }
    }
}
