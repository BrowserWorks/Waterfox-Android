/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.lib.crash.Crash
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.sentry.SentryService
import mozilla.components.lib.crash.service.CrashReporterService
import net.waterfox.android.*
import net.waterfox.android.ext.components
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

        if (isSentryEnabled()) {
            val shouldSendCaughtExceptions = Config.channel == ReleaseChannel.Release

            val sentryService = SentryService(
                context,
                BuildConfig.SENTRY_TOKEN,
                tags = mapOf(
                    "geckoview" to "$MOZ_APP_VERSION-$MOZ_APP_BUILDID",
                    "waterfox.git" to BuildConfig.GIT_HASH
                ),
                environment = BuildConfig.BUILD_TYPE,
                sendEventForNativeCrashes = false, // Do not send native crashes to Sentry
                sendCaughtExceptions = shouldSendCaughtExceptions,
                sentryProjectUrl = getSentryProjectUrl()
            )

            services.add(sentryService)
        } else {
            // At least one service needs to be added to the services list,
            // so we add a No Op implementation if Sentry is disabled (like in case of debug builds).
            services.add(NoOpCrashReporterService)
        }

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
                organizationName = "BrowserWorks"
            ),
            enabled = true,
            nonFatalCrashIntent = pendingIntent,
            notificationsDelegate = context.components.notificationsDelegate,
        )
    }
}

private fun isSentryEnabled() = !BuildConfig.SENTRY_TOKEN.isNullOrEmpty()

private fun getSentryProjectUrl() =
    if (Config.channel == ReleaseChannel.Release) "https://sentry.io/organizations/browserworks/issues/?project=4506314635280384"
    else null

object NoOpCrashReporterService : CrashReporterService {
    override val id: String = ""
    override val name: String = ""

    override fun createCrashReportUrl(identifier: String): String? = null
    override fun report(throwable: Throwable, breadcrumbs: ArrayList<Breadcrumb>): String? = null
    override fun report(crash: Crash.NativeCodeCrash): String? = null
    override fun report(crash: Crash.UncaughtExceptionCrash): String? = null
}
