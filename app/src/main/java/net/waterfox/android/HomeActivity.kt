/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.AttributeSet
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PROTECTED
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.MediaSessionAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.feature.contextmenu.DefaultSelectionActionDelegate
import mozilla.components.feature.customtabs.isCustomTabIntent
import mozilla.components.feature.media.ext.findActiveMediaTab
import mozilla.components.feature.privatemode.notification.PrivateNotificationFeature
import mozilla.components.feature.search.BrowserStoreSearchAdapter
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.arch.lifecycle.addObservers
import mozilla.components.support.ktx.android.content.call
import mozilla.components.support.ktx.android.content.email
import mozilla.components.support.ktx.android.content.share
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.locale.LocaleAwareAppCompatActivity
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupObserver
import net.waterfox.android.addons.AddonDetailsFragmentDirections
import net.waterfox.android.addons.AddonPermissionsDetailsFragmentDirections
import net.waterfox.android.addons.AddonsManagementFragmentDirections
import net.waterfox.android.addons.ExtensionsProcessDisabledBackgroundController
import net.waterfox.android.addons.ExtensionsProcessDisabledForegroundController
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.browser.browsingmode.BrowsingModeManager
import net.waterfox.android.browser.browsingmode.DefaultBrowsingModeManager
import net.waterfox.android.customtabs.ExternalAppBrowserActivity
import net.waterfox.android.databinding.ActivityHomeBinding
import net.waterfox.android.exceptions.trackingprotection.TrackingProtectionExceptionsFragmentDirections
import net.waterfox.android.ext.*
import net.waterfox.android.extension.WebExtensionPromptFeature
import net.waterfox.android.home.HomeFragmentDirections
import net.waterfox.android.home.intent.*
import net.waterfox.android.library.bookmarks.BookmarkFragmentDirections
import net.waterfox.android.library.bookmarks.DesktopFolders
import net.waterfox.android.library.history.HistoryFragmentDirections
import net.waterfox.android.library.historymetadata.HistoryMetadataGroupFragmentDirections
import net.waterfox.android.library.recentlyclosed.RecentlyClosedFragmentDirections
import net.waterfox.android.onboarding.DefaultBrowserNotificationWorker
import net.waterfox.android.onboarding.WaterfoxOnboarding
import net.waterfox.android.perf.*
import net.waterfox.android.search.SearchDialogFragmentDirections
import net.waterfox.android.session.PrivateNotificationService
import net.waterfox.android.settings.HttpsOnlyFragmentDirections
import net.waterfox.android.settings.SettingsFragmentDirections
import net.waterfox.android.settings.SupportUtils
import net.waterfox.android.settings.TrackingProtectionFragmentDirections
import net.waterfox.android.settings.about.AboutFragmentDirections
import net.waterfox.android.settings.logins.fragment.LoginDetailFragmentDirections
import net.waterfox.android.settings.logins.fragment.SavedLoginsAuthFragmentDirections
import net.waterfox.android.settings.search.AddSearchEngineFragmentDirections
import net.waterfox.android.settings.search.EditCustomSearchEngineFragmentDirections
import net.waterfox.android.share.AddNewDeviceFragmentDirections
import net.waterfox.android.tabstray.TabsTrayFragment
import net.waterfox.android.tabstray.TabsTrayFragmentDirections
import net.waterfox.android.theme.DefaultThemeManager
import net.waterfox.android.theme.ThemeManager
import net.waterfox.android.trackingprotection.TrackingProtectionPanelDialogFragmentDirections
import net.waterfox.android.utils.BrowsersCache
import net.waterfox.android.utils.ManufacturerCodes
import net.waterfox.android.utils.Settings
import java.lang.ref.WeakReference

/**
 * The main activity of the application. The application is primarily a single Activity (this one)
 * with fragments switching out to display different views. The most important views shown here are the:
 * - home screen
 * - browser screen
 */
@SuppressWarnings("TooManyFunctions", "LargeClass", "LongParameterList", "LongMethod")
open class HomeActivity : LocaleAwareAppCompatActivity(), NavHostActivity {
    private lateinit var binding: ActivityHomeBinding
    lateinit var themeManager: ThemeManager
    lateinit var browsingModeManager: BrowsingModeManager

