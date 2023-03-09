/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.advanced

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.support.locale.LocaleManager
import net.waterfox.android.R
import net.waterfox.android.theme.WaterfoxTheme
import java.util.*

interface LocaleSettingsViewInteractor {

    fun onLocaleSelected(locale: Locale)

    fun onDefaultLocaleSelected()

    fun onSearchQueryTyped(query: String)
}

@OptIn(ExperimentalComposeUiApi::class)
class LocaleSettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var localeList by mutableStateOf<List<Locale>>(emptyList())
    var selectedLocale by mutableStateOf(Locale.getDefault())

    lateinit var interactor: LocaleSettingsViewInteractor

    fun update(state: LocaleSettingsState) {
        localeList = state.searchedLocaleList
        selectedLocale = state.selectedLocale
    }

    @Composable
    override fun Content() {
        WaterfoxTheme {
            LazyColumn(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                    testTag = "locale.list"
                },
            ) {
                if (localeList.isNotEmpty()) {
                    val locale = localeList[0]
                    item(key = R.string.default_locale_text) {
                        LocaleItem(
                            title = stringResource(R.string.default_locale_text),
                            subtitle = locale.getDisplayName(locale).capitalize(locale),
                            selected = isCurrentLocaleSelected(locale, isDefault = true),
                            onClick = { interactor.onDefaultLocaleSelected() },
                        )
                    }
                }

                if (localeList.size > 1) {
                    items(
                        items = localeList.subList(1, localeList.size),
                        key = { locale -> locale.displayName },
                    ) { locale ->
                        LocaleItem(
                            title = getDisplayName(locale),
                            subtitle = locale.getProperDisplayName(),
                            selected = isCurrentLocaleSelected(locale, isDefault = false),
                            onClick = { interactor.onLocaleSelected(locale) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LocaleItem(
        title: String,
        subtitle: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        return Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(onClick = onClick),
        ) {
            if (selected) {
                Image(
                    painterResource(R.drawable.mozac_ic_check),
                    contentDescription = stringResource(R.string.a11y_selected_locale_content_description),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(
                            start = 16.dp,
                            end = 32.dp,
                        )
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "locale.selected"
                        },
                    colorFilter = ColorFilter.tint(WaterfoxTheme.colors.textPrimary),
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .width(72.dp)
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "locale.unselected"
                        },
                )
            }

            Column {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "locale.title"
                        },
                    color = WaterfoxTheme.colors.textPrimary,
                    fontSize = 16.sp,
                )

                Text(
                    text = subtitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "locale.subtitle"
                        },
                    color = WaterfoxTheme.colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }

    private fun getDisplayName(locale: Locale): String =
        locale.getDisplayName(locale).capitalize(locale)

    /**
     * Returns the locale in the selected language
     */
    private fun Locale.getProperDisplayName() =
        displayName.capitalize(Locale.getDefault())

    /**
     * Similar to Kotlin's capitalize with locale parameter, but that method is currently experimental
     */
    private fun String.capitalize(locale: Locale) =
        substring(0, 1).uppercase(locale) + substring(1)

    private fun isCurrentLocaleSelected(locale: Locale, isDefault: Boolean) = if (isDefault) {
        locale == selectedLocale && LocaleManager.isDefaultLocaleSelected(context)
    } else {
        locale == selectedLocale && !LocaleManager.isDefaultLocaleSelected(context)
    }

}
