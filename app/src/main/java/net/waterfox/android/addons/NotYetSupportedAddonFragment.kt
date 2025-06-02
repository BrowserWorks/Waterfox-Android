/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.addons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapter
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapterDelegate
import net.waterfox.android.R
import net.waterfox.android.databinding.FragmentNotYetSupportedAddonsBinding
import net.waterfox.android.ext.components
import net.waterfox.android.ext.showToolbar

private const val LEARN_MORE_URL =
    "https://support.mozilla.org/kb/add-compatibility-firefox-preview"

/**
 * Fragment for displaying and managing add-ons that are not yet supported by the browser.
 */
class NotYetSupportedAddonFragment :
    Fragment(R.layout.fragment_not_yet_supported_addons), UnsupportedAddonsAdapterDelegate {

    private val args by navArgs<NotYetSupportedAddonFragmentArgs>()
    private var unsupportedAddonsAdapter: UnsupportedAddonsAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        unsupportedAddonsAdapter = UnsupportedAddonsAdapter(
            addonManager = requireContext().components.addonManager,
            unsupportedAddonsAdapterDelegate = this@NotYetSupportedAddonFragment,
            addons = args.addons.toList()
        )

        val binding = FragmentNotYetSupportedAddonsBinding.bind(view)

        binding.unsupportedAddOnsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = unsupportedAddonsAdapter
        }

        binding.learnMoreLabel.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(LEARN_MORE_URL))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(mozilla.components.feature.addons.R.string.mozac_feature_addons_unavailable_section))
    }

    override fun onUninstallError(addonId: String, throwable: Throwable) {
        this@NotYetSupportedAddonFragment.view?.let { view ->
            showSnackBar(view, getString(mozilla.components.feature.addons.R.string.mozac_feature_addons_failed_to_remove, ""))
        }

        if (unsupportedAddonsAdapter?.itemCount == 0) {
            findNavController().popBackStack()
        }
    }

    override fun onUninstallSuccess() {
        this@NotYetSupportedAddonFragment.view?.let { view ->
            showSnackBar(view, getString(mozilla.components.feature.addons.R.string.mozac_feature_addons_successfully_removed, ""))
        }
    }
}
