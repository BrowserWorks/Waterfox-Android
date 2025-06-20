/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.addons

import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import androidx.core.view.isVisible
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.feature.addons.Addon
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.test.robolectric.testContext
import net.waterfox.android.R
import net.waterfox.android.databinding.FragmentAddOnDetailsBinding
import net.waterfox.android.helpers.WaterfoxRobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(WaterfoxRobolectricTestRunner::class)
class AddonDetailsBindingDelegateTest {

    private lateinit var view: View
    private lateinit var binding: FragmentAddOnDetailsBinding
    private lateinit var interactor: AddonDetailsInteractor
    private lateinit var detailsBindingDelegate: AddonDetailsBindingDelegate
    private val baseAddon = Addon(
        id = "",
        translatableDescription = mapOf(
            Addon.DEFAULT_LOCALE to "Some blank addon\nwith a blank line",
        ),
        updatedAt = "2020-11-23T08:00:00Z",
    )

    @Before
    fun setup() {
        binding = FragmentAddOnDetailsBinding.inflate(LayoutInflater.from(testContext))
        view = binding.root
        interactor = mockk(relaxed = true)

        detailsBindingDelegate = AddonDetailsBindingDelegate(binding, interactor)
    }

    @Test
    fun `bind addons rating`() {
        detailsBindingDelegate.bind(
            baseAddon.copy(
                rating = null,
            ),
        )
        assertEquals(0f, binding.ratingView.rating)

        detailsBindingDelegate.bind(
            baseAddon.copy(
                rating = Addon.Rating(
                    average = 4.3f,
                    reviews = 100,
                ),
            ),
        )
        assertEquals(4.5f, binding.ratingView.rating)
        assertEquals("100", binding.reviewCount.text)

        val ratingContentDescription = testContext.getString(mozilla.components.feature.addons.R.string.mozac_feature_addons_rating_content_description_2)
        var formattedRatting = String.format(ratingContentDescription, 4.3f)
        assertEquals(formattedRatting, binding.ratingLabel.contentDescription)
        assertEquals(IMPORTANT_FOR_ACCESSIBILITY_NO, binding.ratingView.importantForAccessibility)

        val reviewContentDescription = testContext.getString(mozilla.components.feature.addons.R.string.mozac_feature_addons_user_rating_count_2)
        formattedRatting = String.format(reviewContentDescription, 100)
        assertEquals(formattedRatting, binding.reviewCount.contentDescription)
    }

    @Test
    fun `bind addons rating with review url`() {
        detailsBindingDelegate.bind(
            baseAddon.copy(
                rating = Addon.Rating(average = 4.3f, reviews = 100),
                ratingUrl = "https://example.org/",
            ),
        )
        assertEquals("100", binding.reviewCount.text.toString())

        binding.reviewCount.performClick()

        verify { interactor.openWebsite(Uri.parse("https://example.org/")) }
    }

    @Test
    fun `bind addons homepage`() {
        detailsBindingDelegate.bind(
            baseAddon.copy(
                homepageUrl = "https://mozilla.org",
            ),
        )

        binding.homePageLabel.performClick()

        verify { interactor.openWebsite(Uri.parse("https://mozilla.org")) }
    }

    @Test
    fun `bind addons last updated`() {
        detailsBindingDelegate.bind(baseAddon)

        assertEquals("Nov 23, 2020", binding.lastUpdatedText.text)
        val expectedContentDescription = binding.lastUpdatedLabel.text.toString() + " " + "Nov 23, 2020"
        assertEquals(expectedContentDescription, binding.lastUpdatedLabel.contentDescription)
        assertEquals(IMPORTANT_FOR_ACCESSIBILITY_NO, binding.lastUpdatedText.importantForAccessibility)
    }

