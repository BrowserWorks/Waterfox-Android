/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.bookmarks.viewholders

import androidx.appcompat.content.res.AppCompatResources
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.waterfox.android.ext.components
import net.waterfox.android.ext.hideAndDisable
import net.waterfox.android.ext.showAndEnable
import net.waterfox.android.library.LibrarySiteItemView
import net.waterfox.android.library.bookmarks.BookmarkFragmentInteractor
import net.waterfox.android.library.bookmarks.BookmarkFragmentState
import net.waterfox.android.library.bookmarks.BookmarkPayload

class BookmarkNodeViewHolderTest {

    @MockK private lateinit var interactor: BookmarkFragmentInteractor
    @MockK(relaxed = true) private lateinit var siteItemView: LibrarySiteItemView
    @MockK private lateinit var icons: BrowserIcons
    private lateinit var holder: BookmarkNodeViewHolder

    private val item = BookmarkNode(
        type = BookmarkNodeType.ITEM,
        guid = "456",
        parentGuid = "123",
        position = 0u,
        title = "Mozilla",
        url = "https://www.mozilla.org",
        dateAdded = 0,
        children = listOf()
    )
    private val folder = BookmarkNode(
        type = BookmarkNodeType.FOLDER,
        guid = "456",
        parentGuid = "123",
        position = 0u,
        title = "Folder",
        url = null,
        dateAdded = 0,
        children = listOf()
    )

    private val falsePayload = BookmarkPayload(
        titleChanged = false,
        urlChanged = false,
        selectedChanged = false,
        modeChanged = false,
        iconChanged = false
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true)
        every { siteItemView.context.components.core.icons } returns icons
        every { icons.loadIntoView(siteItemView.iconView, any()) } returns mockk()

        holder = BookmarkNodeViewHolder(siteItemView, interactor)
    }

    @After
    fun teardown() {
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun `binds views for unselected item`() {
        val mode = BookmarkFragmentState.Mode.Normal()
        holder.bind(item, mode, BookmarkPayload())

        verify {
            siteItemView.setSelectionInteractor(item, mode, interactor)
            siteItemView.titleView.text = item.title
            siteItemView.urlView.text = item.url
            siteItemView.overflowView.showAndEnable()
            siteItemView.changeSelected(false)
            icons.loadIntoView(siteItemView.iconView, IconRequest(item.url!!))
        }
    }

    @Test
    fun `binds views for selected item for item`() {
        val mode = BookmarkFragmentState.Mode.Selecting(setOf(item))
        holder.bind(item, mode, BookmarkPayload())

        verify {
            siteItemView.setSelectionInteractor(item, mode, interactor)
            siteItemView.titleView.text = item.title
            siteItemView.urlView.text = item.url
            siteItemView.overflowView.hideAndDisable()
            siteItemView.changeSelected(true)
        }
    }

    @Test
    fun `bind with payload of no changes does not rebind views for item`() {
        holder.bind(
            item,
            BookmarkFragmentState.Mode.Normal(),
            falsePayload
        )

        verify(inverse = true) {
            siteItemView.titleView.text = item.title
            siteItemView.urlView.text = item.url
            siteItemView.overflowView.showAndEnable()
            siteItemView.overflowView.hideAndDisable()
            siteItemView.changeSelected(any())
        }
        verify { siteItemView.iconView wasNot Called }
    }

    @Test
    fun `binding an item with a null title uses the url as the title for item`() {
        val item = item.copy(title = null)
        holder.bind(item, BookmarkFragmentState.Mode.Normal(), BookmarkPayload())

        verify { siteItemView.titleView.text = item.url }
    }

    @Test
    fun `binding an item with a blank title uses the url as the title for item`() {
        val item = item.copy(title = " ")
        holder.bind(item, BookmarkFragmentState.Mode.Normal(), BookmarkPayload())

        verify { siteItemView.titleView.text = item.url }
    }

    @Test
    fun `rebinds title if item title is null and the item url has changed for item`() {
        val item = item.copy(title = null)
        holder.bind(
            item,
            BookmarkFragmentState.Mode.Normal(),
            BookmarkPayload(
                titleChanged = false,
                urlChanged = true,
                selectedChanged = false,
                modeChanged = false,
                iconChanged = false
            )
        )

        verify { siteItemView.titleView.text = item.url }
    }

    @Test
    fun `rebinds title if item title is blank and the item url has changed for item`() {
        val item = item.copy(title = " ")
        holder.bind(
            item,
            BookmarkFragmentState.Mode.Normal(),
            BookmarkPayload(
                titleChanged = false,
                urlChanged = true,
                selectedChanged = false,
                modeChanged = false,
                iconChanged = false
            )
        )

        verify { siteItemView.titleView.text = item.url }
    }

    @Test
    fun `binds title and selected state for folder`() {
        holder.bind(folder, BookmarkFragmentState.Mode.Normal(), BookmarkPayload())

        verify {
            siteItemView.titleView.text = folder.title
            siteItemView.overflowView.showAndEnable()
            siteItemView.changeSelected(false)
        }

        holder.bind(folder, BookmarkFragmentState.Mode.Selecting(setOf(folder)), BookmarkPayload())

        verify {
            siteItemView.titleView.text = folder.title
            siteItemView.overflowView.hideAndDisable()
            siteItemView.changeSelected(true)
        }
    }

    @Test
    fun `bind with payload of no changes does not rebind views for folder`() {
        holder.bind(
            folder,
            BookmarkFragmentState.Mode.Normal(),
            falsePayload
        )

        verify(inverse = true) {
            siteItemView.titleView.text = folder.title
            siteItemView.overflowView.showAndEnable()
            siteItemView.overflowView.hideAndDisable()
            siteItemView.changeSelected(any())
        }
    }
}
