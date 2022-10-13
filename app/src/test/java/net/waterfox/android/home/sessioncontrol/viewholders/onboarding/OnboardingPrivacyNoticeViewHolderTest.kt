/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders.onboarding

import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.GleanMetrics.Onboarding
import net.waterfox.android.R
import net.waterfox.android.databinding.OnboardingPrivacyNoticeBinding
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.home.sessioncontrol.OnboardingInteractor

@RunWith(WaterfoxRobolectricTestRunner::class)
class OnboardingPrivacyNoticeViewHolderTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    private lateinit var binding: OnboardingPrivacyNoticeBinding
    private lateinit var interactor: OnboardingInteractor

    @Before
    fun setup() {
        val context = ContextThemeWrapper(testContext, R.style.NormalTheme)
        binding = OnboardingPrivacyNoticeBinding.inflate(LayoutInflater.from(context))
        interactor = mockk(relaxed = true)
    }

    @Test
    fun `call interactor on click`() {
        every { testContext.components.analytics } returns mockk(relaxed = true)
        OnboardingPrivacyNoticeViewHolder(binding.root, interactor)

        binding.readButton.performClick()
        verify { interactor.onReadPrivacyNoticeClicked() }
        // Check if the event was recorded
        assertNotNull(Onboarding.privacyNotice.testGetValue())
        assertEquals(1, Onboarding.privacyNotice.testGetValue()!!.size)
        assertNull(Onboarding.privacyNotice.testGetValue()!!.single().extra)
    }
}
