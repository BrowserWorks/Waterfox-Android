/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.settings.logins

import android.text.InputType
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import net.waterfox.android.R

fun togglePasswordReveal(passwordText: TextView, revealPasswordButton: ImageButton) {
    val context = passwordText.context

    if (passwordText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD
        or InputType.TYPE_CLASS_TEXT
    ) {
        passwordText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        revealPasswordButton.setImageDrawable(
            AppCompatResources.getDrawable(context, mozilla.components.ui.icons.R.drawable.mozac_ic_eye_slash_24)
        )
        revealPasswordButton.contentDescription =
            context.getString(R.string.saved_login_hide_password)
    } else {
        passwordText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        revealPasswordButton.setImageDrawable(
            AppCompatResources.getDrawable(context, mozilla.components.ui.icons.R.drawable.mozac_ic_eye_24)
        )
        revealPasswordButton.contentDescription =
            context.getString(R.string.saved_login_reveal_password)
    }
    // We need to reset to take effect
    passwordText.text = passwordText.text
}