    private var isVisuallyComplete = false

    private var privateNotificationObserver: PrivateNotificationFeature<PrivateNotificationService>? =
        null

    private var isToolbarInflated = false

    private val webExtensionPopupObserver by lazy {
        WebExtensionPopupObserver(components.core.store, ::openPopup)
    }
    val webExtensionPromptFeature by lazy {
        WebExtensionPromptFeature(
            store = components.core.store,
            context = this@HomeActivity,
            fragmentManager = supportFragmentManager,
            onLinkClicked = { url, shouldOpenInBrowser ->
                if (shouldOpenInBrowser) {
                    openToBrowserAndLoad(
                        searchTermOrURL = url,
                        newTab = true,
                        from = BrowserDirection.FromGlobal,
                    )
                } else {
                    startActivity(
                        SupportUtils.createCustomTabIntent(
                            context = this,
                            url = url,
                        ),
                    )
                }
            },
        )
    }

    private val extensionsProcessDisabledForegroundController by lazy {
        ExtensionsProcessDisabledForegroundController(this@HomeActivity)
    }

    private val extensionsProcessDisabledBackgroundController by lazy {
        ExtensionsProcessDisabledBackgroundController(
            browserStore = components.core.store,
            appStore = components.appStore,
        )
    }

    private val serviceWorkerSupport by lazy {
        ServiceWorkerSupportFeature(this)
    }

    private var inflater: LayoutInflater? = null

    private val navHost by lazy {
        supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
    }

    private val onboarding by lazy { WaterfoxOnboarding(applicationContext) }

    private val externalSourceIntentProcessors by lazy {
        listOf(
            HomeDeepLinkIntentProcessor(this),
            SpeechProcessingIntentProcessor(this, components.core.store),
            StartSearchIntentProcessor(),
            OpenBrowserIntentProcessor(this, ::getIntentSessionId),
            OpenSpecificTabIntentProcessor(this),
            DefaultBrowserIntentProcessor(this)
        )
    }

    // See onKeyDown for why this is necessary
    private var backLongPressJob: Job? = null

    private lateinit var navigationToolbar: Toolbar

    // Tracker for contextual menu (Copy|Search|Select all|etc...)
    private var actionMode: ActionMode? = null

    private val startupPathProvider = StartupPathProvider()

    final override fun onCreate(savedInstanceState: Bundle?) {
        // DO NOT MOVE ANYTHING ABOVE THIS getProfilerTime CALL.
        val startTimeProfiler = components.core.engine.profiler?.getProfilerTime()

        components.strictMode.attachListenerToDisablePenaltyDeath(supportFragmentManager)
        MarkersFragmentLifecycleCallbacks.register(supportFragmentManager, components.core.engine)

        // There is disk read violations on some devices such as samsung and pixel for android 9/10
        components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            // Theme setup should always be called before super.onCreate
            setupThemeAndBrowsingMode(getModeFromIntentOrLastKnown(intent))
            super.onCreate(savedInstanceState)
        }

