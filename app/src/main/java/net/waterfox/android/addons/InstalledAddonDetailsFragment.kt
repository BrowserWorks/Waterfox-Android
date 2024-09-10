/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.engine.webextension.EnableSource
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.translateName
import mozilla.components.support.base.log.logger.Logger
import net.waterfox.android.BuildConfig
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.databinding.FragmentInstalledAddOnDetailsBinding
import net.waterfox.android.ext.components
import net.waterfox.android.ext.runIfFragmentIsAttached
import net.waterfox.android.ext.showToolbar

/**
 * An activity to show the details of a installed add-on.
 */
@Suppress("LargeClass", "TooManyFunctions")
class InstalledAddonDetailsFragment : Fragment() {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal lateinit var addon: Addon
    internal val logger = Logger("InstalledAddonDetailsFragment")

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val binding get() = _binding!!

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Suppress("VariableNaming")
    internal var _binding: FragmentInstalledAddOnDetailsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (!::addon.isInitialized) {
            addon = AddonDetailsFragmentArgs.fromBundle(requireNotNull(arguments)).addon
        }

        setBindingAndBindUI(
            FragmentInstalledAddOnDetailsBinding.inflate(
                inflater,
                container,
                false,
            ),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindAddon()
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            showToolbar(title = addon.translateName(it))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun setBindingAndBindUI(binding: FragmentInstalledAddOnDetailsBinding) {
        _binding = binding
        bindUI()
    }

    @VisibleForTesting
    internal fun provideAddonManager() = requireContext().components.addonManager

    @VisibleForTesting
    internal fun bindAddon(dispatchers: CoroutineDispatcher = Dispatchers.IO) {
        lifecycleScope.launch(dispatchers) {
            try {
                val addons = provideAddonManager().getAddons()
                runIfFragmentIsAttached {
                    addons.find { addon.id == it.id }.let {
                        if (it == null) {
                            throw AddonManagerException(Exception("Addon ${addon.id} not found"))
                        } else {
                            withContext(Dispatchers.Main) {
                                addon = it
                                bindUI()
                            }
                        }
                        withContext(Dispatchers.Main) {
                            binding.addOnProgressBar.isVisible = false
                            binding.addonContainer.isVisible = true
                        }
                    }
                }
            } catch (e: AddonManagerException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    logger.error("Unable to bind addon", e)
                    showUnableToQueryAddonsMessage()
                }
            }
        }
    }

    @VisibleForTesting
    internal fun showUnableToQueryAddonsMessage() {
        runIfFragmentIsAttached {
            showSnackBar(
                binding.root,
                getString(R.string.mozac_feature_addons_failed_to_query_extensions),
            )
            findNavController().popBackStack()
        }
    }

    @VisibleForTesting
    internal fun bindUI() {
        bindEnableSwitch()
        bindSettings()
        bindDetails()
        bindPermissions()
        bindAllowInPrivateBrowsingSwitch()
        bindRemoveButton()
        bindReportButton()
    }

    @VisibleForTesting
    internal fun provideEnableSwitch() = binding.enableSwitch

    @VisibleForTesting
    internal fun providePrivateBrowsingSwitch() = binding.allowInPrivateBrowsingSwitch

