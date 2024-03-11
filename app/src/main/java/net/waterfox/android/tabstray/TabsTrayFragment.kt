/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.tabstray

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.feature.downloads.ui.DownloadCancelDialogFragment
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import net.waterfox.android.Config
import net.waterfox.android.HomeActivity
import net.waterfox.android.NavGraphDirections
import net.waterfox.android.R
import net.waterfox.android.components.StoreProvider
import net.waterfox.android.components.WaterfoxSnackbar
import net.waterfox.android.databinding.ComponentTabstray2Binding
import net.waterfox.android.databinding.ComponentTabstray3Binding
import net.waterfox.android.databinding.ComponentTabstray3FabBinding
import net.waterfox.android.databinding.ComponentTabstrayFabBinding
import net.waterfox.android.databinding.FragmentTabTrayDialogBinding
import net.waterfox.android.databinding.TabsTrayTabCounter2Binding
import net.waterfox.android.databinding.TabstrayMultiselectItemsBinding
import net.waterfox.android.ext.components
import net.waterfox.android.ext.requireComponents
import net.waterfox.android.ext.runIfFragmentIsAttached
import net.waterfox.android.ext.settings
import net.waterfox.android.home.HomeScreenViewModel
import net.waterfox.android.library.bookmarks.BookmarksSharedViewModel
import net.waterfox.android.share.ShareFragment
import net.waterfox.android.tabstray.browser.SelectionBannerBinding
import net.waterfox.android.tabstray.browser.SelectionBannerBinding.VisibilityModifier
import net.waterfox.android.tabstray.browser.SelectionHandleBinding
import net.waterfox.android.tabstray.browser.TabSorter
import net.waterfox.android.tabstray.ext.anchorWithAction
import net.waterfox.android.tabstray.ext.bookmarkMessage
import net.waterfox.android.tabstray.ext.collectionMessage
import net.waterfox.android.tabstray.ext.make
import net.waterfox.android.tabstray.ext.showWithTheme
import net.waterfox.android.tabstray.syncedtabs.SyncedTabsIntegration
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.ThemeManager
import net.waterfox.android.theme.WaterfoxTheme
import net.waterfox.android.utils.allowUndo
import kotlin.math.max

/**
 * The action or screen that was used to navigate to the Tabs Tray.
 */
enum class TabsTrayAccessPoint {
    None,
    HomeRecentSyncedTab,
}

@Suppress("TooManyFunctions", "LargeClass")
class TabsTrayFragment : AppCompatDialogFragment() {

    @VisibleForTesting internal lateinit var tabsTrayStore: TabsTrayStore
    private lateinit var tabsTrayDialog: TabsTrayDialog
    private lateinit var tabsTrayInteractor: TabsTrayInteractor
    private lateinit var tabsTrayController: DefaultTabsTrayController
    private lateinit var navigationInteractor: DefaultNavigationInteractor

    @VisibleForTesting internal lateinit var trayBehaviorManager: TabSheetBehaviorManager

    private val tabLayoutMediator = ViewBoundFeatureWrapper<TabLayoutMediator>()
    private val tabCounterBinding = ViewBoundFeatureWrapper<TabCounterBinding>()
    private val floatingActionButtonBinding = ViewBoundFeatureWrapper<FloatingActionButtonBinding>()
    private val selectionBannerBinding = ViewBoundFeatureWrapper<SelectionBannerBinding>()
    private val selectionHandleBinding = ViewBoundFeatureWrapper<SelectionHandleBinding>()
    private val tabsTrayCtaBinding = ViewBoundFeatureWrapper<TabsTrayInfoBannerBinding>()
    private val secureTabsTrayBinding = ViewBoundFeatureWrapper<SecureTabsTrayBinding>()
    private val tabsFeature = ViewBoundFeatureWrapper<TabsFeature>()
    private val tabsTrayInactiveTabsOnboardingBinding = ViewBoundFeatureWrapper<TabsTrayInactiveTabsOnboardingBinding>()
    private val syncedTabsIntegration = ViewBoundFeatureWrapper<SyncedTabsIntegration>()
    private val bookmarksSharedViewModel: BookmarksSharedViewModel by activityViewModels()

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _tabsTrayBinding: ComponentTabstray2Binding? = null
    private val tabsTrayBinding get() = _tabsTrayBinding!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _tabsTrayDialogBinding: FragmentTabTrayDialogBinding? = null
    private val tabsTrayDialogBinding get() = _tabsTrayDialogBinding!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _fabButtonBinding: ComponentTabstrayFabBinding? = null
    private val fabButtonBinding get() = _fabButtonBinding!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _tabsTrayComposeBinding: ComponentTabstray3Binding? = null
    private val tabsTrayComposeBinding get() = _tabsTrayComposeBinding!!

