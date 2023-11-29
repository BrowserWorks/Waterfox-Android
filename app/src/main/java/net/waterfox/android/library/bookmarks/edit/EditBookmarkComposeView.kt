/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.library.bookmarks.edit

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.waterfox.android.R
import net.waterfox.android.compose.ClearableEditText
import net.waterfox.android.theme.Theme
import net.waterfox.android.theme.WaterfoxTheme

@OptIn(ExperimentalComposeUiApi::class)
class EditBookmarkComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private lateinit var _bookmarkName: MutableState<String?>
    var bookmarkName: String?
        get() = _bookmarkName.value
        set(value) {
            _bookmarkName.value = value
        }

    private lateinit var _bookmarkUrl: MutableState<String?>
    var bookmarkUrl: String?
        get() = _bookmarkUrl.value
        set(value) {
            _bookmarkUrl.value = value
        }

    var bookmarkNameErrorMessage by mutableStateOf<String?>(null)
    var bookmarkUrlVisible by mutableStateOf(true)
    var bookmarkUrlErrorMessage by mutableStateOf<String?>(null)
    var bookmarkUrlErrorDrawable by mutableStateOf<Int?>(null)
    var bookmarkParentFolder by mutableStateOf<String?>("")
    var onBookmarkParentFolderClick by mutableStateOf({})
    var progressBarVisible by mutableStateOf(false)

    private var _keyboardController: SoftwareKeyboardController? = null

    fun hideKeyboard() {
        _keyboardController?.hide()
    }

    @Composable
    override fun Content() {
        _bookmarkName = rememberSaveable { mutableStateOf("") }
        _bookmarkUrl = rememberSaveable { mutableStateOf("") }
        _keyboardController = LocalSoftwareKeyboardController.current
        val bookmarkNameFocusRequester = remember {
            FocusRequester()
        }

        WaterfoxTheme(theme = Theme.getTheme()) {
            Column {
                if (progressBarVisible) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .offset(y = (-3).dp),
                    )
                }

                ClearableEditText(
                    modifier = Modifier
                        .focusRequester(bookmarkNameFocusRequester)
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "edit.bookmark.name"
                        },
                    label = stringResource(R.string.bookmark_name_label),
                    value = _bookmarkName.value ?: "",
                    onValueChanged = { _bookmarkName.value = it },
                    onClearClicked = { _bookmarkName.value = "" },
                    errorMessage = bookmarkNameErrorMessage,
                    keyboardType = KeyboardType.Text,
                )

                if (bookmarkUrlVisible) {
                    ClearableEditText(
                        modifier = Modifier.semantics {
                            testTagsAsResourceId = true
                            testTag = "edit.bookmark.url"
                        },
                        label = stringResource(R.string.bookmark_url_label),
                        value = _bookmarkUrl.value ?: "",
                        onValueChanged = {
                            _bookmarkUrl.value = it
                            bookmarkUrlErrorMessage = null
                            bookmarkUrlErrorDrawable = null
                        },
                        onClearClicked = { _bookmarkUrl.value = "" },
                        errorMessage = bookmarkUrlErrorMessage,
                        errorDrawable = bookmarkUrlErrorDrawable,
                        keyboardType = KeyboardType.Uri,
                    )
                }

                Text(
                    text = stringResource(id = R.string.bookmark_folder_label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 8.dp),
                    color = WaterfoxTheme.colors.textPrimary,
                    fontSize = 12.sp,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(start = 4.dp, top = 16.dp)
                        .clickable { onBookmarkParentFolderClick() }
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "edit.bookmark.parent.folder"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_icon),
                        contentDescription = null,
                        tint = WaterfoxTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bookmarkParentFolder ?: "",
                        color = WaterfoxTheme.colors.textSecondary,
                        fontSize = 16.sp,
                    )
                }
            }
        }

        LaunchedEffect(bookmarkNameFocusRequester) {
            bookmarkNameFocusRequester.requestFocus()
        }
    }
}
