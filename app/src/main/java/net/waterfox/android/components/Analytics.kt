/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.sentry.SentryService
import mozilla.components.lib.crash.service.CrashReporterService
import mozilla.components.lib.crash.service.MozillaSocorroService
import mozilla.components.service.nimbus.NimbusApi
import net.waterfox.android.*
import net.waterfox.android.experiments.createNimbus
import net.waterfox.android.gleanplumb.CustomAttributeProvider
import net.waterfox.android.gleanplumb.NimbusMessagingStorage
import net.waterfox.android.gleanplumb.OnDiskMessageMetadataStorage
import net.waterfox.android.nimbus.FxNimbus
import net.waterfox.android.perf.lazyMonitored
import org.mozilla.geckoview.BuildConfig.*

/**
 * Component group for all functionality related to analytics e.g. crash reporting.
 */
class Analytics(
    private val context: Context
) {
    val crashReporter: CrashReporter by lazyMonitored {
        val services = mutableListOf<CrashReporterService>()
        val distributionId = when (Config.channel.isMozillaOnline) {
            true -> "MozillaOnline"
            false -> "Mozilla"
        }

        // TODO: [Waterfox] Do we want to keep Sentry?
        if (isSentryEnabled()) {
            // We treat caught exceptions similar to debug logging.
            // On the release channel volume of these is too high for our Sentry instances, and
            // we get most value out of nightly/beta logging anyway.
            val shouldSendCaughtExceptions = when (Config.channel) {
                ReleaseChannel.Release -> false
                else -> true
            }
            val sentryService = SentryService(
                context,
                BuildConfig.SENTRY_TOKEN,
                tags = mapOf(
                    "geckoview" to "$MOZ_APP_VERSION-$MOZ_APP_BUILDID",
                    "waterfox.git" to BuildConfig.GIT_HASH,
                ),
                environment = BuildConfig.BUILD_TYPE,
                sendEventForNativeCrashes = false, // Do not send native crashes to Sentry
                sendCaughtExceptions = shouldSendCaughtExceptions,
                sentryProjectUrl = getSentryProjectUrl()
            )

            services.add(sentryService)
        }

        // The name "Waterfox" here matches the product name on Socorro and is unrelated to the actual app name:
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1523284
        val socorroService = MozillaSocorroService(
            context, appName = "Waterfox",
            version = MOZ_APP_VERSION, buildId = MOZ_APP_BUILDID, vendor = MOZ_APP_VENDOR,
            releaseChannel = MOZ_UPDATE_CHANNEL, distributionId = distributionId
        )
        services.add(socorroService)

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val crashReportingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0 // No flags. Default behavior.
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            crashReportingIntentFlags
        )

        CrashReporter(
            context = context,
            services = services,
            shouldPrompt = CrashReporter.Prompt.ALWAYS,
            promptConfiguration = CrashReporter.PromptConfiguration(
                appName = context.getString(R.string.app_name),
                organizationName = "Mozilla"
            ),
            enabled = true,
            nonFatalCrashIntent = pendingIntent
        )
    }

    val experiments: NimbusApi by lazyMonitored {
        createNimbus(context, BuildConfig.NIMBUS_ENDPOINT)
    }

    val messagingStorage by lazyMonitored {
        NimbusMessagingStorage(
            context = context,
            metadataStorage = OnDiskMessageMetadataStorage(context),
            gleanPlumb = experiments,
            reportMalformedMessage = {},
            messagingFeature = FxNimbus.features.messaging,
            attributeProvider = CustomAttributeProvider,
        )
    }
}

private fun isSentryEnabled() = !BuildConfig.SENTRY_TOKEN.isNullOrEmpty()

private fun getSentryProjectUrl(): String? {
    val baseUrl = "https://sentry.io/organizations/mozilla/issues"
    return when (Config.channel) {
        ReleaseChannel.Release -> "$baseUrl/?project=6375561"
        else -> null
    }
}
