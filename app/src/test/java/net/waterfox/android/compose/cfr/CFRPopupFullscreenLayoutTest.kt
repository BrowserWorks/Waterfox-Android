/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose.cfr

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.view.View
import android.view.ViewManager
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@RunWith(WaterfoxRobolectricTestRunner::class)
class CFRPopupFullscreenLayoutTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @Test
    fun `GIVEN already attached to window WHEN the popup is shown THEN do nothing`() {
        val anchor = View(testContext).apply {
            setViewTreeLifecycleOwner(mock())
            this.setViewTreeSavedStateRegistryOwner(mock())
        }

        val popupView = spy(
            CFRPopupFullscreenLayout(
                text = "test",
                anchor = anchor,
                properties = mock(),
                onDismiss = mock(),
                action = { },
            ),
        )
        `when`(popupView.isAttachedToWindow).thenReturn(true)
        popupView.show()

        assertNull(popupView.findViewTreeLifecycleOwner())
        assertNull(popupView.findViewTreeSavedStateRegistryOwner())
    }

    @Test
    fun `GIVEN is attached to window WHEN the popup is dismissed THEN cleanup lifecycle owners and detach from window`() {
        val context = spy(testContext)
        val anchor = View(context).apply {
            setViewTreeLifecycleOwner(mock())
            this.setViewTreeSavedStateRegistryOwner(mock())
        }
        val windowManager = spy(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        doReturn(windowManager).`when`(context).getSystemService(Context.WINDOW_SERVICE)
        val popupView = spy(CFRPopupFullscreenLayout("test", anchor, mock(), { }, { }))
        `when`(popupView.isAttachedToWindow).thenReturn(false)
        popupView.show()
        assertNotNull(popupView.findViewTreeLifecycleOwner())
        assertNotNull(popupView.findViewTreeSavedStateRegistryOwner())
        `when`(popupView.isAttachedToWindow).thenReturn(true)
        popupView.dismiss()

        assertNull(popupView.findViewTreeLifecycleOwner())
        assertNull(popupView.findViewTreeSavedStateRegistryOwner())
        verify(windowManager).removeViewImmediate(popupView)
    }

    @Test
    fun `GIVEN a popup WHEN adding it to window THEN use translucent layout params`() {
        val context = spy(testContext)
        val anchor = View(context).apply {
            setViewTreeLifecycleOwner(mock())
            this.setViewTreeSavedStateRegistryOwner(mock())
        }
        val windowManager = spy(context.getSystemService(Context.WINDOW_SERVICE))
        doReturn(windowManager).`when`(context).getSystemService(Context.WINDOW_SERVICE)
        val popupView = spy(CFRPopupFullscreenLayout("test", anchor, mock(), { }, { }))
        val layoutParamsCaptor = argumentCaptor<LayoutParams>()

        popupView.show()

        verify(windowManager as ViewManager).addView(mozilla.components.support.test.eq(popupView), layoutParamsCaptor.capture())
        assertEquals(LayoutParams.TYPE_APPLICATION_PANEL, layoutParamsCaptor.value.type)
        assertEquals(anchor.applicationWindowToken, layoutParamsCaptor.value.token)
        assertEquals(LayoutParams.MATCH_PARENT, layoutParamsCaptor.value.width)
        assertEquals(LayoutParams.MATCH_PARENT, layoutParamsCaptor.value.height)
        assertEquals(PixelFormat.TRANSLUCENT, layoutParamsCaptor.value.format)
        assertEquals(
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_HARDWARE_ACCELERATED,
            layoutParamsCaptor.value.flags,
        )
    }

    @Test
    fun `WHEN creating layout params THEN get fullscreen translucent layout params`() {
        val anchor = View(testContext)
        val popupView = spy(CFRPopupFullscreenLayout("test", anchor, mock(), { }, { }))

        val result = popupView.createLayoutParams()

        assertEquals(LayoutParams.TYPE_APPLICATION_PANEL, result.type)
        assertEquals(anchor.applicationWindowToken, result.token)
        assertEquals(LayoutParams.MATCH_PARENT, result.width)
        assertEquals(LayoutParams.MATCH_PARENT, result.height)
        assertEquals(PixelFormat.TRANSLUCENT, result.format)
        assertEquals(
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_HARDWARE_ACCELERATED,
            result.flags,
        )
    }
}