        // Checks if Activity is currently in PiP mode if launched from external intents, then exits it
        checkAndExitPiP()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onCreate()",
            data = mapOf(
                "recreated" to (savedInstanceState != null).toString(),
                "intent" to (intent?.action ?: "null")
            )
        )

        components.publicSuffixList.prefetch()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ProfilerMarkers.addListenerForOnGlobalLayout(components.core.engine, this, binding.root)

        // Must be after we set the content view
        if (isVisuallyComplete) {
            components.performance.visualCompletenessQueue
                .attachViewToRunVisualCompletenessQueueLater(WeakReference(binding.rootContainer))
        }

        privateNotificationObserver = PrivateNotificationFeature(
            applicationContext,
            components.core.store,
            PrivateNotificationService::class
        ).also {
            it.start()
        }

        // Unless the activity is recreated, navigate to home first (without rendering it)
        // to add it to the back stack.
        if (savedInstanceState == null) {
            navigateToHome()
        }

        if (!shouldStartOnHome() && shouldNavigateToBrowserOnColdStart(savedInstanceState)) {
            navigateToBrowserOnColdStart()
        }

        if (settings().showHomeOnboardingDialog && onboarding.userHasBeenOnboarded()) {
            navHost.navController.navigate(NavGraphDirections.actionGlobalHomeOnboardingDialog())
        }

        Performance.processIntentIfPerformanceTest(intent, this)

        supportActionBar?.hide()

        lifecycle.addObservers(
            webExtensionPopupObserver,
            extensionsProcessDisabledForegroundController,
            extensionsProcessDisabledBackgroundController,
            serviceWorkerSupport,
        )

        if (!isCustomTabIntent(intent)) {
            lifecycle.addObserver(webExtensionPromptFeature)
        }

        if (shouldAddToRecentsScreen(intent)) {
            intent.removeExtra(START_IN_RECENTS_SCREEN)
            moveTaskToBack(true)
        }

        startupPathProvider.attachOnActivityOnCreate(lifecycle, intent)

        components.core.requestInterceptor.setNavigationController(navHost.navController)

        if (settings().showContileFeature) {
            components.core.contileTopSitesUpdater.startPeriodicWork()
        }

        components.backgroundServices.accountManagerAvailableQueue.runIfReadyOrQueue {
            lifecycleScope.launch(IO) {
                // If we're authenticated, kick-off a sync and a device state refresh.
                components.backgroundServices.accountManager.authenticatedAccount()?.let {
                    components.backgroundServices.accountManager.syncNow(reason = SyncReason.Startup)
                }
            }
        }

        components.core.engine.profiler?.addMarker(
            MarkersActivityLifecycleCallbacks.MARKER_NAME, startTimeProfiler, "HomeActivity.onCreate"
        )

        components.notificationsDelegate.bindToActivity(this)

        StartupTimeline.onActivityCreateEndHome(this) // DO NOT MOVE ANYTHING BELOW HERE.
    }

    private fun checkAndExitPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode && intent != null) {
            // Exit PiP mode
            moveTaskToBack(false)
            startActivity(Intent(this, this::class.java).setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    @CallSuper
    @Suppress("TooGenericExceptionCaught")
    override fun onResume() {
        super.onResume()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onResume()"
        )

        lifecycleScope.launch(IO) {
            try {
                if (settings().showContileFeature) {
                    components.core.contileTopSitesProvider.refreshTopSitesIfCacheExpired()
                }
            } catch (e: Exception) {
                Logger.error("Failed to refresh contile top sites", e)
            }

            DefaultBrowserNotificationWorker.setDefaultBrowserNotificationIfNeeded(applicationContext)
        }
    }

    override fun onStart() {
        // DO NOT MOVE ANYTHING ABOVE THIS getProfilerTime CALL.
        val startProfilerTime = components.core.engine.profiler?.getProfilerTime()

        super.onStart()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onStart()"
        )

        ProfilerMarkers.homeActivityOnStart(binding.rootContainer, components.core.engine.profiler)
        components.core.engine.profiler?.addMarker(
            MarkersActivityLifecycleCallbacks.MARKER_NAME, startProfilerTime, "HomeActivity.onStart"
        ) // DO NOT MOVE ANYTHING BELOW THIS addMarker CALL.
    }

    override fun onStop() {
        super.onStop()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onStop()",
            data = mapOf(
                "finishing" to isFinishing.toString()
            )
        )
    }

    final override fun onPause() {
        // We should return to the browser if there were normal tabs when we left the app
        settings().shouldReturnToBrowser =
            components.core.store.state.getNormalOrPrivateTabs(private = false).isNotEmpty()

        lifecycleScope.launch(IO) {
            components.core.bookmarksStorage.getTree(BookmarkRoot.Root.id, true)?.let {
                val desktopRootNode = DesktopFolders(
                    applicationContext,
                    showMobileRoot = false
                ).withOptionalDesktopFolders(it)
                settings().desktopBookmarksSize = getBookmarkCount(desktopRootNode)
            }

            components.core.bookmarksStorage.getTree(BookmarkRoot.Mobile.id, true)?.let {
                settings().mobileBookmarksSize = getBookmarkCount(it)
            }
        }

        super.onPause()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onPause()",
            data = mapOf(
                "finishing" to isFinishing.toString()
            )
        )

        // Every time the application goes into the background, it is possible that the user
        // is about to change the browsers installed on their system. Therefore, we reset the cache of
        // all the installed browsers.
        //
        // NB: There are ways for the user to install new products without leaving the browser.
        BrowsersCache.resetAll()
    }

    private fun getBookmarkCount(node: BookmarkNode): Int {
        val children = node.children
        return if (children == null) {
            0
        } else {
            var count = 0

            for (child in children) {
                if (child.type == BookmarkNodeType.FOLDER) {
                    count += getBookmarkCount(child)
                } else if (child.type == BookmarkNodeType.ITEM) {
                    count++
                }
            }

            count
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onDestroy()",
            data = mapOf(
                "finishing" to isFinishing.toString()
            )
        )

        components.core.contileTopSitesUpdater.stopPeriodicWork()
        privateNotificationObserver?.stop()
        components.notificationsDelegate.unBindActivity(this)

        if (this !is ExternalAppBrowserActivity) {
            stopMediaSession()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onConfigurationChanged()"
        )
    }

    override fun recreate() {
        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "recreate()"
        )

        super.recreate()
    }

    /**
     * Handles intents received when the activity is open.
     */
    final override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
        startupPathProvider.onIntentReceived(intent)
    }

    open fun handleNewIntent(intent: Intent) {
        // Diagnostic breadcrumb for "Display already aquired" crash:
        // https://github.com/mozilla-mobile/android-components/issues/7960
        breadcrumb(
            message = "onNewIntent()",
            data = mapOf(
                "intent" to intent.action.toString()
            )
        )

        val tab = components.core.store.state.findActiveMediaTab()
        if (tab != null) {
            components.useCases.sessionUseCases.exitFullscreen(tab.id)
        }

        val intentProcessors =
            listOf(
                CrashReporterIntentProcessor(components.appStore)
            ) + externalSourceIntentProcessors
        val intentHandled =
            intentProcessors.any { it.process(intent, navHost.navController, this.intent) }
        browsingModeManager.mode = getModeFromIntentOrLastKnown(intent)

        if (intentHandled) {
            supportFragmentManager
                .primaryNavigationFragment
                ?.childFragmentManager
                ?.fragments
                ?.lastOrNull()
                ?.let { it as? TabsTrayFragment }
                ?.also { it.dismissAllowingStateLoss() }
        }
    }

    /**
     * Overrides view inflation to inject a custom [EngineView] from [components].
     */
    final override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? = when (name) {
        EngineView::class.java.name -> components.core.engine.createView(context, attrs).apply {
            selectionActionDelegate = DefaultSelectionActionDelegate(
                BrowserStoreSearchAdapter(
                    components.core.store,
                    tabId = getIntentSessionId(intent.toSafeIntent())
                ),
                resources = context.resources,
                shareTextClicked = { share(it) },
                emailTextClicked = { email(it) },
                callTextClicked = { call(it) },
                actionSorter = ::actionSorter
            )
        }.asView()
        else -> super.onCreateView(parent, name, context, attrs)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        actionMode = mode
        super.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        actionMode = null
        super.onActionModeFinished(mode)
    }

    fun finishActionMode() {
        actionMode?.finish().also { actionMode = null }
    }

    @Suppress("MagicNumber")
    // Defining the positions as constants doesn't seem super useful here.
    private fun actionSorter(actions: Array<String>): Array<String> {
        val order = hashMapOf<String, Int>()

        order["CUSTOM_CONTEXT_MENU_EMAIL"] = 0
        order["CUSTOM_CONTEXT_MENU_CALL"] = 1
        order["org.mozilla.geckoview.COPY"] = 2
        order["CUSTOM_CONTEXT_MENU_SEARCH"] = 3
        order["CUSTOM_CONTEXT_MENU_SEARCH_PRIVATELY"] = 4
        order["org.mozilla.geckoview.PASTE"] = 5
        order["org.mozilla.geckoview.SELECT_ALL"] = 6
        order["CUSTOM_CONTEXT_MENU_SHARE"] = 7

        return actions.sortedBy { actionName ->
            // Sort the actions in our preferred order, putting "other" actions unsorted at the end
            order[actionName] ?: actions.size
        }.toTypedArray()
    }

    final override fun onBackPressed() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is UserInteractionHandler && it.onBackPressed()) {
                return
            }
        }
        onBackPressedDispatcher.onBackPressed()
    }

    @Suppress("DEPRECATION")
    // https://github.com/mozilla-mobile/fenix/issues/19919
    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is ActivityResultHandler && it.onActivityResult(requestCode, data, resultCode)) {
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun shouldUseCustomBackLongPress(): Boolean {
        val isAndroidN =
            Build.VERSION.SDK_INT == Build.VERSION_CODES.N || Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1
        // Huawei devices seem to have problems with onKeyLongPress
        // See https://github.com/mozilla-mobile/fenix/issues/13498
        return isAndroidN || ManufacturerCodes.isHuawei
    }

    private fun handleBackLongPress(): Boolean {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is OnBackLongPressedListener && it.onBackLongPressed()) {
                return true
            }
        }
        return false
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ProfilerMarkers.addForDispatchTouchEvent(components.core.engine.profiler, ev)
        return super.dispatchTouchEvent(ev)
    }

    final override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Inspired by https://searchfox.org/mozilla-esr68/source/mobile/android/base/java/org/mozilla/gecko/BrowserApp.java#584-613
        // Android N and Huawei devices have broken onKeyLongPress events for the back button, so we
        // instead implement the long press behavior ourselves
        // - For short presses, we cancel the callback in onKeyUp
        // - For long presses, the normal keypress is marked as cancelled, hence won't be handled elsewhere
        //   (but Android still provides the haptic feedback), and the long press action is run
        if (shouldUseCustomBackLongPress() && keyCode == KeyEvent.KEYCODE_BACK) {
            backLongPressJob = lifecycleScope.launch {
                delay(ViewConfiguration.getLongPressTimeout().toLong())
                handleBackLongPress()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    final override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (shouldUseCustomBackLongPress() && keyCode == KeyEvent.KEYCODE_BACK) {
            backLongPressJob?.cancel()
        }
        return super.onKeyUp(keyCode, event)
    }

    final override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        // onKeyLongPress is broken in Android N so we don't handle back button long presses here
        // for N. The version check ensures we don't handle back button long presses twice.
        if (!shouldUseCustomBackLongPress() && keyCode == KeyEvent.KEYCODE_BACK) {
            return handleBackLongPress()
        }
        return super.onKeyLongPress(keyCode, event)
    }

    final override fun onUserLeaveHint() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is UserInteractionHandler && it.onHomePressed()) {
                return
            }
        }

        super.onUserLeaveHint()
    }

    protected open fun getBreadcrumbMessage(destination: NavDestination): String {
        val fragmentName = resources.getResourceEntryName(destination.id)
        return "Changing to fragment $fragmentName, isCustomTab: false"
    }

    @VisibleForTesting(otherwise = PROTECTED)
    internal open fun getIntentSource(intent: SafeIntent): String? {
        return when {
            intent.isLauncherIntent -> "APP_ICON"
            intent.action == Intent.ACTION_VIEW -> "LINK"
            else -> null
        }
    }

    /**
     * External sources such as 3rd party links and shortcuts use this function to enter
     * private mode directly before the content view is created. Returns the mode set by the intent
     * otherwise falls back to the last known mode.
     */
    internal fun getModeFromIntentOrLastKnown(intent: Intent?): BrowsingMode {
        intent?.toSafeIntent()?.let {
            if (it.hasExtra(PRIVATE_BROWSING_MODE)) {
                val startPrivateMode = it.getBooleanExtra(PRIVATE_BROWSING_MODE, false)
                return BrowsingMode.fromBoolean(isPrivate = startPrivateMode)
            }
        }
        return settings().lastKnownMode
    }

    /**
     * Determines whether the activity should be pushed to be backstack (i.e., 'minimized' to the recents
     * screen) upon starting.
     * @param intent - The intent that started this activity. Is checked for having the 'START_IN_RECENTS_SCREEN'-extra.
     * @return true if the activity should be started and pushed to the recents screen, false otherwise.
     */
    private fun shouldAddToRecentsScreen(intent: Intent?): Boolean {
        intent?.toSafeIntent()?.let {
            return it.getBooleanExtra(START_IN_RECENTS_SCREEN, false)
        }
        return false
    }

    private fun setupThemeAndBrowsingMode(mode: BrowsingMode) {
        settings().lastKnownMode = mode
        browsingModeManager = createBrowsingModeManager(mode)
        themeManager = createThemeManager()
        themeManager.setActivityTheme(this)
        themeManager.applyStatusBarTheme(this)
    }

    // Stop active media when activity is destroyed.
    private fun stopMediaSession() {
        if (isFinishing) {
            components.core.store.state.tabs.forEach {
                it.mediaSessionState?.controller?.stop()
            }

            components.core.store.state.findActiveMediaTab()?.let {
                components.core.store.dispatch(
                    MediaSessionAction.DeactivatedMediaSessionAction(
                        it.id,
                    ),
                )
            }
        }
    }

    /**
     * Returns the [supportActionBar], inflating it if necessary.
     * Everyone should call this instead of supportActionBar.
     */
    override fun getSupportActionBarAndInflateIfNecessary(): ActionBar {
        if (!isToolbarInflated) {
            navigationToolbar = binding.navigationToolbarStub.inflate() as Toolbar

            setSupportActionBar(navigationToolbar)
            // Add ids to this that we don't want to have a toolbar back button
            setupNavigationToolbar()
            setNavigationIcon(R.drawable.ic_back_button)

            isToolbarInflated = true
        }
        return supportActionBar!!
    }

    @Suppress("SpreadOperator")
    fun setupNavigationToolbar(vararg topLevelDestinationIds: Int) {
        NavigationUI.setupWithNavController(
            navigationToolbar,
            navHost.navController,
            AppBarConfiguration.Builder(*topLevelDestinationIds).build()
        )

        navigationToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    protected open fun getIntentSessionId(intent: SafeIntent): String? = null

    /**
     * Navigates to the browser fragment and loads a URL or performs a search (depending on the
     * value of [searchTermOrURL]).
     *
     * @param flags Flags that will be used when loading the URL (not applied to searches).
     */
    @Suppress("LongParameterList")
    fun openToBrowserAndLoad(
        searchTermOrURL: String,
        newTab: Boolean,
        from: BrowserDirection,
        customTabSessionId: String? = null,
        engine: SearchEngine? = null,
        forceSearch: Boolean = false,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
        requestDesktopMode: Boolean = false,
        historyMetadata: HistoryMetadataKey? = null
    ) {
        openToBrowser(from, customTabSessionId)
        load(searchTermOrURL, newTab, engine, forceSearch, flags, requestDesktopMode, historyMetadata)
    }

    fun openToBrowser(from: BrowserDirection, customTabSessionId: String? = null) {
        if (navHost.navController.alreadyOnDestination(R.id.browserFragment)) return
        @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
        val directions = getNavDirections(from, customTabSessionId)
        if (directions != null) {
            navHost.navController.nav(fragmentId, directions)
        }
    }

    protected open fun getNavDirections(
        from: BrowserDirection,
        customTabSessionId: String?
    ): NavDirections? = when (from) {
        BrowserDirection.FromGlobal ->
            NavGraphDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHome ->
            HomeFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSearchDialog ->
            SearchDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSettings ->
            SettingsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromBookmarks ->
            BookmarkFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromBookmarkSearchDialog ->
            SearchDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHistory ->
            HistoryFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHistorySearchDialog ->
            SearchDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHistoryMetadataGroup ->
            HistoryMetadataGroupFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTrackingProtectionExceptions ->
            TrackingProtectionExceptionsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHttpsOnlyMode ->
            HttpsOnlyFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAbout ->
            AboutFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTrackingProtection ->
            TrackingProtectionFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTrackingProtectionDialog ->
            TrackingProtectionPanelDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSavedLoginsFragment ->
            SavedLoginsAuthFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddNewDeviceFragment ->
            AddNewDeviceFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddSearchEngineFragment ->
            AddSearchEngineFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromEditCustomSearchEngineFragment ->
            EditCustomSearchEngineFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddonDetailsFragment ->
            AddonDetailsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddonPermissionsDetailsFragment ->
            AddonPermissionsDetailsFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromLoginDetailFragment ->
            LoginDetailFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromTabsTray ->
            TabsTrayFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromRecentlyClosed ->
            RecentlyClosedFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromAddonsManagementFragment ->
            AddonsManagementFragmentDirections.actionGlobalBrowser(customTabSessionId)
    }

    /**
     * Loads a URL or performs a search (depending on the value of [searchTermOrURL]).
     *
     * @param flags Flags that will be used when loading the URL (not applied to searches).
     * @param historyMetadata The [HistoryMetadataKey] of the new tab in case this tab
     * was opened from history.
     */
    private fun load(
        searchTermOrURL: String,
        newTab: Boolean,
        engine: SearchEngine?,
        forceSearch: Boolean,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none(),
        requestDesktopMode: Boolean = false,
        historyMetadata: HistoryMetadataKey? = null
    ) {
        val startTime = components.core.engine.profiler?.getProfilerTime()
        val mode = browsingModeManager.mode

        val private = when (mode) {
            BrowsingMode.Private -> true
            BrowsingMode.Normal -> false
        }

        // In situations where we want to perform a search but have no search engine (e.g. the user
        // has removed all of them, or we couldn't load any) we will pass searchTermOrURL to Gecko
        // and let it try to load whatever was entered.
        if ((!forceSearch && searchTermOrURL.isUrl()) || engine == null) {
            val tabId = if (newTab) {
                components.useCases.tabsUseCases.addTab(
                    url = searchTermOrURL.toNormalizedUrl(),
                    flags = flags,
                    private = private,
                    historyMetadata = historyMetadata
                )
            } else {
                components.useCases.sessionUseCases.loadUrl(
                    url = searchTermOrURL.toNormalizedUrl(),
                    flags = flags
                )
                components.core.store.state.selectedTabId
            }

            if (requestDesktopMode && tabId != null) {
                handleRequestDesktopMode(tabId)
            }
        } else {
            if (newTab) {
                val searchUseCase = if (mode.isPrivate) {
                    components.useCases.searchUseCases.newPrivateTabSearch
                } else {
                    components.useCases.searchUseCases.newTabSearch
                }
                searchUseCase.invoke(
                    searchTermOrURL,
                    SessionState.Source.Internal.UserEntered,
                    true,
                    searchEngine = engine,
                )
            } else {
                components.useCases.searchUseCases.defaultSearch.invoke(searchTermOrURL, engine)
            }
        }

        if (components.core.engine.profiler?.isProfilerActive() == true) {
            // Wrapping the `addMarker` method with `isProfilerActive` even though it's no-op when
            // profiler is not active. That way, `text` argument will not create a string builder all the time.
            components.core.engine.profiler?.addMarker(
                "HomeActivity.load",
                startTime,
                "newTab: $newTab"
            )
        }
    }

    internal fun handleRequestDesktopMode(tabId: String) {
        components.useCases.sessionUseCases.requestDesktopSite(true, tabId)

        // Reset preference value after opening the tab in desktop mode
        settings().openNextTabInDesktopMode = false
    }

    open fun navigateToBrowserOnColdStart() {
        // Normal tabs + cold start -> Should go back to browser if we had any tabs open when we left last
        // except for PBM + Cold Start there won't be any tabs since they're evicted so we never will navigate
        if (settings().shouldReturnToBrowser && !browsingModeManager.mode.isPrivate) {
            // Navigate to home first (without rendering it) to add it to the back stack.
            openToBrowser(BrowserDirection.FromGlobal, null)
        }
    }

    open fun navigateToHome() {
        navHost.navController.navigate(NavGraphDirections.actionStartupHome())
    }

    override fun attachBaseContext(base: Context) {
        base.components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            super.attachBaseContext(base)
        }
    }

    override fun getSystemService(name: String): Any? {
        // Issue #17759 had a crash with the PerformanceInflater.kt on Android 5.0 and 5.1
        // when using the TimePicker. Since the inflater was created for performance monitoring
        // purposes and that we test on new android versions, this means that any difference in
        // inflation will be caught on those devices.
        if (LAYOUT_INFLATER_SERVICE == name && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (inflater == null) {
                inflater = PerformanceInflater(LayoutInflater.from(baseContext), this)
            }
            return inflater
        }
        return super.getSystemService(name)
    }

    protected open fun createBrowsingModeManager(initialMode: BrowsingMode): BrowsingModeManager {
        return DefaultBrowsingModeManager(initialMode, components.settings) { newMode ->
            updateSecureWindowFlags(newMode)
            themeManager.currentTheme = newMode
        }.also {
            updateSecureWindowFlags(initialMode)
        }
    }

    private fun updateSecureWindowFlags(mode: BrowsingMode = browsingModeManager.mode) {
        if (mode == BrowsingMode.Private && !settings().allowScreenshotsInPrivateMode) {
            window.addFlags(FLAG_SECURE)
        } else {
            window.clearFlags(FLAG_SECURE)
        }
    }

    protected open fun createThemeManager(): ThemeManager {
        return DefaultThemeManager(browsingModeManager.mode, this)
    }

    private fun openPopup(webExtensionState: WebExtensionState) {
        val action = NavGraphDirections.actionGlobalWebExtensionActionPopupFragment(
            webExtensionId = webExtensionState.id,
            webExtensionTitle = webExtensionState.name
        )
        navHost.navController.navigate(action)
    }

    /**
     * The root container is null at this point, so let the HomeActivity know that
     * we are visually complete.
     */
    fun setVisualCompletenessQueueReady() {
        isVisuallyComplete = true
    }

    @VisibleForTesting
    internal fun isActivityColdStarted(startingIntent: Intent, activityIcicle: Bundle?): Boolean {
        // First time opening this activity in the task.
        // Cold start / start from Recents after back press.
        return activityIcicle == null &&
            // Activity was restarted from Recents after it was destroyed by Android while in background
            // in cases of memory pressure / "Don't keep activities".
            startingIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
    }

    /**
     *  Indicates if the user should be redirected to the [BrowserFragment] or to the [HomeFragment],
     *  links from an external apps should always opened in the [BrowserFragment].
     */
    fun shouldStartOnHome(intent: Intent? = this.intent): Boolean {
        return components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            // We only want to open on home when users tap the app,
            // we want to ignore other cases when the app gets open by users clicking on links.
            getSettings().shouldStartOnHome() && intent?.action == ACTION_MAIN
        }
    }

    fun processIntent(intent: Intent): Boolean {
        return externalSourceIntentProcessors.any {
            it.process(
                intent,
                navHost.navController,
                this.intent
            )
        }
    }

    @VisibleForTesting
    internal fun getSettings(): Settings = settings()

    private fun shouldNavigateToBrowserOnColdStart(savedInstanceState: Bundle?): Boolean {
        return isActivityColdStarted(intent, savedInstanceState) &&
            !processIntent(intent)
    }

    companion object {
        const val OPEN_TO_BROWSER = "open_to_browser"
        const val OPEN_TO_BROWSER_AND_LOAD = "open_to_browser_and_load"
        const val OPEN_TO_SEARCH = "open_to_search"
        const val PRIVATE_BROWSING_MODE = "private_browsing_mode"
        const val EXTRA_DELETE_PRIVATE_TABS = "notification_delete_and_open"
        const val EXTRA_OPENED_FROM_NOTIFICATION = "notification_open"
        const val START_IN_RECENTS_SCREEN = "start_in_recents_screen"
    }
}
