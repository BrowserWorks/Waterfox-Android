/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android

import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.webextension.DisabledFlags
import mozilla.components.concept.engine.webextension.Metadata
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.feature.addons.migration.DefaultSupportedAddonsChecker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.utils.BrowsersCache

@RunWith(WaterfoxRobolectricTestRunner::class)
class WaterfoxApplicationTest {

    private lateinit var application: WaterfoxApplication
    private lateinit var browsersCache: BrowsersCache
    private lateinit var browserStore: BrowserStore

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        browsersCache = mockk(relaxed = true)
        browserStore = BrowserStore()
    }

    @Test
    fun `GIVEN there are unsupported addons installed WHEN subscribing for new add-ons checks THEN register for checks`() {
        val checker = mockk<DefaultSupportedAddonsChecker>(relaxed = true)
        val unSupportedExtension: WebExtension = mockk()
        val metadata: Metadata = mockk()

        every { unSupportedExtension.getMetadata() } returns metadata
        every { metadata.disabledFlags } returns DisabledFlags.select(DisabledFlags.APP_SUPPORT)

        application.subscribeForNewAddonsIfNeeded(checker, listOf(unSupportedExtension))

        verify { checker.registerForChecks() }
    }

    @Test
    fun `GIVEN there are no unsupported addons installed WHEN subscribing for new add-ons checks THEN unregister for checks`() {
        val checker = mockk<DefaultSupportedAddonsChecker>(relaxed = true)
        val unSupportedExtension: WebExtension = mockk()
        val metadata: Metadata = mockk()

        every { unSupportedExtension.getMetadata() } returns metadata
        every { metadata.disabledFlags } returns DisabledFlags.select(DisabledFlags.USER)

        application.subscribeForNewAddonsIfNeeded(checker, listOf(unSupportedExtension))

        verify { checker.unregisterForChecks() }
    }
}