    @Suppress("VariableNaming")
    internal var _fabButtonComposeBinding: ComponentTabstray3FabBinding? = null
    private val fabButtonComposeBinding get() = _fabButtonComposeBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TabTrayDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args by navArgs<TabsTrayFragmentArgs>()
        val initialMode = if (args.enterMultiselect) {
            TabsTrayState.Mode.Select(emptySet())
        } else {
            TabsTrayState.Mode.Normal
        }
        val initialPage = args.page
        val activity = activity as HomeActivity

        tabsTrayStore = StoreProvider.get(this) {
            TabsTrayStore(
                initialState = TabsTrayState(
                    selectedPage = initialPage,
                    mode = initialMode,
                ),
            )
        }

        navigationInteractor =
            DefaultNavigationInteractor(
                browserStore = requireComponents.core.store,
                navController = findNavController(),
                dismissTabTray = ::dismissTabsTray,
                dismissTabTrayAndNavigateHome = ::dismissTabsTrayAndNavigateHome,
                showCancelledDownloadWarning = ::showCancelledDownloadWarning,
                accountManager = requireComponents.backgroundServices.accountManager,
            )

        tabsTrayController = DefaultTabsTrayController(
            activity = activity,
            appStore = requireComponents.appStore,
            tabsTrayStore = tabsTrayStore,
            browserStore = requireComponents.core.store,
            settings = requireContext().settings(),
            browsingModeManager = activity.browsingModeManager,
            navController = findNavController(),
            navigateToHomeAndDeleteSession = ::navigateToHomeAndDeleteSession,
            navigationInteractor = navigationInteractor,
            profiler = requireComponents.core.engine.profiler,
            tabsUseCases = requireComponents.useCases.tabsUseCases,
            bookmarksUseCase = requireComponents.useCases.bookmarksUseCases,
            ioDispatcher = Dispatchers.IO,
            collectionStorage = requireComponents.core.tabCollectionStorage,
            selectTabPosition = ::selectTabPosition,
            dismissTray = ::dismissTabsTray,
            showUndoSnackbarForTab = ::showUndoSnackbarForTab,
            showCancelledDownloadWarning = ::showCancelledDownloadWarning,
            showCollectionSnackbar = ::showCollectionSnackbar,
            showBookmarkSnackbar = ::showBookmarkSnackbar,
            bookmarksSharedViewModel = bookmarksSharedViewModel,
        )

