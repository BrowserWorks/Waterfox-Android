/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.addons

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import mozilla.components.feature.addons.ui.translateName
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.databinding.FragmentAddOnPermissionsBinding
import net.waterfox.android.ext.showToolbar

/**
 * A fragment to show the permissions of an add-on.
 */
class AddonPermissionsDetailsFragment :
    Fragment(R.layout.fragment_add_on_permissions),
    AddonPermissionsDetailsInteractor {

    private val args by navArgs<AddonPermissionsDetailsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAddOnPermissionsBinding.bind(view)
        AddonPermissionDetailsBindingDelegate(binding, interactor = this).bind(args.addon)
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            showToolbar(title = args.addon.translateName(it))
        }
    }

    override fun openWebsite(addonSiteUrl: Uri) {
        (activity as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = addonSiteUrl.toString(),
            newTab = true,
            from = BrowserDirection.FromAddonPermissionsDetailsFragment
        )
    }
}
