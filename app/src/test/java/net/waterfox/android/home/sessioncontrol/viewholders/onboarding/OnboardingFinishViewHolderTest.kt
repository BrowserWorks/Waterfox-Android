/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol.viewholders.onboarding

import android.view.LayoutInflater
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import net.waterfox.android.databinding.OnboardingFinishBinding
import net.waterfox.android.ext.components
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import net.waterfox.android.home.sessioncontrol.OnboardingInteractor

@RunWith(WaterfoxRobolectricTestRunner::class)
class OnboardingFinishViewHolderTest {

    private lateinit var binding: OnboardingFinishBinding
    private lateinit var interactor: OnboardingInteractor

    @Before
    fun setup() {
        binding = OnboardingFinishBinding.inflate(LayoutInflater.from(testContext))
        interactor = mockk(relaxed = true)
    }

    @Test
    fun `call interactor on click`() {
        every { testContext.components.analytics } returns mockk(relaxed = true)
        OnboardingFinishViewHolder(binding.root, interactor)

        binding.finishButton.performClick()
        verify { interactor.onStartBrowsingClicked() }
    }
}
