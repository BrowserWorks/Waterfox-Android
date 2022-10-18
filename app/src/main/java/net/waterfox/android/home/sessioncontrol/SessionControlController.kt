/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.sessioncontrol

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.state.availableSearchEngines
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tab.collections.ext.invoke
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.support.ktx.android.view.showKeyboard
import net.waterfox.android.BrowserDirection
import net.waterfox.android.FeatureFlags
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.browser.BrowserFragmentDirections
import net.waterfox.android.browser.browsingmode.BrowsingMode
import net.waterfox.android.collections.SaveCollectionStep
import net.waterfox.android.components.AppStore
import net.waterfox.android.components.TabCollectionStorage
import net.waterfox.android.components.appstate.AppAction
import net.waterfox.android.ext.components
import net.waterfox.android.ext.nav
import net.waterfox.android.ext.settings
import net.waterfox.android.gleanplumb.Message
import net.waterfox.android.gleanplumb.MessageController
import net.waterfox.android.home.HomeFragment
import net.waterfox.android.home.HomeFragmentDirections
import net.waterfox.android.home.Mode
import net.waterfox.android.settings.SupportUtils
import net.waterfox.android.settings.SupportUtils.SumoTopic.PRIVATE_BROWSING_MYTHS
import net.waterfox.android.utils.Settings
import java.util.*
import mozilla.components.feature.tab.collections.Tab as ComponentTab

/**
 * [HomeFragment] controller. An interface that handles the view manipulation of the Tabs triggered
 * by the Interactor.
 */
@Suppress("TooManyFunctions")
interface SessionControlController {
    /**
     * @see [CollectionInteractor.onCollectionAddTabTapped]
     */
    fun handleCollectionAddTabTapped(collection: TabCollection)

    /**
     * @see [CollectionInteractor.onCollectionOpenTabClicked]
     */
    fun handleCollectionOpenTabClicked(tab: ComponentTab)

    /**
     * @see [CollectionInteractor.onCollectionOpenTabsTapped]
     */
    fun handleCollectionOpenTabsTapped(collection: TabCollection)

    /**
     * @see [CollectionInteractor.onCollectionRemoveTab]
     */
    fun handleCollectionRemoveTab(collection: TabCollection, tab: ComponentTab, wasSwiped: Boolean)

    /**
     * @see [CollectionInteractor.onCollectionShareTabsClicked]
     */
    fun handleCollectionShareTabsClicked(collection: TabCollection)

    /**
     * @see [CollectionInteractor.onDeleteCollectionTapped]
     */
    fun handleDeleteCollectionTapped(collection: TabCollection)

    /**
     * @see [TopSiteInteractor.onOpenInPrivateTabClicked]
     */
    fun handleOpenInPrivateTabClicked(topSite: TopSite)

    /**
     * @see [TabSessionInteractor.onPrivateBrowsingLearnMoreClicked]
     */
    fun handlePrivateBrowsingLearnMoreClicked()