        tabsTrayInteractor = DefaultTabsTrayInteractor(
            controller = tabsTrayController,
        )
        tabsTrayDialog = TabsTrayDialog(requireContext(), theme) { tabsTrayInteractor }
        return tabsTrayDialog
    }

    override fun onPause() {
        super.onPause()
        dialog?.window?.setWindowAnimations(R.style.DialogFragmentRestoreAnimation)
    }

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _tabsTrayDialogBinding = FragmentTabTrayDialogBinding.inflate(
            inflater,
            container,
            false,
        )

        if (requireContext().settings().enableTabsTrayToCompose) {
            _tabsTrayComposeBinding = ComponentTabstray3Binding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )

            _fabButtonComposeBinding = ComponentTabstray3FabBinding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )

            tabsTrayComposeBinding.root.setContent {
                WaterfoxTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                    TabsTray(
                        appStore = requireComponents.appStore,
                        browserStore = requireComponents.core.store,
                        tabsTrayStore = tabsTrayStore,
                        storage = requireComponents.core.thumbnailStorage,
                        displayTabsInGrid = requireContext().settings().gridTabView,
                        isInDebugMode = Config.channel.isDebug ||
                            requireComponents.settings.showSecretDebugMenuThisSession,
                        shouldShowTabAutoCloseBanner = requireContext().settings().shouldShowAutoCloseTabsBanner &&
                            requireContext().settings().canShowCfr,
                        shouldShowInactiveTabsAutoCloseDialog =
                        requireContext().settings()::shouldShowInactiveTabsAutoCloseDialog,
                        onTabPageClick = { page ->
                            tabsTrayInteractor.onTrayPositionSelected(page.ordinal, false)
                        },
                        onTabClose = { tab ->
                            tabsTrayInteractor.onTabClosed(tab, TABS_TRAY_FEATURE_NAME)
                        },
                        onTabMediaClick = tabsTrayInteractor::onMediaClicked,
                        onTabClick = { tab ->
                            tabsTrayInteractor.onTabSelected(tab, TABS_TRAY_FEATURE_NAME)
                        },
                        onTabLongClick = tabsTrayInteractor::onTabLongClicked,
                        onInactiveTabsHeaderClick = tabsTrayInteractor::onInactiveTabsHeaderClicked,
                        onDeleteAllInactiveTabsClick = tabsTrayInteractor::onDeleteAllInactiveTabsClicked,
                        onInactiveTabsAutoCloseDialogShown = {},
                        onInactiveTabAutoCloseDialogCloseButtonClick =
                        tabsTrayInteractor::onAutoCloseDialogCloseButtonClicked,
                        onEnableInactiveTabAutoCloseClick = {
                            tabsTrayInteractor.onEnableAutoCloseClicked()
                            showInactiveTabsAutoCloseConfirmationSnackbar()
                        },
                        onInactiveTabClick = tabsTrayInteractor::onInactiveTabClicked,
                        onInactiveTabClose = tabsTrayInteractor::onInactiveTabClosed,
                        onSyncedTabClick = tabsTrayInteractor::onSyncedTabClicked,
                        onSaveToCollectionClick = tabsTrayInteractor::onAddSelectedTabsToCollectionClicked,
                        onShareSelectedTabsClick = tabsTrayInteractor::onShareSelectedTabs,
                        onShareAllTabsClick = {
                            navigationInteractor.onShareTabsOfTypeClicked(
                                private = tabsTrayStore.state.selectedPage == Page.PrivateTabs,
                            )
                        },
                        onTabSettingsClick = navigationInteractor::onTabSettingsClicked,
                        onRecentlyClosedClick = navigationInteractor::onOpenRecentlyClosedClicked,
                        onAccountSettingsClick = navigationInteractor::onAccountSettingsClicked,
                        onDeleteAllTabsClick = {
                            navigationInteractor.onCloseAllTabsClicked(
                                private = tabsTrayStore.state.selectedPage == Page.PrivateTabs,
                            )
                        },
                        onDeleteSelectedTabsClick = tabsTrayInteractor::onDeleteSelectedTabsClicked,
                        onBookmarkSelectedTabsClick = tabsTrayInteractor::onBookmarkSelectedTabsClicked,
                        onForceSelectedTabsAsInactiveClick = tabsTrayInteractor::onForceSelectedTabsAsInactiveClicked,
                        onTabsTrayDismiss = ::onTabsTrayDismissed,
                        onTabAutoCloseBannerViewOptionsClick = {
                            navigationInteractor.onTabSettingsClicked()
                            requireContext().settings().shouldShowAutoCloseTabsBanner = false
                        },
                        onTabAutoCloseBannerDismiss = {
                            requireContext().settings().shouldShowAutoCloseTabsBanner = false
                        },
                        onTabAutoCloseBannerShown = {
                            requireContext().settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
                        },
                        onMove = tabsTrayInteractor::onTabsMove,
                    )
                }
            }

            fabButtonComposeBinding.root.setContent {
                WaterfoxTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                    TabsTrayFab(
                        tabsTrayStore = tabsTrayStore,
                        isSignedIn = requireContext().settings().signedInFxaAccount,
                        onNormalTabsFabClicked = tabsTrayInteractor::onNormalTabsFabClicked,
                        onPrivateTabsFabClicked = tabsTrayInteractor::onPrivateTabsFabClicked,
                        onSyncedTabsFabClicked = tabsTrayInteractor::onSyncedTabsFabClicked,
                    )
                }
            }
        } else {
            _tabsTrayBinding = ComponentTabstray2Binding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )
            _fabButtonBinding = ComponentTabstrayFabBinding.inflate(
                inflater,
                tabsTrayDialogBinding.root,
                true,
            )
        }

        return tabsTrayDialogBinding.root
    }

    override fun onStart() {
        super.onStart()
        findPreviousDialogFragment()?.let { dialog ->
            dialog.onAcceptClicked = ::onCancelDownloadWarningAccepted
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _tabsTrayBinding = null
        _tabsTrayDialogBinding = null
        _fabButtonBinding = null
        _tabsTrayComposeBinding = null
        _fabButtonComposeBinding = null
    }

    @Suppress("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rootView = if (requireContext().settings().enableTabsTrayToCompose) {
            tabsTrayComposeBinding.root
        } else {
            tabsTrayBinding.tabWrapper
        }

        val newTabFab = if (requireContext().settings().enableTabsTrayToCompose) {
            fabButtonComposeBinding.root
        } else {
            fabButtonBinding.newTabButton
        }

        val behavior = BottomSheetBehavior.from(rootView).apply {
            addBottomSheetCallback(
                TraySheetBehaviorCallback(
                    this,
                    navigationInteractor,
                    tabsTrayDialog,
                    newTabFab,
                ),
            )
            skipCollapsed = true
        }

        trayBehaviorManager = TabSheetBehaviorManager(
            behavior = behavior,
            orientation = resources.configuration.orientation,
            maxNumberOfTabs = max(
                requireContext().components.core.store.state.normalTabs.size,
                requireContext().components.core.store.state.privateTabs.size,
            ),
            numberForExpandingTray = if (requireContext().settings().gridTabView) {
                EXPAND_AT_GRID_SIZE
            } else {
                EXPAND_AT_LIST_SIZE
            },
            displayMetrics = requireContext().resources.displayMetrics,
        )

        setupBackgroundDismissalListener {
            onTabsTrayDismissed()
        }

        if (!requireContext().settings().enableTabsTrayToCompose) {
            val activity = activity as HomeActivity

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                fabButtonBinding.newTabButton.accessibilityTraversalAfter =
                    tabsTrayBinding.tabLayout.id
            }

            setupMenu(navigationInteractor)
            setupPager(
                context = view.context,
                lifecycleOwner = viewLifecycleOwner,
                store = tabsTrayStore,
                trayInteractor = tabsTrayInteractor,
            )

            tabsTrayCtaBinding.set(
                feature = TabsTrayInfoBannerBinding(
                    context = view.context,
                    store = requireComponents.core.store,
                    infoBannerView = tabsTrayBinding.infoBanner,
                    settings = requireComponents.settings,
                    navigationInteractor = navigationInteractor,
                ),
                owner = this,
                view = view,
            )

            tabLayoutMediator.set(
                feature = TabLayoutMediator(
                    tabLayout = tabsTrayBinding.tabLayout,
                    tabPager = tabsTrayBinding.tabsTray,
                    interactor = tabsTrayInteractor,
                    browsingModeManager = activity.browsingModeManager,
                    tabsTrayStore = tabsTrayStore,
                ),
                owner = this,
                view = view,
            )

            val tabsTrayTabCounter2Binding = TabsTrayTabCounter2Binding.bind(
                tabsTrayBinding.tabLayout,
            )

            tabCounterBinding.set(
                feature = TabCounterBinding(
                    store = requireComponents.core.store,
                    counter = tabsTrayTabCounter2Binding.tabCounter,
                ),
                owner = this,
                view = view,
            )

            floatingActionButtonBinding.set(
                feature = FloatingActionButtonBinding(
                    store = tabsTrayStore,
                    actionButton = fabButtonBinding.newTabButton,
                    interactor = tabsTrayInteractor,
                    isSignedIn = requireContext().settings().signedInFxaAccount,
                ),
                owner = this,
                view = view,
            )

            val tabsTrayMultiselectItemsBinding = TabstrayMultiselectItemsBinding.bind(
                tabsTrayBinding.root,
            )

            selectionBannerBinding.set(
                feature = SelectionBannerBinding(
                    context = requireContext(),
                    binding = tabsTrayBinding,
                    store = tabsTrayStore,
                    interactor = tabsTrayInteractor,
                    backgroundView = tabsTrayBinding.topBar,
                    showOnSelectViews = VisibilityModifier(
                        tabsTrayMultiselectItemsBinding.collectMultiSelect,
                        tabsTrayMultiselectItemsBinding.shareMultiSelect,
                        tabsTrayMultiselectItemsBinding.menuMultiSelect,
                        tabsTrayBinding.multiselectTitle,
                        tabsTrayBinding.exitMultiSelect,
                    ),
                    showOnNormalViews = VisibilityModifier(
                        tabsTrayBinding.tabLayout,
                        tabsTrayBinding.tabTrayOverflow,
                        fabButtonBinding.newTabButton,
                    ),
                ),
                owner = this,
                view = view,
            )

            selectionHandleBinding.set(
                feature = SelectionHandleBinding(
                    store = tabsTrayStore,
                    handle = tabsTrayBinding.handle,
                    containerLayout = tabsTrayBinding.tabWrapper,
                ),
                owner = this,
                view = view,
            )

            tabsTrayInactiveTabsOnboardingBinding.set(
                feature = TabsTrayInactiveTabsOnboardingBinding(
                    context = requireContext(),
                    store = requireComponents.core.store,
                    tabsTrayBinding = tabsTrayBinding,
                    settings = requireComponents.settings,
                    navigationInteractor = navigationInteractor,
                ),
                owner = this,
                view = view,
            )
        }

        tabsFeature.set(
            feature = TabsFeature(
                tabsTray = TabSorter(
                    requireContext().settings(),
                    tabsTrayStore,
                ),
                store = requireContext().components.core.store,
            ),
            owner = this,
            view = view,
        )

        secureTabsTrayBinding.set(
            feature = SecureTabsTrayBinding(
                store = tabsTrayStore,
                settings = requireComponents.settings,
                fragment = this,
                dialog = dialog as TabsTrayDialog,
            ),
            owner = this,
            view = view,
        )

        syncedTabsIntegration.set(
            feature = SyncedTabsIntegration(
                store = tabsTrayStore,
                context = requireContext(),
                navController = findNavController(),
                storage = requireComponents.backgroundServices.syncedTabsStorage,
                accountManager = requireComponents.backgroundServices.accountManager,
                lifecycleOwner = this,
            ),
            owner = this,
            view = view,
        )

        setFragmentResultListener(ShareFragment.RESULT_KEY) { _, _ ->
            dismissTabsTray()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        trayBehaviorManager.updateDependingOnOrientation(newConfig.orientation)
        if (!requireContext().settings().enableTabsTrayToCompose && requireContext().settings().gridTabView) {
            tabsTrayBinding.tabsTray.adapter?.notifyDataSetChanged()
        }
    }

    @VisibleForTesting
    internal fun onCancelDownloadWarningAccepted(tabId: String?, source: String?) {
        if (tabId != null) {
            tabsTrayInteractor.onDeletePrivateTabWarningAccepted(tabId, source)
        } else {
            navigationInteractor.onCloseAllPrivateTabsWarningConfirmed(private = true)
        }
    }

    @VisibleForTesting
    internal fun showCancelledDownloadWarning(downloadCount: Int, tabId: String?, source: String?) {
        val dialog = DownloadCancelDialogFragment.newInstance(
            downloadCount = downloadCount,
            tabId = tabId,
            source = source,
            promptStyling = DownloadCancelDialogFragment.PromptStyling(
                gravity = Gravity.BOTTOM,
                shouldWidthMatchParent = true,
                positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                    R.attr.accent,
                    requireContext(),
                ),
                positiveButtonTextColor = ThemeManager.resolveAttribute(
                    R.attr.textOnColorPrimary,
                    requireContext(),
                ),
                positiveButtonRadius = (resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat(),
            ),

            onPositiveButtonClicked = ::onCancelDownloadWarningAccepted,
        )
        dialog.show(parentFragmentManager, DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG)
    }

    @VisibleForTesting
    internal fun showUndoSnackbarForTab(isPrivate: Boolean) {
        val snackbarMessage =
            when (isPrivate) {
                true -> getString(R.string.snackbar_private_tab_closed)
                false -> getString(R.string.snackbar_tab_closed)
            }
        val pagePosition = if (isPrivate) Page.PrivateTabs.ordinal else Page.NormalTabs.ordinal

        lifecycleScope.allowUndo(
            view = requireView(),
            message = snackbarMessage,
            undoActionTitle = getString(R.string.snackbar_deleted_undo),
            onCancel = {
                requireComponents.useCases.tabsUseCases.undo.invoke()

                if (requireContext().settings().enableTabsTrayToCompose) {
                    tabsTrayStore.dispatch(TabsTrayAction.PageSelected(Page.positionToPage(pagePosition)))
                } else {
                    tabLayoutMediator.withFeature {
                        it.selectTabAtPosition(pagePosition)
                    }
                }
            },
            operation = { },
            elevation = ELEVATION,
            anchorView = getSnackbarAnchor(),
        )
    }

    @VisibleForTesting
    internal fun setupPager(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        store: TabsTrayStore,
        trayInteractor: TabsTrayInteractor,
    ) {
        tabsTrayBinding.tabsTray.apply {
            adapter = TrayPagerAdapter(
                context = context,
                lifecycleOwner = lifecycleOwner,
                tabsTrayStore = store,
                interactor = trayInteractor,
                browserStore = requireComponents.core.store,
                appStore = requireComponents.appStore,
            )
            isUserInputEnabled = false
        }
    }

    @VisibleForTesting
    internal fun setupMenu(navigationInteractor: NavigationInteractor) {
        tabsTrayBinding.tabTrayOverflow.setOnClickListener { anchor ->
            val menu = getTrayMenu(
                context = requireContext(),
                browserStore = requireComponents.core.store,
                tabsTrayStore = tabsTrayStore,
                tabLayout = tabsTrayBinding.tabLayout,
                navigationInteractor = navigationInteractor,
            ).build()

            menu.showWithTheme(anchor)
        }
    }

    @VisibleForTesting
    internal fun getTrayMenu(
        context: Context,
        browserStore: BrowserStore,
        tabsTrayStore: TabsTrayStore,
        tabLayout: TabLayout,
        navigationInteractor: NavigationInteractor,
    ) = MenuIntegration(context, browserStore, tabsTrayStore, tabLayout, navigationInteractor)

    @VisibleForTesting
    internal fun setupBackgroundDismissalListener(block: (View) -> Unit) {
        tabsTrayDialogBinding.tabLayout.setOnClickListener(block)
        if (!requireContext().settings().enableTabsTrayToCompose) {
            tabsTrayBinding.handle.setOnClickListener(block)
        }
    }

    @VisibleForTesting
    internal fun dismissTabsTrayAndNavigateHome(sessionId: String) {
        navigateToHomeAndDeleteSession(sessionId)
        dismissTabsTray()
    }

    internal val homeViewModel: HomeScreenViewModel by activityViewModels()

    @VisibleForTesting
    internal fun navigateToHomeAndDeleteSession(sessionId: String) {
        homeViewModel.sessionToDelete = sessionId
        val directions = NavGraphDirections.actionGlobalHome()
        findNavController().navigate(directions)
    }

    @VisibleForTesting
    internal fun selectTabPosition(position: Int, smoothScroll: Boolean) {
        if (!requireContext().settings().enableTabsTrayToCompose) {
            tabsTrayBinding.tabsTray.setCurrentItem(position, smoothScroll)
            tabsTrayBinding.tabLayout.getTabAt(position)?.select()
        }
    }

    @VisibleForTesting
    internal fun dismissTabsTray() {
        // This should always be the last thing we do because nothing (e.g. telemetry)
        // is guaranteed after that.
        dismissAllowingStateLoss()
    }

    @VisibleForTesting
    internal fun showCollectionSnackbar(
        tabSize: Int,
        isNewCollection: Boolean = false,
    ) {
        runIfFragmentIsAttached {
            WaterfoxSnackbar
                .make(requireView())
                .collectionMessage(tabSize, isNewCollection)
                .anchorWithAction(getSnackbarAnchor()) {
                    findNavController().navigate(
                        TabsTrayFragmentDirections.actionGlobalHome(
                            focusOnAddressBar = false,
                            scrollToCollection = true,
                        ),
                    )
                    dismissTabsTray()
                }.show()
        }
    }

    @VisibleForTesting
    internal fun showBookmarkSnackbar(
        tabSize: Int,
    ) {
        WaterfoxSnackbar
            .make(requireView())
            .bookmarkMessage(tabSize)
            .anchorWithAction(getSnackbarAnchor()) {
                findNavController().navigate(
                    TabsTrayFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id),
                )
                dismissTabsTray()
            }
            .show()
    }

    @Suppress("MaxLineLength")
    private fun findPreviousDialogFragment(): DownloadCancelDialogFragment? {
        return parentFragmentManager.findFragmentByTag(DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG) as? DownloadCancelDialogFragment
    }

    private fun getSnackbarAnchor(): View? = when {
        requireContext().settings().enableTabsTrayToCompose -> fabButtonComposeBinding.root
        fabButtonBinding.newTabButton.isVisible -> fabButtonBinding.newTabButton
        else -> null
    }

    private fun showInactiveTabsAutoCloseConfirmationSnackbar() {
        val text = getString(R.string.inactive_tabs_auto_close_message_snackbar)
        val snackbar = WaterfoxSnackbar.make(
            view = tabsTrayComposeBinding.root,
            duration = WaterfoxSnackbar.LENGTH_SHORT,
            isDisplayedWithBrowserToolbar = true,
        ).setText(text)
        snackbar.view.elevation = ELEVATION
        snackbar.show()
    }

    private fun onTabsTrayDismissed() {
        dismissAllowingStateLoss()
    }

    companion object {
        private const val DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG = "DOWNLOAD_CANCEL_DIALOG_FRAGMENT_TAG"

        // Minimum number of list items for which to show the tabs tray as expanded.
        const val EXPAND_AT_LIST_SIZE = 4

        // Minimum number of grid items for which to show the tabs tray as expanded.
        private const val EXPAND_AT_GRID_SIZE = 3

        // Elevation for undo toasts
        @VisibleForTesting
        internal const val ELEVATION = 80f

        private const val TABS_TRAY_FEATURE_NAME = "Tabs tray"
    }
}
