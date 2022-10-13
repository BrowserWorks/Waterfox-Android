/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.exceptions.login

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.R
import net.waterfox.android.exceptions.ExceptionsAdapter
import net.waterfox.android.exceptions.viewholders.ExceptionsDeleteButtonViewHolder
import net.waterfox.android.exceptions.viewholders.ExceptionsHeaderViewHolder
import net.waterfox.android.exceptions.viewholders.ExceptionsListItemViewHolder
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner

@RunWith(WaterfoxRobolectricTestRunner::class)
class LoginExceptionsAdapterTest {

    private lateinit var interactor: LoginExceptionsInteractor
    private lateinit var adapter: LoginExceptionsAdapter
    private lateinit var context: Context

    @Before
    fun setup() {
        interactor = mockk()
        adapter = LoginExceptionsAdapter(interactor)
        context = ContextThemeWrapper(testContext, R.style.NormalTheme)
    }

    @Test
    fun `creates correct view holder type`() {
        every { testContext.components.core.icons } returns BrowserIcons(testContext, mockk(relaxed = true))
        val parent = FrameLayout(context)
        adapter.updateData(listOf(mockk(), mockk()))
        assertEquals(4, adapter.itemCount)

        val holders = (0 until adapter.itemCount).asSequence()
            .map { i -> adapter.getItemViewType(i) }
            .map { viewType -> adapter.onCreateViewHolder(parent, viewType) }
            .toList()
        assertEquals(4, holders.size)

        assertTrue(holders[0] is ExceptionsHeaderViewHolder)
        assertTrue(holders[1] is ExceptionsListItemViewHolder<*>)
        assertTrue(holders[2] is ExceptionsListItemViewHolder<*>)
        assertTrue(holders[3] is ExceptionsDeleteButtonViewHolder)
    }

    @Test
    fun `headers and delete should check if the other object is the same`() {
        assertTrue(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                ExceptionsAdapter.AdapterItem.Header,
                ExceptionsAdapter.AdapterItem.Header
            )
        )
        assertTrue(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                ExceptionsAdapter.AdapterItem.DeleteButton,
                ExceptionsAdapter.AdapterItem.DeleteButton
            )
        )
        assertFalse(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                ExceptionsAdapter.AdapterItem.Header,
                ExceptionsAdapter.AdapterItem.DeleteButton
            )
        )
        assertTrue(
            LoginExceptionsAdapter.DiffCallback.areContentsTheSame(
                ExceptionsAdapter.AdapterItem.Header,
                ExceptionsAdapter.AdapterItem.Header
            )
        )
        assertTrue(
            LoginExceptionsAdapter.DiffCallback.areContentsTheSame(
                ExceptionsAdapter.AdapterItem.DeleteButton,
                ExceptionsAdapter.AdapterItem.DeleteButton
            )
        )
        assertFalse(
            LoginExceptionsAdapter.DiffCallback.areContentsTheSame(
                ExceptionsAdapter.AdapterItem.DeleteButton,
                ExceptionsAdapter.AdapterItem.Header
            )
        )
    }

    @Test
    fun `items with the same id should be marked as same`() {
        assertTrue(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                LoginExceptionsAdapter.LoginAdapterItem(
                    mockk {
                        every { id } returns 12L
                    }
                ),
                LoginExceptionsAdapter.LoginAdapterItem(
                    mockk {
                        every { id } returns 12L
                    }
                )
            )
        )
        assertFalse(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                LoginExceptionsAdapter.LoginAdapterItem(
                    mockk {
                        every { id } returns 14L
                    }
                ),
                LoginExceptionsAdapter.LoginAdapterItem(
                    mockk {
                        every { id } returns 12L
                    }
                )
            )
        )
        assertFalse(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                LoginExceptionsAdapter.LoginAdapterItem(
                    mockk {
                        every { id } returns 14L
                    }
                ),
                ExceptionsAdapter.AdapterItem.Header
            )
        )
        assertFalse(
            LoginExceptionsAdapter.DiffCallback.areItemsTheSame(
                ExceptionsAdapter.AdapterItem.DeleteButton,
                LoginExceptionsAdapter.LoginAdapterItem(
                    mockk {
                        every { id } returns 14L
                    }
                )
            )
        )
    }
}