    @VisibleForTesting
    @SuppressWarnings("LongMethod")
    internal fun bindEnableSwitch() {
        val switch = provideEnableSwitch()
        val privateBrowsingSwitch = providePrivateBrowsingSwitch()
        switch.isChecked = addon.isEnabled()
        // When the ad-on is blocklisted or not correctly signed, we do not want to enable the toggle switch
        // because users shouldn't be able to re-enable an add-on in this state.
        if (
            addon.isDisabledAsBlocklisted() ||
            addon.isDisabledAsNotCorrectlySigned() ||
            addon.isDisabledAsIncompatible()
        ) {
            switch.isEnabled = false
            return
        }
        switch.setOnCheckedChangeListener { v, isChecked ->
            val addonManager = v.context.components.addonManager
            switch.isClickable = false
            disableButtons()
            if (isChecked) {
                enableAddon(
                    addonManager,
                    onSuccess = {
                        runIfFragmentIsAttached {
                            this.addon = it
                            switch.isClickable = true
                            privateBrowsingSwitch.isVisible = it.isEnabled()
                            privateBrowsingSwitch.isChecked =
                                it.incognito != Addon.Incognito.NOT_ALLOWED && it.isAllowedInPrivateBrowsing()
                            binding.settings.isVisible = shouldSettingsBeVisible()
                            enableButtons()
                            context?.let {
                                showSnackBar(
                                    binding.root,
                                    getString(
                                        R.string.mozac_feature_addons_successfully_enabled,
                                        addon.translateName(it),
                                    ),
                                )
                            }
                        }
                    },
                    onError = {
                        runIfFragmentIsAttached {
                            switch.isClickable = true
                            enableButtons()
                            switch.isChecked = addon.isEnabled()
                            context?.let {
                                showSnackBar(
                                    binding.root,
                                    getString(
                                        R.string.mozac_feature_addons_failed_to_enable,
                                        addon.translateName(it),
                                    ),
                                )
                            }
                        }
                    },
                )
            } else {
                binding.settings.isVisible = false
                addonManager.disableAddon(
                    addon,
                    onSuccess = {
                        runIfFragmentIsAttached {
                            this.addon = it
                            switch.isClickable = true
                            privateBrowsingSwitch.isVisible = it.isEnabled()
                            enableButtons()
                            context?.let {
                                showSnackBar(
                                    binding.root,
                                    getString(
                                        R.string.mozac_feature_addons_successfully_disabled,
                                        addon.translateName(it),
                                    ),
                                )
                            }
                        }
                    },
                    onError = {
                        runIfFragmentIsAttached {
                            switch.isClickable = true
                            switch.isChecked = addon.isEnabled()
                            enableButtons()
                            context?.let {
                                showSnackBar(
                                    binding.root,
                                    getString(
                                        R.string.mozac_feature_addons_failed_to_disable,
                                        addon.translateName(it),
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun enableAddon(
        addonManager: AddonManager,
        onSuccess: (Addon) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        // If the addon is migrated from Fennec and supported in Fenix, for the addon to be enabled,
        // we need to also request the addon to be enabled as supported by the app
        if (addon.isSupported() && addon.isDisabledAsUnsupported()) {
            addonManager.enableAddon(
                addon,
                EnableSource.APP_SUPPORT,
                { enabledAddon ->
                    addonManager.enableAddon(enabledAddon, EnableSource.USER, onSuccess, onError)
                },
                onError,
            )
        } else {
            addonManager.enableAddon(addon, EnableSource.USER, onSuccess, onError)
        }
    }

    @VisibleForTesting
    internal fun bindAllowInPrivateBrowsingSwitch() {
        val switch = providePrivateBrowsingSwitch()
        switch.isVisible = addon.isEnabled()

        if (addon.incognito == Addon.Incognito.NOT_ALLOWED) {
            switch.isChecked = false
            switch.isEnabled = false
            switch.text = requireContext().getString(R.string.mozac_feature_addons_not_allowed_in_private_browsing)
            return
        }

        switch.isChecked = addon.isAllowedInPrivateBrowsing()

        switch.setOnCheckedChangeListener { v, isChecked ->
            val addonManager = v.context.components.addonManager
            switch.isClickable = false
            disableButtons()
            addonManager.setAddonAllowedInPrivateBrowsing(
                addon,
                isChecked,
                onSuccess = {
                    runIfFragmentIsAttached {
                        this.addon = it
                        switch.isClickable = true
                        enableButtons()
                    }
                },
                onError = {
                    runIfFragmentIsAttached {
                        switch.isChecked = addon.isAllowedInPrivateBrowsing()
                        switch.isClickable = true
                        enableButtons()
                    }
                },
            )
        }
    }

    private fun bindReportButton() {
        binding.reportAddOn.setOnClickListener {
            val shouldCreatePrivateSession = (activity as HomeActivity).browsingModeManager.mode.isPrivate

            it.context.components.useCases.tabsUseCases.selectOrAddTab(
                url = "${BuildConfig.AMO_BASE_URL}/android/feedback/addon/${addon.id}/",
                private = shouldCreatePrivateSession,
                ignoreFragment = true,
            )

            // Send user to the newly open tab.
            Navigation.findNavController(it).navigate(
                InstalledAddonDetailsFragmentDirections.actionGlobalBrowser(null),
            )
        }
    }

    private fun bindSettings() {
        binding.settings.apply {
            isVisible = shouldSettingsBeVisible()
            setOnClickListener {
                val settingUrl = addon.installedState?.optionsPageUrl ?: return@setOnClickListener
                val directions = if (addon.installedState?.openOptionsPageInTab == true) {
                    val components = it.context.components
                    val shouldCreatePrivateSession =
                        (activity as HomeActivity).browsingModeManager.mode.isPrivate

                    // If the addon settings page is already open in a tab, select that one
                    components.useCases.tabsUseCases.selectOrAddTab(
                        url = settingUrl,
                        private = shouldCreatePrivateSession,
                        ignoreFragment = true,
                    )

                    InstalledAddonDetailsFragmentDirections.actionGlobalBrowser(null)
                } else {
                    InstalledAddonDetailsFragmentDirections
                        .actionInstalledAddonFragmentToAddonInternalSettingsFragment(addon)
                }
                Navigation.findNavController(this).navigate(directions)
            }
        }
    }

    private fun bindDetails() {
        binding.details.setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonDetailsFragment(
                    addon,
                )
            Navigation.findNavController(binding.root).navigate(directions)
        }
    }

    private fun bindPermissions() {
        binding.permissions.setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonPermissionsDetailsFragment(
                    addon,
                )
            Navigation.findNavController(binding.root).navigate(directions)
        }
    }

    private fun bindRemoveButton() {
        binding.removeAddOn.setOnClickListener {
            setAllInteractiveViewsClickable(binding, false)
            requireContext().components.addonManager.uninstallAddon(
                addon,
                onSuccess = {
                    runIfFragmentIsAttached {
                        setAllInteractiveViewsClickable(binding, true)
                        context?.let {
                            showSnackBar(
                                binding.root,
                                getString(
                                    R.string.mozac_feature_addons_successfully_uninstalled,
                                    addon.translateName(it),
                                ),
                            )
                        }
                        binding.root.findNavController().popBackStack()
                    }
                },
                onError = { _, _ ->
                    runIfFragmentIsAttached {
                        setAllInteractiveViewsClickable(binding, true)
                        context?.let {
                            showSnackBar(
                                binding.root,
                                getString(
                                    R.string.mozac_feature_addons_failed_to_uninstall,
                                    addon.translateName(it),
                                ),
                            )
                        }
                    }
                },
            )
        }
    }

    private fun setAllInteractiveViewsClickable(
        binding: FragmentInstalledAddOnDetailsBinding,
        clickable: Boolean,
    ) {
        binding.enableSwitch.isClickable = clickable
        binding.settings.isClickable = clickable
        binding.details.isClickable = clickable
        binding.permissions.isClickable = clickable
        binding.removeAddOn.isClickable = clickable
        binding.reportAddOn.isClickable = clickable
    }

    private fun enableButtons() {
        binding.removeAddOn.isEnabled = true
        binding.reportAddOn.isEnabled = true
    }

    private fun disableButtons() {
        binding.removeAddOn.isEnabled = false
        binding.reportAddOn.isEnabled = false
    }

    private fun shouldSettingsBeVisible() = !addon.installedState?.optionsPageUrl.isNullOrEmpty()
}