    /**
     * @see [TopSiteInteractor.onRenameTopSiteClicked]
     */
    fun handleRenameTopSiteClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onRemoveTopSiteClicked]
     */
    fun handleRemoveTopSiteClicked(topSite: TopSite)

    /**
     * @see [CollectionInteractor.onRenameCollectionTapped]
     */
    fun handleRenameCollectionTapped(collection: TabCollection)

    /**
     * @see [TopSiteInteractor.onSelectTopSite]
     */
    fun handleSelectTopSite(topSite: TopSite, position: Int)

    /**
     * @see [TopSiteInteractor.onSettingsClicked]
     */
    fun handleTopSiteSettingsClicked()

    /**
     * @see [TopSiteInteractor.onSponsorPrivacyClicked]
     */
    fun handleSponsorPrivacyClicked()

    /**
     * @see [OnboardingInteractor.onStartBrowsingClicked]
     */
    fun handleStartBrowsingClicked()

    /**
     * @see [OnboardingInteractor.onReadPrivacyNoticeClicked]
     */
    fun handleReadPrivacyNoticeClicked()

    /**
     * @see [CollectionInteractor.onToggleCollectionExpanded]
     */
    fun handleToggleCollectionExpanded(collection: TabCollection, expand: Boolean)

    /**
     * @see [ToolbarInteractor.onPasteAndGo]
     */
    fun handlePasteAndGo(clipboardText: String)

    /**
     * @see [ToolbarInteractor.onPaste]
     */
    fun handlePaste(clipboardText: String)

    /**
     * @see [CollectionInteractor.onAddTabsToCollectionTapped]
     */
    fun handleCreateCollection()

    /**
     * @see [CollectionInteractor.onRemoveCollectionsPlaceholder]
     */
    fun handleRemoveCollectionsPlaceholder()

    /**
     * @see [CollectionInteractor.onCollectionMenuOpened] and [TopSiteInteractor.onTopSiteMenuOpened]
     */
    fun handleMenuOpened()

    /**
     * @see [MessageCardInteractor.onMessageClicked]
     */
    fun handleMessageClicked(message: Message)

    /**
     * @see [MessageCardInteractor.onMessageClosedClicked]
     */
    fun handleMessageClosed(message: Message)

    /**
     * @see [TabSessionInteractor.onPrivateModeButtonClicked]
     */
    fun handlePrivateModeButtonClicked(newMode: BrowsingMode, userHasBeenOnboarded: Boolean)

    /**
     * @see [CustomizeHomeIteractor.openCustomizeHomePage]
     */
    fun handleCustomizeHomeTapped()

    /**
     * @see [OnboardingInteractor.showOnboardingDialog]
     */
    fun handleShowOnboardingDialog()
}

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class DefaultSessionControlController(
    private val activity: HomeActivity,
    private val settings: Settings,
    private val engine: Engine,
    private val messageController: MessageController,
    private val store: BrowserStore,
    private val tabCollectionStorage: TabCollectionStorage,
    private val addTabUseCase: TabsUseCases.AddNewTabUseCase,
    private val restoreUseCase: TabsUseCases.RestoreUseCase,
    private val reloadUrlUseCase: SessionUseCases.ReloadUrlUseCase,
    private val selectTabUseCase: TabsUseCases.SelectTabUseCase,
    private val appStore: AppStore,
    private val navController: NavController,
    private val viewLifecycleScope: CoroutineScope,
    private val hideOnboarding: () -> Unit,
    private val registerCollectionStorageObserver: () -> Unit,
    private val removeCollectionWithUndo: (tabCollection: TabCollection) -> Unit,
    private val showTabTray: () -> Unit
) : SessionControlController {

    override fun handleCollectionAddTabTapped(collection: TabCollection) {
        showCollectionCreationFragment(
            step = SaveCollectionStep.SelectTabs,
            selectedTabCollectionId = collection.id
        )
    }

    override fun handleMenuOpened() {
        dismissSearchDialogIfDisplayed()
    }

    override fun handleCollectionOpenTabClicked(tab: ComponentTab) {
        dismissSearchDialogIfDisplayed()

        restoreUseCase.invoke(
            activity,
            engine,
            tab,
            onTabRestored = {
                activity.openToBrowser(BrowserDirection.FromHome)
                selectTabUseCase.invoke(it)
                reloadUrlUseCase.invoke(it)
            },
            onFailure = {
                activity.openToBrowserAndLoad(
                    searchTermOrURL = tab.url,
                    newTab = true,
                    from = BrowserDirection.FromHome
                )
            }
        )
    }

    override fun handleCollectionOpenTabsTapped(collection: TabCollection) {
        restoreUseCase.invoke(
            activity,
            engine,
            collection,
            onFailure = { url ->
                addTabUseCase.invoke(url)
            }
        )

        showTabTray()
    }

    override fun handleCollectionRemoveTab(
        collection: TabCollection,
        tab: ComponentTab,
        wasSwiped: Boolean
    ) {
        if (collection.tabs.size == 1) {
            removeCollectionWithUndo(collection)
        } else {
            viewLifecycleScope.launch {
                tabCollectionStorage.removeTabFromCollection(collection, tab)
            }
        }
    }

    override fun handleCollectionShareTabsClicked(collection: TabCollection) {
        dismissSearchDialogIfDisplayed()
        showShareFragment(
            collection.title,
            collection.tabs.map { ShareData(url = it.url, title = it.title) }
        )
    }

    override fun handleDeleteCollectionTapped(collection: TabCollection) {
        removeCollectionWithUndo(collection)
    }

    override fun handleOpenInPrivateTabClicked(topSite: TopSite) {
        with(activity) {
            browsingModeManager.mode = BrowsingMode.Private
            openToBrowserAndLoad(
                searchTermOrURL = topSite.url,
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    override fun handlePrivateBrowsingLearnMoreClicked() {
        dismissSearchDialogIfDisplayed()
        activity.openToBrowserAndLoad(
            searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(PRIVATE_BROWSING_MYTHS),
            newTab = true,
            from = BrowserDirection.FromHome
        )
    }

    @SuppressLint("InflateParams")
    override fun handleRenameTopSiteClicked(topSite: TopSite) {
        activity.let {
            val customLayout =
                LayoutInflater.from(it).inflate(R.layout.top_sites_rename_dialog, null)
            val topSiteLabelEditText: EditText =
                customLayout.findViewById(R.id.top_site_title)
            topSiteLabelEditText.setText(topSite.title)

            AlertDialog.Builder(it).apply {
                setTitle(R.string.rename_top_site)
                setView(customLayout)
                setPositiveButton(R.string.top_sites_rename_dialog_ok) { dialog, _ ->
                    viewLifecycleScope.launch(Dispatchers.IO) {
                        with(activity.components.useCases.topSitesUseCase) {
                            updateTopSites(
                                topSite,
                                topSiteLabelEditText.text.toString(),
                                topSite.url
                            )
                        }
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.top_sites_rename_dialog_cancel) { dialog, _ ->
                    dialog.cancel()
                }
            }.show().also {
                topSiteLabelEditText.setSelection(0, topSiteLabelEditText.text.length)
                topSiteLabelEditText.showKeyboard()
            }
        }
    }

    override fun handleRemoveTopSiteClicked(topSite: TopSite) {
        viewLifecycleScope.launch(Dispatchers.IO) {
            with(activity.components.useCases.topSitesUseCase) {
                removeTopSites(topSite)
            }
        }
    }

    override fun handleRenameCollectionTapped(collection: TabCollection) {
        showCollectionCreationFragment(
            step = SaveCollectionStep.RenameCollection,
            selectedTabCollectionId = collection.id
        )
    }

    override fun handleSelectTopSite(topSite: TopSite, position: Int) {
        dismissSearchDialogIfDisplayed()

        val tabId = addTabUseCase.invoke(
            url = appendSearchAttributionToUrlIfNeeded(topSite.url),
            selectTab = true,
            startLoading = true
        )

        if (settings.openNextTabInDesktopMode) {
            activity.handleRequestDesktopMode(tabId)
        }
        activity.openToBrowser(BrowserDirection.FromHome)
    }

    override fun handleTopSiteSettingsClicked() {
        navController.nav(
            R.id.homeFragment,
            HomeFragmentDirections.actionGlobalHomeSettingsFragment()
        )
    }

    override fun handleSponsorPrivacyClicked() {
        activity.openToBrowserAndLoad(
            searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.SPONSOR_PRIVACY),
            newTab = true,
            from = BrowserDirection.FromHome
        )
    }

    @VisibleForTesting
    internal fun getAvailableSearchEngines() =
        activity.components.core.store.state.search.searchEngines +
            activity.components.core.store.state.search.availableSearchEngines

    /**
     * Append a search attribution query to any provided search engine URL based on the
     * user's current region.
     */
    private fun appendSearchAttributionToUrlIfNeeded(url: String): String {
        if (url == SupportUtils.GOOGLE_URL) {
            store.state.search.region?.let { region ->
                return when (region.current) {
                    "US" -> SupportUtils.GOOGLE_US_URL
                    else -> SupportUtils.GOOGLE_XX_URL
                }
            }
        }

        return url
    }

    private fun dismissSearchDialogIfDisplayed() {
        if (navController.currentDestination?.id == R.id.searchDialogFragment) {
            navController.navigateUp()
        }
    }

    override fun handleStartBrowsingClicked() {
        hideOnboarding()
    }

    override fun handleCustomizeHomeTapped() {
        val directions = HomeFragmentDirections.actionGlobalHomeSettingsFragment()
        navController.nav(navController.currentDestination?.id, directions)
    }

    override fun handleShowOnboardingDialog() {
        if (FeatureFlags.showHomeOnboarding) {
            navController.nav(
                R.id.homeFragment,
                HomeFragmentDirections.actionGlobalHomeOnboardingDialog()
            )
        }
    }

    override fun handleReadPrivacyNoticeClicked() {
        activity.openToBrowserAndLoad(
            searchTermOrURL = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE),
            newTab = true,
            from = BrowserDirection.FromHome
        )
    }

    override fun handleToggleCollectionExpanded(collection: TabCollection, expand: Boolean) {
        appStore.dispatch(AppAction.CollectionExpanded(collection, expand))
    }

    private fun showTabTrayCollectionCreation() {
        val directions = HomeFragmentDirections.actionGlobalTabsTrayFragment(
            enterMultiselect = true
        )
        navController.nav(R.id.homeFragment, directions)
    }

    private fun showCollectionCreationFragment(
        step: SaveCollectionStep,
        selectedTabIds: Array<String>? = null,
        selectedTabCollectionId: Long? = null
    ) {
        if (navController.currentDestination?.id == R.id.collectionCreationFragment) return

        // Only register the observer right before moving to collection creation
        registerCollectionStorageObserver()

        val tabIds = store.state
            .getNormalOrPrivateTabs(private = activity.browsingModeManager.mode.isPrivate)
            .map { session -> session.id }
            .toList()
            .toTypedArray()
        val directions = HomeFragmentDirections.actionGlobalCollectionCreationFragment(
            tabIds = tabIds,
            saveCollectionStep = step,
            selectedTabIds = selectedTabIds,
            selectedTabCollectionId = selectedTabCollectionId ?: -1
        )
        navController.nav(R.id.homeFragment, directions)
    }

    override fun handleCreateCollection() {
        showTabTrayCollectionCreation()
    }

    override fun handleRemoveCollectionsPlaceholder() {
        settings.showCollectionsPlaceholderOnHome = false
        appStore.dispatch(AppAction.RemoveCollectionsPlaceholder)
    }

    private fun showShareFragment(shareSubject: String, data: List<ShareData>) {
        val directions = HomeFragmentDirections.actionGlobalShareFragment(
            shareSubject = shareSubject,
            data = data.toTypedArray()
        )
        navController.nav(R.id.homeFragment, directions)
    }

    override fun handlePasteAndGo(clipboardText: String) {
        val searchEngine = store.state.search.selectedOrDefaultSearchEngine

        activity.openToBrowserAndLoad(
            searchTermOrURL = clipboardText,
            newTab = true,
            from = BrowserDirection.FromHome,
            engine = searchEngine
        )
    }

    override fun handlePaste(clipboardText: String) {
        val directions = HomeFragmentDirections.actionGlobalSearchDialog(
            sessionId = null,
            pastedText = clipboardText
        )
        navController.nav(R.id.homeFragment, directions)
    }

    override fun handleMessageClicked(message: Message) {
        messageController.onMessagePressed(message)
    }

    override fun handleMessageClosed(message: Message) {
        messageController.onMessageDismissed(message)
    }

    override fun handlePrivateModeButtonClicked(
        newMode: BrowsingMode,
        userHasBeenOnboarded: Boolean
    ) {
        if (newMode == BrowsingMode.Private) {
            activity.settings().incrementNumTimesPrivateModeOpened()
        }

        if (userHasBeenOnboarded) {
            appStore.dispatch(
                AppAction.ModeChange(Mode.fromBrowsingMode(newMode))
            )

            if (navController.currentDestination?.id == R.id.searchDialogFragment) {
                navController.navigate(
                    BrowserFragmentDirections.actionGlobalSearchDialog(
                        sessionId = null
                    )
                )
            }
        }
    }
}
