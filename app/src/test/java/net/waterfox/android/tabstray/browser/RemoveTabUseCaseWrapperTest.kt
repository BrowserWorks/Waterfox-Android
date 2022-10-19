/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray.browser

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner

@RunWith(WaterfoxRobolectricTestRunner::class)
class RemoveTabUseCaseWrapperTest {

    @Test
    fun `WHEN invoked with no source name THEN use case and callback are triggered`() {
        var actualTabId: String? = null
        val onRemove: (String) -> Unit = { tabId ->
            actualTabId = tabId
        }
        val wrapper = RemoveTabUseCaseWrapper(onRemove)

        wrapper("123")

        assertEquals("123", actualTabId)
    }

    @Test
    fun `WHEN invoked with a source name THEN use case and callback are triggered`() {
        var actualTabId: String? = null
        val onRemove: (String) -> Unit = { tabId ->
            actualTabId = tabId
        }
        val wrapper = RemoveTabUseCaseWrapper(onRemove)

        wrapper("123", "Test")

        assertEquals("123", actualTabId)
    }
}
