/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android

import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode
import android.util.Log.INFO
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.work.Configuration.Builder
import androidx.work.Configuration.Provider
import kotlinx.coroutines.*
import mozilla.appservices.Megazord
import mozilla.components.browser.state.action.SystemAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.webextension.isUnsupported
import mozilla.components.concept.push.PushProcessor
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.addons.migration.DefaultSupportedAddonsChecker
import mozilla.components.feature.addons.update.GlobalAddonDependencyProvider
import mozilla.components.feature.top.sites.TopSitesFrecencyConfig
import mozilla.components.feature.top.sites.TopSitesProviderConfig
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.support.base.facts.register
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.ktx.android.content.isMainProcess
import mozilla.components.support.ktx.android.content.runOnlyInMainProcess
import mozilla.components.support.locale.LocaleAwareApplication
import mozilla.components.support.rusterrors.initializeRustErrors
import mozilla.components.support.rusthttp.RustHttpConfig
import mozilla.components.support.rustlog.RustLog
import mozilla.components.support.utils.logElapsedTime
import mozilla.components.support.webextensions.WebExtensionSupport
import net.waterfox.android.components.Components
import net.waterfox.android.components.Core
import net.waterfox.android.components.appstate.AppAction
import net.waterfox.android.ext.containsQueryParameters
import net.waterfox.android.ext.settings
import net.waterfox.android.perf.MarkersActivityLifecycleCallbacks
import net.waterfox.android.perf.ProfilerMarkerFactProcessor
import net.waterfox.android.perf.StartupTimeline
import net.waterfox.android.perf.runBlockingIncrement
import net.waterfox.android.push.PushFxaIntegration
import net.waterfox.android.push.WebPushEngineIntegration
import net.waterfox.android.session.PerformanceActivityLifecycleCallbacks
import net.waterfox.android.session.VisibilityLifecycleCallback
import net.waterfox.android.utils.BrowsersCache
import net.waterfox.android.utils.Settings.Companion.TOP_SITES_PROVIDER_MAX_THRESHOLD
import org.mozilla.experiments.nimbus.NimbusInterface
import org.mozilla.experiments.nimbus.internal.EnrolledExperiment
import java.util.concurrent.TimeUnit

/**
 *The main application class for Waterfox. Records data to measure initialization performance.
 *  Installs [CrashReporter] and setup Megazord in the main process.
 */
@Suppress("Registered", "TooManyFunctions", "LargeClass")
open class WaterfoxApplication : LocaleAwareApplication(), Provider {
    init {
        recordOnInit() // DO NOT MOVE ANYTHING ABOVE HERE: the timing of this measurement is critical.
    }

    private val logger = Logger("WaterfoxApplication")

    open val components by lazy { Components(this) }

    var visibilityLifecycleCallback: VisibilityLifecycleCallback? = null
        private set

    override fun onCreate() {
        super.onCreate()

        setupInAllProcesses()

        if (!isMainProcess()) {
            // If this is not the main process then do not continue with the initialization here. Everything that
            // follows only needs to be done in our app's main process and should not be done in other processes like
            // a GeckoView child process or the crash handling process. Most importantly we never want to end up in a
            // situation where we create a GeckoRuntime from the Gecko child process.
            return
        }

        setupInMainProcessOnly()
        // WATERFOX
        // downloadWallpapers()
    }

    @CallSuper
    open fun setupInAllProcesses() {
        setupCrashReporting()

        // We want the log messages of all builds to go to Android logcat
        Log.addSink(WaterfoxLogSink(logsDebug = Config.channel.isDebug))
    }

    @CallSuper
    open fun setupInMainProcessOnly() {
        ProfilerMarkerFactProcessor.create { components.core.engine.profiler }.register()

        run {
            // Attention: Do not invoke any code from a-s in this scope.
            val megazordSetup = setupMegazord()

            setDayNightTheme()
            components.strictMode.enableStrictMode(true)
            warmBrowsersCache()

            // Make sure the engine is initialized and ready to use.
            components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
                components.core.engine.warmUp()
            }
            initializeWebExtensionSupport()
            restoreBrowserState()
            restoreDownloads()

            // Just to make sure it is impossible for any application-services pieces
            // to invoke parts of itself that require complete megazord initialization
            // before that process completes, we wait here, if necessary.
            if (!megazordSetup.isCompleted) {
                runBlockingIncrement { megazordSetup.await() }
            }
        }

        setupLeakCanary()
        setupPush()

