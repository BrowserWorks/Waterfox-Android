/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.topsites

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.databinding.TopSiteItemBinding
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.home.sessioncontrol.TopSiteInteractor
import org.junit.Assert.assertTrue

@RunWith(WaterfoxRobolectricTestRunner::class)
class TopSiteItemViewHolderTest {

    private lateinit var binding: TopSiteItemBinding
    private lateinit var interactor: TopSiteInteractor
    private lateinit var lifecycleOwner: LifecycleOwner

    private val topSite = TopSite.Default(
        id = 1L,
        title = "Baidu",
        url = "https://www.baidu.com/",
        createdAt = 0
    )

    @Before
    fun setup() {
        binding = TopSiteItemBinding.inflate(LayoutInflater.from(testContext))
        interactor = mockk(relaxed = true)
        lifecycleOwner = mockk(relaxed = true)

        every { testContext.components.core.icons } returns BrowserIcons(testContext, mockk(relaxed = true))
    }

    @Test
    fun `calls interactor on click`() {
        TopSiteItemViewHolder(binding.root, lifecycleOwner, interactor).bind(topSite, position = 0)

        binding.topSiteItem.performClick()
        verify { interactor.onSelectTopSite(topSite, position = 0) }
    }

    @Test
    fun `calls interactor on long click`() {
        every { testContext.components.analytics } returns mockk(relaxed = true)
        TopSiteItemViewHolder(binding.root, lifecycleOwner, interactor).bind(topSite, position = 0)

        binding.topSiteItem.performLongClick()
        verify { interactor.onTopSiteMenuOpened() }
    }

    @Test
    fun `GIVEN a default top site WHEN bind is called THEN the pin indicator is visible`() {
        val defaultTopSite = TopSite.Default(
            id = 1L,
            title = "Baidu",
            url = "https://www.baidu.com/",
            createdAt = 0
        )

        TopSiteItemViewHolder(binding.root, lifecycleOwner, interactor).bind(defaultTopSite, position = 0)

        assertTrue(binding.topSitePin.isVisible)
    }

    @Test
    fun `GIVEN a pinned top site WHEN bind is called THEN the pin indicator is visible`() {
        val pinnedTopSite = TopSite.Pinned(
            id = 1L,
            title = "Mozilla",
            url = "https://www.mozilla.org",
            createdAt = 0
        )

        TopSiteItemViewHolder(binding.root, lifecycleOwner, interactor).bind(pinnedTopSite, position = 0)

        assertTrue(binding.topSitePin.isVisible)
    }

    @Test
    fun `GIVEN a frecent top site WHEN bind is called THEN the title does not have a pin indicator`() {
        val frecentTopSite = TopSite.Frecent(
            id = 1L,
            title = "Mozilla",
            url = "https://www.mozilla.org",
            createdAt = 0
        )

        TopSiteItemViewHolder(binding.root, lifecycleOwner, interactor).bind(frecentTopSite, position = 0)
        val pinIndicator = binding.topSiteTitle.compoundDrawables[0]

        assertNull(pinIndicator)
    }
}
