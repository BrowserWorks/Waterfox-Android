/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package net.waterfox.android.onboarding.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import net.waterfox.android.R
import net.waterfox.android.compose.button.PrimaryButton
import net.waterfox.android.compose.button.SecondaryButton
import net.waterfox.android.theme.WaterfoxTheme

/**
 * Enum that represents the onboarding screen that is displayed.
 */
private enum class OnboardingState {
    Welcome,
    SyncSignIn,
}

/**
 * A screen for displaying a welcome and sync sign in onboarding.
 *
 * @param isSyncSignIn Whether or not the user is signed into their Firefox Sync account.
 * @param onDismiss Invoked when the user clicks on the close or "Skip" button.
 * @param onSignInButtonClick Invoked when the user clicks on the "Sign In" button
 */
@Composable
fun Onboarding(
    isSyncSignIn: Boolean,
    onDismiss: () -> Unit,
    onSignInButtonClick: () -> Unit,
) {
    var onboardingState by remember { mutableStateOf(OnboardingState.Welcome) }

    Column(
        modifier = Modifier
            .background(WaterfoxTheme.colors.layer1)
            .fillMaxSize()
            .padding(bottom = 32.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = { onDismiss() },
            ) {
                Icon(
                    painter = painterResource(id = mozilla.components.ui.icons.R.drawable.mozac_ic_cross_20),
                    contentDescription = stringResource(R.string.onboarding_home_content_description_close_button),
                    tint = WaterfoxTheme.colors.iconPrimary,
                )
            }
        }

        if (onboardingState == OnboardingState.Welcome) {
            OnboardingWelcomeContent()

            OnboardingWelcomeBottomContent(
                onboardingState = onboardingState,
                isSyncSignIn = isSyncSignIn,
                onGetStartedButtonClick = {
                    if (isSyncSignIn) {
                        onDismiss()
                    } else {
                        onboardingState = OnboardingState.SyncSignIn
                    }
                },
            )
        } else if (onboardingState == OnboardingState.SyncSignIn) {
            OnboardingSyncSignInContent()

            OnboardingSyncSignInBottomContent(
                onboardingState = onboardingState,
                onSignInButtonClick = { onSignInButtonClick() },
                onSkipButtonClick = { onDismiss() },
            )
        }
    }
}

@Composable
private fun OnboardingWelcomeBottomContent(
    onboardingState: OnboardingState,
    isSyncSignIn: Boolean,
    onGetStartedButtonClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        PrimaryButton(
            text = stringResource(id = R.string.onboarding_home_get_started_button),
            onClick = onGetStartedButtonClick,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isSyncSignIn) {
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Indicators(onboardingState = onboardingState)
        }
    }
}

@Composable
private fun OnboardingWelcomeContent() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_waterfox),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = R.string.onboarding_home_welcome_title_2),
            color = WaterfoxTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            style = WaterfoxTheme.typography.headline5,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.onboarding_home_welcome_description),
            color = WaterfoxTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            style = WaterfoxTheme.typography.body2,
        )
    }
}

@Composable
private fun OnboardingSyncSignInContent() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_onboarding_sync),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = R.string.onboarding_home_sync_title_3),
            color = WaterfoxTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            style = WaterfoxTheme.typography.headline5,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.onboarding_home_sync_description),
            color = WaterfoxTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            style = WaterfoxTheme.typography.body2,
        )
    }
}

@Composable
private fun OnboardingSyncSignInBottomContent(
    onboardingState: OnboardingState,
    onSignInButtonClick: () -> Unit,
    onSkipButtonClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        PrimaryButton(
            text = stringResource(id = R.string.onboarding_home_sign_in_button),
            onClick = onSignInButtonClick,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SecondaryButton(
            text = stringResource(id = R.string.onboarding_home_skip_button),
            onClick = onSkipButtonClick,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Indicators(onboardingState = onboardingState)
    }
}

@Composable
private fun Indicators(onboardingState: OnboardingState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Indicator(
            color = if (onboardingState == OnboardingState.Welcome) {
                WaterfoxTheme.colors.indicatorActive
            } else {
                WaterfoxTheme.colors.indicatorInactive
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        Indicator(
            color = if (onboardingState == OnboardingState.SyncSignIn) {
                WaterfoxTheme.colors.indicatorActive
            } else {
                WaterfoxTheme.colors.indicatorInactive
            },
        )
    }
}

@Composable
private fun Indicator(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
@Preview
private fun OnboardingPreview() {
    WaterfoxTheme {
        Onboarding(
            isSyncSignIn = false,
            onDismiss = {},
            onSignInButtonClick = {},
        )
    }
}