        visibilityLifecycleCallback = VisibilityLifecycleCallback(getSystemService())
        registerActivityLifecycleCallbacks(visibilityLifecycleCallback)
        registerActivityLifecycleCallbacks(MarkersActivityLifecycleCallbacks(components.core.engine))

        // Storage maintenance disabled, for now, as it was interfering with background migrations.
        // See https://github.com/mozilla-mobile/fenix/issues/7227 for context.
        // if ((System.currentTimeMillis() - settings().lastPlacesStorageMaintenance) > ONE_DAY_MILLIS) {
        //    runStorageMaintenance()
        // }

        components.appStartReasonProvider.registerInAppOnCreate(this)
        components.startupActivityLog.registerInAppOnCreate(this)
        initVisualCompletenessQueueAndQueueTasks()
    }

    @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
    private fun restoreBrowserState() = GlobalScope.launch(Dispatchers.Main) {
        val store = components.core.store
        val sessionStorage = components.core.sessionStorage

        components.useCases.tabsUseCases.restore(sessionStorage, settings().getTabTimeout())

        // Now that we have restored our previous state (if there's one) let's setup auto saving the state while
        // the app is used.
        sessionStorage.autoSave(store)
            .periodicallyInForeground(interval = 30, unit = TimeUnit.SECONDS)
            .whenGoingToBackground()
            .whenSessionsChange()
    }

    private fun restoreDownloads() {
        components.useCases.downloadUseCases.restoreDownloads()
    }

    private fun initVisualCompletenessQueueAndQueueTasks() {
        val queue = components.performance.visualCompletenessQueue.queue

        fun initQueue() {
            registerActivityLifecycleCallbacks(PerformanceActivityLifecycleCallbacks(queue))
        }

        @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
        fun queueInitStorageAndServices() {
            components.performance.visualCompletenessQueue.queue.runIfReadyOrQueue {
                GlobalScope.launch(Dispatchers.IO) {
                    logger.info("Running post-visual completeness tasks...")
                    logElapsedTime(logger, "Storage initialization") {
                        components.core.historyStorage.warmUp()
                        components.core.bookmarksStorage.warmUp()
                        components.core.passwordsStorage.warmUp()
                        components.core.autofillStorage.warmUp()

                        // Populate the top site cache to improve initial load experience
                        // of the home fragment when the app is launched to a tab. The actual
                        // database call is not expensive. However, the additional context
                        // switches delay rendering top sites when the cache is empty, which
                        // we can prevent with this.
                        components.core.topSitesStorage.getTopSites(
                            totalSites = components.settings.topSitesMaxLimit,
                            frecencyConfig = TopSitesFrecencyConfig(
                                FrecencyThresholdOption.SKIP_ONE_TIME_PAGES
                            ) { !it.containsQueryParameters(components.settings.frecencyFilterQuery) },
                            providerConfig = TopSitesProviderConfig(
                                showProviderTopSites = components.settings.showContileFeature,
                                maxThreshold = TOP_SITES_PROVIDER_MAX_THRESHOLD
                            )
                        )

                        // This service uses `historyStorage`, and so we can only touch it when we know
                        // it's safe to touch `historyStorage. By 'safe', we mainly mean that underlying
                        // places library will be able to load, which requires first running Megazord.init().
                        // The visual completeness tasks are scheduled after the Megazord.init() call.
                        components.core.historyMetadataService.cleanup(
                            System.currentTimeMillis() - Core.HISTORY_METADATA_MAX_AGE_IN_MS
                        )
                    }
                }
                // Account manager initialization needs to happen on the main thread.
                GlobalScope.launch(Dispatchers.Main) {
                    logElapsedTime(logger, "Kicking-off account manager") {
                        components.backgroundServices.accountManager
                    }
                }
            }
        }

        @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
        fun queueReviewPrompt() {
            GlobalScope.launch(Dispatchers.IO) {
                components.reviewPromptController.trackApplicationLaunch()
            }
        }

        @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
        fun queueRestoreLocale() {
            components.performance.visualCompletenessQueue.queue.runIfReadyOrQueue {
                GlobalScope.launch(Dispatchers.IO) {
                    components.useCases.localeUseCases.restore()
                }
            }
        }

        initQueue()

        // We init these items in the visual completeness queue to avoid them initing in the critical
        // startup path, before the UI finishes drawing (i.e. visual completeness).
        queueInitStorageAndServices()
        queueReviewPrompt()
        queueRestoreLocale()
    }

    // See https://github.com/mozilla-mobile/fenix/issues/7227 for context.
    // To re-enable this, we need to do so in a way that won't interfere with any startup operations
    // which acquire reserved+ sqlite lock. Currently, Fennec migrations need to write to storage
    // on startup, and since they run in a background service we can't simply order these operations.
    // @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
    // private fun runStorageMaintenance() {
    //     GlobalScope.launch(Dispatchers.IO) {
    //        // Bookmarks and history storage sit on top of the same db file so we only need to
    //        // run maintenance on one - arbitrarily using bookmarks.
    //        // components.core.bookmarksStorage.runMaintenance()
    //     }
    //     settings().lastPlacesStorageMaintenance = System.currentTimeMillis()
    // }

    protected open fun setupLeakCanary() {
        // no-op, LeakCanary is disabled by default
    }

    open fun updateLeakCanaryState(isEnabled: Boolean) {
        // no-op, LeakCanary is disabled by default
    }

    private fun setupPush() {
        // Sets the PushFeature as the singleton instance for push messages to go to.
        // We need the push feature setup here to deliver messages in the case where the service
        // starts up the app first.
        components.push.feature?.let {
            Logger.info("AutoPushFeature is configured, initializing it...")

            // Install the AutoPush singleton to receive messages.
            PushProcessor.install(it)

            WebPushEngineIntegration(components.core.engine, it).start()

            // Perform a one-time initialization of the account manager if a message is received.
            PushFxaIntegration(it, lazy { components.backgroundServices.accountManager }).launch()

            // Initialize the service. This could potentially be done in a coroutine in the future.
            it.initialize()
        }
    }

    private fun setupCrashReporting() {
        components
            .analytics
            .crashReporter
            .install(this)
    }

    /**
     * Initiate Megazord sequence! Megazord Battle Mode!
     *
     * The application-services combined libraries are known as the "megazord". We use the default `full`
     * megazord - it contains everything that Waterfox needs, and (currently) nothing more.
     *
     * Documentation on what megazords are, and why they're needed:
     * - https://github.com/mozilla/application-services/blob/master/docs/design/megazords.md
     * - https://mozilla.github.io/application-services/docs/applications/consuming-megazord-libraries.html
     */
    @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
    private fun setupMegazord(): Deferred<Unit> {
        // Note: Megazord.init() must be called as soon as possible ...
        Megazord.init()
        // Give the generated FxNimbus a closure to lazily get the Nimbus object
        // TODO: [Waterfox] remove Nimbus initialization
//        FxNimbus.initialize { components.analytics.experiments }
        return GlobalScope.async(Dispatchers.IO) {
            initializeRustErrors(components.analytics.crashReporter)
            // ... but RustHttpConfig.setClient() and RustLog.enable() can be called later.
            RustHttpConfig.setClient(lazy { components.core.client })
            // Once application-services has switched to using the new
            // error reporting system, RustLog shouldn't input a CrashReporter
            // anymore.
            // (https://github.com/mozilla/application-services/issues/4981).
            RustLog.enable(components.analytics.crashReporter)
            // We want to ensure Nimbus is initialized as early as possible so we can
            // experiment on features close to startup.
            // But we need viaduct (the RustHttp client) to be ready before we do.
            components.analytics.experiments.apply {
                initialize()
                setupNimbusObserver(this)
            }
        }
    }

    private fun setupNimbusObserver(nimbus: Observable<NimbusInterface.Observer>) {
        nimbus.register(object : NimbusInterface.Observer {
            override fun onUpdatesApplied(updated: List<EnrolledExperiment>) {
                onNimbusStartupAndUpdate()
            }
        })
    }

    private fun onNimbusStartupAndUpdate() {
        val settings = settings()
        if (FeatureFlags.messagingFeature && settings.isExperimentationEnabled) {
            components.appStore.dispatch(AppAction.MessagingAction.Restore)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // Additional logging and breadcrumb to debug memory issues:
        // https://github.com/mozilla-mobile/fenix/issues/12731

        logger.info("onTrimMemory(), level=$level, main=${isMainProcess()}")

        components.analytics.crashReporter.recordCrashBreadcrumb(
            Breadcrumb(
                category = "Memory",
                message = "onTrimMemory()",
                data = mapOf(
                    "level" to level.toString(),
                    "main" to isMainProcess().toString()
                ),
                level = Breadcrumb.Level.INFO
            )
        )

        runOnlyInMainProcess {
            components.core.icons.onTrimMemory(level)
            components.core.store.dispatch(SystemAction.LowMemoryAction(level))
        }
    }

    @SuppressLint("WrongConstant")
    // Suppressing erroneous lint warning about using MODE_NIGHT_AUTO_BATTERY, a likely library bug
    private fun setDayNightTheme() {
        val settings = this.settings()
        when {
            settings.shouldUseLightTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
            }
            settings.shouldUseDarkTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
            }
            SDK_INT < Build.VERSION_CODES.P && settings.shouldUseAutoBatteryTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                )
            }
            SDK_INT >= Build.VERSION_CODES.P && settings.shouldFollowDeviceTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
            // First run of app no default set, set the default to Follow System for 28+ and Normal Mode otherwise
            else -> {
                if (SDK_INT >= Build.VERSION_CODES.P) {
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    )
                    settings.shouldFollowDeviceTheme = true
                } else {
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                    )
                    settings.shouldUseLightTheme = true
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage
    private fun warmBrowsersCache() {
        // We avoid blocking the main thread for BrowsersCache on startup by loading it on
        // background thread.
        GlobalScope.launch(Dispatchers.Default) {
            BrowsersCache.all(this@WaterfoxApplication)
        }
    }

    private fun initializeWebExtensionSupport() {
        try {
            GlobalAddonDependencyProvider.initialize(
                components.addonManager,
                components.addonUpdater,
                onCrash = { exception ->
                    components.analytics.crashReporter.submitCaughtException(exception)
                }
            )
            WebExtensionSupport.initialize(
                components.core.engine,
                components.core.store,
                onNewTabOverride = {
                    _, engineSession, url ->
                    val shouldCreatePrivateSession =
                        components.core.store.state.selectedTab?.content?.private
                            ?: components.settings.openLinksInAPrivateTab

                    components.useCases.tabsUseCases.addTab(
                        url = url,
                        selectTab = true,
                        engineSession = engineSession,
                        private = shouldCreatePrivateSession
                    )
                },
                onCloseTabOverride = {
                    _, sessionId ->
                    components.useCases.tabsUseCases.removeTab(sessionId)
                },
                onSelectTabOverride = {
                    _, sessionId ->
                    components.useCases.tabsUseCases.selectTab(sessionId)
                },
                onExtensionsLoaded = { extensions ->
                    components.addonUpdater.registerForFutureUpdates(extensions)
                    subscribeForNewAddonsIfNeeded(components.supportedAddonsChecker, extensions)
                },
                onUpdatePermissionRequest = components.addonUpdater::onUpdatePermissionRequest
            )
        } catch (e: UnsupportedOperationException) {
            Logger.error("Failed to initialize web extension support", e)
        }
    }

    @VisibleForTesting
    internal fun subscribeForNewAddonsIfNeeded(
        checker: DefaultSupportedAddonsChecker,
        installedExtensions: List<WebExtension>
    ) {
        val hasUnsupportedAddons = installedExtensions.any { it.isUnsupported() }
        if (hasUnsupportedAddons) {
            checker.registerForChecks()
        } else {
            // As checks are a persistent subscriptions, we have to make sure
            // we remove any previous subscriptions.
            checker.unregisterForChecks()
        }
    }

    protected fun recordOnInit() {
        // This gets called by more than one process. Ideally we'd only run this in the main process
        // but the code to check which process we're in crashes because the Context isn't valid yet.
        //
        // This method is not covered by our internal crash reporting: be very careful when modifying it.
        StartupTimeline.onApplicationInit() // DO NOT MOVE ANYTHING ABOVE HERE: the timing is critical.
    }

    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        // Workaround for androidx appcompat issue where follow system day/night mode config changes
        // are not triggered when also using createConfigurationContext like we do in LocaleManager
        // https://issuetracker.google.com/issues/143570309#comment3
        applicationContext.resources.configuration.uiMode = config.uiMode

        if (isMainProcess()) {
            // We can only do this on the main process as resetAfter will access components.core, which
            // will initialize the engine and create an additional GeckoRuntime from the Gecko
            // child process, causing a crash.

            // There's a strict mode violation in A-Cs LocaleAwareApplication which
            // reads from shared prefs: https://github.com/mozilla-mobile/android-components/issues/8816
            components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
                super.onConfigurationChanged(config)
            }
        } else {
            super.onConfigurationChanged(config)
        }
    }

    override fun getWorkManagerConfiguration() = Builder().setMinimumLoggingLevel(INFO).build()

    @OptIn(DelicateCoroutinesApi::class)
    open fun downloadWallpapers() {
        GlobalScope.launch {
            components.wallpaperManager.downloadAllRemoteWallpapers()
        }
    }
}
