/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.about.viewholders

import android.view.LayoutInflater
import android.view.View
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.settings.about.AboutItem
import net.waterfox.android.settings.about.AboutPageItem
import net.waterfox.android.settings.about.AboutPageListener

@RunWith(WaterfoxRobolectricTestRunner::class)
class AboutItemViewHolderTest {

    private val item = AboutPageItem(AboutItem.Libraries, "Libraries")
    private lateinit var view: View
    private lateinit var listener: AboutPageListener

    @Before
    fun setup() {
        view = LayoutInflater.from(testContext).inflate(AboutItemViewHolder.LAYOUT_ID, null)
        listener = mockk(relaxed = true)
    }

    @Test
    fun `bind title`() {
        val holder = AboutItemViewHolder(view, listener)
        holder.bind(item)

        assertEquals("Libraries", holder.binding.aboutItemTitle.text)
    }

    @Test
    fun `call listener on click`() {
        val holder = AboutItemViewHolder(view, listener)
        holder.bind(item)
        holder.itemView.performClick()

        verify { listener.onAboutItemClicked(AboutItem.Libraries) }
    }
}
