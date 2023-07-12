/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.compose

import android.os.Build
import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    linkColor: Color = Color.Blue,
    linkEntire: Boolean = false,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    clickable: Boolean = true,
    onClickLink: ((linkText: String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val linkInfos = if (linkEntire) listOf(LinkInfo(text, 0, text.length)) else SpannableStr.getLinkInfos(text)
    val annotatedString = buildAnnotatedString {
        append(text)
        linkInfos.forEach {
            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                ),
                start = it.start,
                end = it.end
            )
            addStringAnnotation(
                tag = "tag",
                annotation = it.url,
                start = it.start,
                end = it.end
            )
        }
    }
    if (clickable) {
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style,
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let { result ->
                    if (linkEntire) {
                        onClickLink?.invoke(annotatedString.substring(result.start, result.end))
                    } else {
                        uriHandler.openUri(result.item)
                        onClickLink?.invoke(annotatedString.substring(result.start, result.end))
                    }
                }
            }
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style
        )
    }
}

@Composable
private fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    onClick: (Int) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            layoutResult.value?.let { layoutResult ->
                onClick(layoutResult.getOffsetForPosition(pos))
            }
        }
    }
    Text(
        text = text,
        modifier = modifier.then(pressIndicator),
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        },
        style = style
    )
}

private data class LinkInfo(
    val url: String,
    val start: Int,
    val end: Int
)

private class SpannableStr(source: CharSequence): SpannableString(source) {
    companion object {
        fun getLinkInfos(text: String): List<LinkInfo> {
            val spannableStr = SpannableStr(text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Linkify.addLinks(spannableStr, Linkify.ALL) { str: String -> URLSpan(str)  }
            } else {
                Linkify.addLinks(spannableStr, Linkify.ALL)
            }
            return spannableStr.linkInfos
        }
    }
    private inner class Data(
        val what: Any?,
        val start: Int,
        val end: Int
    )
    private val spanList = mutableListOf<Data>()

    private val linkInfos: List<LinkInfo>
        get() = spanList.filter { it.what is URLSpan }.map {
            LinkInfo(
                (it.what as URLSpan).url,
                it.start,
                it.end
            )
        }

    override fun removeSpan(what: Any?) {
        super.removeSpan(what)
        spanList.removeAll { it.what == what }
    }

    override fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
        super.setSpan(what, start, end, flags)
        spanList.add(Data(what, start, end))
    }
}
