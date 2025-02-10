/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.gecko

import android.content.Context
import androidx.core.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mozilla.components.browser.engine.gecko.autofill.GeckoAutocompleteStorageDelegate
import mozilla.components.browser.engine.gecko.ext.toContentBlockingSetting
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.storage.CreditCardsAddressesStorage
import mozilla.components.concept.storage.LoginsStorage
import mozilla.components.lib.crash.handler.CrashHandlerService
import mozilla.components.service.sync.autofill.GeckoCreditCardsAddressesStorageDelegate
import mozilla.components.service.sync.logins.GeckoLoginStorageDelegate
import net.waterfox.android.Config
import net.waterfox.android.ext.components
import net.waterfox.android.ext.settings
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

object GeckoProvider {
    private var runtime: GeckoRuntime? = null

    /**
     * Path to geckoview config in app assets
     */
    private const val geckoViewConfigAssetPath = "geckoview-config.yaml"
    /**
     * Path to geckoview config in cache dir.
     */
    private const val geckoViewConfigCachePath = "geckoview-config-v1.yaml"

    /**
     * Import a geckoview config YAML file from assets and return a File that the GeckoRuntime
     * can load it from.
     */
    private suspend fun importGeckoConfig(context: Context): File = withContext(Dispatchers.IO) {
        val configFile = File(context.cacheDir, geckoViewConfigCachePath)
        if (!configFile.exists()) {
            val configAssets = context.assets.open(geckoViewConfigAssetPath)
            val atomicConfigFile = AtomicFile(configFile)
            val writeStream = atomicConfigFile.startWrite()
            configAssets.copyTo(writeStream)
            configAssets.close()
            atomicConfigFile.finishWrite(writeStream)
        }
        configFile
    }

    @Synchronized
    fun getOrCreateRuntime(
        context: Context,
        autofillStorage: Lazy<CreditCardsAddressesStorage>,
        loginStorage: Lazy<LoginsStorage>,
        trackingProtectionPolicy: TrackingProtectionPolicy
    ): GeckoRuntime {
        if (runtime == null) {
            runtime =
                createRuntime(context, autofillStorage, loginStorage, trackingProtectionPolicy)
        }

        return runtime!!
    }

    private fun createRuntime(
        context: Context,
        autofillStorage: Lazy<CreditCardsAddressesStorage>,
        loginStorage: Lazy<LoginsStorage>,
        policy: TrackingProtectionPolicy
    ): GeckoRuntime {
        val settings = context.components.settings
        val builder = GeckoRuntimeSettings.Builder()

        val runtimeSettings = builder
            .crashHandler(CrashHandlerService::class.java)
            .contentBlocking(policy.toContentBlockingSetting())
            .debugLogging(Config.channel.isDebug)
            .aboutConfigEnabled(true)
            .configFilePath(
                if (settings.shouldUseDNSOverObliviousHTTP) {
                    runBlocking { importGeckoConfig(context).absolutePath }
                } else {
                    ""
                }
            )
            .extensionsWebAPIEnabled(true)
            .extensionsProcessEnabled(true)
            .build()

        if (!settings.shouldUseAutoSize) {
            runtimeSettings.automaticFontSizeAdjustment = false
            val fontSize = settings.fontSizeFactor
            runtimeSettings.fontSizeFactor = fontSize
        }

        val geckoRuntime = GeckoRuntime.create(context, runtimeSettings)

        geckoRuntime.autocompleteStorageDelegate = GeckoAutocompleteStorageDelegate(
            GeckoCreditCardsAddressesStorageDelegate(
                storage = autofillStorage,
                isCreditCardAutofillEnabled = { context.settings().shouldAutofillCreditCardDetails },
                isAddressAutofillEnabled = { context.settings().shouldAutofillAddressDetails },
            ),
            GeckoLoginStorageDelegate(
                loginStorage = loginStorage,
                isLoginAutofillEnabled = { context.settings().shouldAutofillLogins },
            ),
        )

        return geckoRuntime
    }
}
