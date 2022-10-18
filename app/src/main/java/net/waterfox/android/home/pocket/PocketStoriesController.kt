/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.home.pocket

import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import mozilla.components.service.pocket.PocketStory
import net.waterfox.android.BrowserDirection
import net.waterfox.android.HomeActivity
import net.waterfox.android.R
import net.waterfox.android.components.AppStore
import net.waterfox.android.components.appstate.AppAction

/**
 * Contract for how all user interactions with the Pocket stories feature are to be handled.
 */
interface PocketStoriesController {
    /**
     * Callback to decide what should happen as an effect of a specific story being shown.
     *
     * @param storyShown The just shown [PocketStory].
     * @param storyPosition `row x column` matrix representing the grid position of the shown story.
     */
    fun handleStoryShown(storyShown: PocketStory, storyPosition: Pair<Int, Int>)

    /**
     * Callback to decide what should happen as an effect of a new list of stories being shown.
     *
     * @param storiesShown the new list of [PocketStory]es shown to the user.
     */
    fun handleStoriesShown(storiesShown: List<PocketStory>)

    /**
     * Callback allowing to handle a specific [PocketRecommendedStoriesCategory] being clicked by the user.
     *
     * @param categoryClicked the just clicked [PocketRecommendedStoriesCategory].
     */
    fun handleCategoryClick(categoryClicked: PocketRecommendedStoriesCategory)

    /**
     * Callback for when the user clicks on a specific story.
     *
     * @param storyClicked The just clicked [PocketStory].
     * @param storyPosition `row x column` matrix representing the grid position of the clicked story.
     */
    fun handleStoryClicked(storyClicked: PocketStory, storyPosition: Pair<Int, Int>)

    /**
     * Callback for when the "Learn more" link is clicked.
     *
     * @param link URL clicked.
     */
    fun handleLearnMoreClicked(link: String)

    /**
     * Callback for when the "Discover more" link is clicked.
     *
     * @param link URL clicked.
     */
    fun handleDiscoverMoreClicked(link: String)
}

/**
 * Default behavior for handling all user interactions with the Pocket recommended stories feature.
 *
 * @param homeActivity [HomeActivity] used to open URLs in a new tab.
 * @param appStore [AppStore] from which to read the current Pocket recommendations and dispatch new actions on.
 * @param navController [NavController] used for navigation.
 */
internal class DefaultPocketStoriesController(
    private val homeActivity: HomeActivity,
    private val appStore: AppStore,
    private val navController: NavController,
) : PocketStoriesController {
    override fun handleStoryShown(
        storyShown: PocketStory,
        storyPosition: Pair<Int, Int>
    ) {
        appStore.dispatch(AppAction.PocketStoriesShown(listOf(storyShown)))
    }

    override fun handleStoriesShown(storiesShown: List<PocketStory>) {
        appStore.dispatch(AppAction.PocketStoriesShown(storiesShown))
    }

    override fun handleCategoryClick(categoryClicked: PocketRecommendedStoriesCategory) {
        val initialCategoriesSelections = appStore.state.pocketStoriesCategoriesSelections

        // First check whether the category is clicked to be deselected.
        if (initialCategoriesSelections.map { it.name }.contains(categoryClicked.name)) {
            appStore.dispatch(AppAction.DeselectPocketStoriesCategory(categoryClicked.name))
            return
        }

        // If a new category is clicked to be selected:
        // Ensure the number of categories selected at a time is capped.
        val oldestCategoryToDeselect =
            if (initialCategoriesSelections.size == POCKET_CATEGORIES_SELECTED_AT_A_TIME_COUNT) {
                initialCategoriesSelections.minByOrNull { it.selectionTimestamp }
            } else {
                null
            }
        oldestCategoryToDeselect?.let {
            appStore.dispatch(AppAction.DeselectPocketStoriesCategory(it.name))
        }

        // Finally update the selection.
        appStore.dispatch(AppAction.SelectPocketStoriesCategory(categoryClicked.name))
    }

    override fun handleStoryClicked(
        storyClicked: PocketStory,
        storyPosition: Pair<Int, Int>
    ) {
        dismissSearchDialogIfDisplayed()
        homeActivity.openToBrowserAndLoad(storyClicked.url, true, BrowserDirection.FromHome)
    }

    override fun handleLearnMoreClicked(link: String) {
        dismissSearchDialogIfDisplayed()
        homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome)
    }

    override fun handleDiscoverMoreClicked(link: String) {
        dismissSearchDialogIfDisplayed()
        homeActivity.openToBrowserAndLoad(link, true, BrowserDirection.FromHome)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun dismissSearchDialogIfDisplayed() {
        if (navController.currentDestination?.id == R.id.searchDialogFragment) {
            navController.navigateUp()
        }
    }
}