    @Test
    fun `bind addons version`() {
        val addon1 = baseAddon.copy(
            version = "1.0.0",
            installedState = null,
        )

        detailsBindingDelegate.bind(addon1)
        assertEquals("1.0.0", binding.versionText.text)
        binding.versionText.performLongClick()
        verify(exactly = 0) { interactor.showUpdaterDialog(addon1) }

        val addon2 = baseAddon.copy(
            version = "1.0.0",
            installedState = Addon.InstalledState(
                id = "",
                version = "2.0.0",
                optionsPageUrl = null,
            ),
        )
        detailsBindingDelegate.bind(addon2)
        assertEquals("2.0.0", binding.versionText.text)
        binding.versionText.performLongClick()
        verify { interactor.showUpdaterDialog(addon2) }
        val expectedContentDescription = binding.versionLabel.text.toString() + " 2.0.0"
        assertEquals(expectedContentDescription, binding.versionLabel.contentDescription)
        assertEquals(IMPORTANT_FOR_ACCESSIBILITY_NO, binding.versionText.importantForAccessibility)
    }

    @Test
    fun `bind addons author`() {
        detailsBindingDelegate.bind(
            baseAddon.copy(author = Addon.Author(name = "Sarah Jane", url = "")),
        )

        assertEquals("Sarah Jane", binding.authorText.text)
        assertNotEquals(testContext.getColorFromAttr(R.attr.textAccent), binding.authorText.currentTextColor)
        val expectedContentDescription = binding.authorLabel.text.toString() + " Sarah Jane"
        assertEquals(expectedContentDescription, binding.authorLabel.contentDescription)
        assertEquals(IMPORTANT_FOR_ACCESSIBILITY_NO, binding.authorText.importantForAccessibility)
    }

    @Test
    fun `bind addons author with url`() {
        detailsBindingDelegate.bind(
            baseAddon.copy(author = Addon.Author(name = "Sarah Jane", url = "https://example.org/")),
        )

        assertEquals("Sarah Jane", binding.authorText.text.toString())
        assertEquals(testContext.getColorFromAttr(R.attr.textAccent), binding.authorText.currentTextColor)

        binding.authorText.performClick()

        verify { interactor.openWebsite(Uri.parse("https://example.org/")) }
    }

    @Test
    fun `bind addons details`() {
        detailsBindingDelegate.bind(baseAddon)

        assertEquals(
            "Some blank addon\nwith a blank line",
            binding.details.text.toString(),
        )
        assertTrue(binding.details.movementMethod is LinkMovementMethod)
    }

    @Test
    fun `bind without last updated date`() {
        detailsBindingDelegate.bind(baseAddon.copy(updatedAt = ""))

        assertFalse(binding.lastUpdatedLabel.isVisible)
        assertFalse(binding.lastUpdatedText.isVisible)
        assertFalse(binding.lastUpdatedDivider.isVisible)
    }

    @Test
    fun `bind without author`() {
        detailsBindingDelegate.bind(baseAddon.copy(author = null))

        assertFalse(binding.authorLabel.isVisible)
        assertFalse(binding.authorText.isVisible)
        assertFalse(binding.authorDivider.isVisible)
    }

    @Test
    fun `bind without a home page`() {
        detailsBindingDelegate.bind(baseAddon.copy(homepageUrl = ""))

        assertFalse(binding.homePageLabel.isVisible)
        assertFalse(binding.homePageDivider.isVisible)
    }

    @Test
    fun `bind add-on detail url`() {
        detailsBindingDelegate.bind(baseAddon.copy(detailUrl = "https://example.org"))

        assertTrue(binding.detailUrl.isVisible)
        assertTrue(binding.detailUrlDivider.isVisible)

        binding.detailUrl.performClick()

        verify { interactor.openWebsite(Uri.parse("https://example.org")) }
    }

    @Test
    fun `bind add-on without a detail url`() {
        detailsBindingDelegate.bind(baseAddon.copy(detailUrl = ""))

        assertFalse(binding.detailUrl.isVisible)
        assertFalse(binding.detailUrlDivider.isVisible)
    }
}
