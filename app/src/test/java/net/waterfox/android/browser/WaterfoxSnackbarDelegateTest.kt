/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.browser

import android.view.View
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import net.waterfox.android.R
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.helpers.MockkRetryTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WaterfoxSnackbarDelegateTest {

    @MockK(relaxed = true)
    private lateinit var view: View

    @MockK(relaxed = true)
    private lateinit var snackbar: WaterfoxSnackbar
    private lateinit var delegate: WaterfoxSnackbarDelegate

    @get:Rule
    val mockkRule = MockkRetryTestRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(WaterfoxSnackbar.Companion)

        delegate = WaterfoxSnackbarDelegate(view)
        every {
            WaterfoxSnackbar.make(view, LENGTH_LONG, isDisplayedWithBrowserToolbar = true)
        } returns snackbar
        every { snackbar.setText(any()) } returns snackbar
        every { snackbar.setAction(any(), any()) } returns snackbar
        every { view.context.getString(R.string.app_name) } returns "Waterfox"
        every { view.context.getString(R.string.edit) } returns "Edit"
    }

    @After
    fun teardown() {
        unmockkObject(WaterfoxSnackbar.Companion)
    }

    @Test
    fun `show with no listener nor action`() {
        delegate.show(
            snackBarParentView = view,
            text = R.string.app_name,
            duration = LENGTH_LONG,
            action = 0,
            listener = null
        )

        verify { snackbar.setText("Waterfox") }
        verify(exactly = 0) { snackbar.setAction(any(), any()) }
        verify { snackbar.show() }
    }

    @Test
    fun `show with listener but no action`() {
        delegate.show(
            snackBarParentView = view,
            text = R.string.app_name,
            duration = LENGTH_LONG,
            action = 0,
            listener = {}
        )

        verify { snackbar.setText("Waterfox") }
        verify(exactly = 0) { snackbar.setAction(any(), any()) }
        verify { snackbar.show() }
    }

    @Test
    fun `show with action but no listener`() {
        delegate.show(
            snackBarParentView = view,
            text = R.string.app_name,
            duration = LENGTH_LONG,
            action = R.string.edit,
            listener = null
        )

        verify { snackbar.setText("Waterfox") }
        verify(exactly = 0) { snackbar.setAction(any(), any()) }
        verify { snackbar.show() }
    }

    @Test
    fun `show with listener and action`() {
        val listener = mockk<(View) -> Unit>(relaxed = true)
        delegate.show(
            snackBarParentView = view,
            text = R.string.app_name,
            duration = LENGTH_LONG,
            action = R.string.edit,
            listener = listener
        )

        verify { snackbar.setText("Waterfox") }
        verify {
            snackbar.setAction(
                "Edit",
                withArg {
                    verify(exactly = 0) { listener(view) }
                    it.invoke()
                    verify { listener(view) }
                }
            )
        }
        verify { snackbar.show() }
    }
}
