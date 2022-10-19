/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner

@RunWith(WaterfoxRobolectricTestRunner::class)
class SelectTabUseCaseWrapperTest {

    val selectUseCase: TabsUseCases.SelectTabUseCase = mockk(relaxed = true)

    @Test
    fun `WHEN invoked with no source name THEN use case and callback are triggered`() {
        var invoked = ""
        val onSelect: (String) -> Unit = { invoked = it }
        val wrapper = SelectTabUseCaseWrapper(selectUseCase, onSelect)

        wrapper("123")

        verify { selectUseCase("123") }
        assertEquals("123", invoked)
    }

    @Test
    fun `WHEN invoked with a source name THEN use case and callback are triggered`() {
        var invoked = ""
        val onSelect: (String) -> Unit = { invoked = it }
        val wrapper = SelectTabUseCaseWrapper(selectUseCase, onSelect)

        wrapper("123", "Test")

        verify { selectUseCase("123") }
        assertEquals("123", invoked)
    }
}
