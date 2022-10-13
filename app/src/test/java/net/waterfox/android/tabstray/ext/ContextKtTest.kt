/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.ext

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextKtTest {

    @Test
    fun `WHEN screen density is very low THEN numberOfGridColumns will still be a minimum of 2`() {
        mockkStatic("net.waterfox.android.tabstray.ext.ContextKt")

        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val displayMetrics = spyk<DisplayMetrics> {
            widthPixels = 1
            density = 1f
        }
        every { context.resources } returns resources
        every { resources.displayMetrics } returns displayMetrics

        val result = context.numberOfGridColumns

        assertEquals(2, result)

        unmockkStatic("net.waterfox.android.tabstray.ext.ContextKt")
    }
}
