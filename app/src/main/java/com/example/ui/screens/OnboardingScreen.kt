package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.R

data class OnboardingPageData(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val badgeTextRes: Int,
    val isPermissionPage: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    hasImagesPermission: Boolean,
    hasVideosPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPageData(
            titleRes = R.string.onboarding_title_1,
            subtitleRes = R.string.onboarding_sub_1,
            icon = Icons.Default.CleaningServices,
            accentColor = CyanPrimary,
            badgeTextRes = R.string.onboarding_badge_1
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_title_2,
            subtitleRes = R.string.onboarding_sub_2,
            icon = Icons.Default.Security,
            accentColor = EmeraldKeep,
            badgeTextRes = R.string.onboarding_badge_2
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_title_3,
            subtitleRes = R.string.onboarding_sub_3,
            icon = Icons.Default.Swipe,
            accentColor = PurpleAccent,
            badgeTextRes = R.string.onboarding_badge_3
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_title_4,
            subtitleRes = R.string.onboarding_sub_4,
            icon = Icons.Default.CleaningServices,
            accentColor = CyanPrimary,
            badgeTextRes = R.string.onboarding_badge_4,
            isPermissionPage = true
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header with Skip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SnapSweep",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(
                            onClick = onFinishOnboarding,
                            modifier = Modifier.testTag("skip_onboarding_button")
                        ) {
                            Text(
                                text = stringResource(R.string.skip),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp)
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    OnboardingPageCard(
                        page = page,
                        isWideScreen = isWideScreen,
                        hasImagesPermission = hasImagesPermission,
                        hasVideosPermission = hasVideosPermission,
                        onRequestPermissions = onRequestPermissions
                    )
                }

                // Bottom Controls: Page Indicator + Navigation Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) pages[pagerState.currentPage].accentColor
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("onboarding_back_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.back),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == pages.size - 1
                        val currentPageColor = pages[pagerState.currentPage].accentColor

                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onFinishOnboarding()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(if (pagerState.currentPage > 0) 1f else 2f)
                                .height(52.dp)
                                .testTag("onboarding_next_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentPageColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (isLastPage) stringResource(R.string.get_started) else stringResource(R.string.next),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageCard(
    page: OnboardingPageData,
    isWideScreen: Boolean,
    hasImagesPermission: Boolean,
    hasVideosPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        page.accentColor.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(28.dp)
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Badge tag
            Surface(
                shape = CircleShape,
                color = page.accentColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, page.accentColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = stringResource(page.badgeTextRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = page.accentColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Hero Icon Graphic
            Box(
                modifier = Modifier
                    .size(if (isWideScreen) 110.dp else 130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                page.accentColor.copy(alpha = 0.25f),
                                page.accentColor.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, page.accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.accentColor,
                    modifier = Modifier.size(if (isWideScreen) 54.dp else 64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isWideScreen) 22.sp else 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle
            Text(
                text = stringResource(page.subtitleRes),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (page.isPermissionPage) {
                Spacer(modifier = Modifier.height(24.dp))
                val isApproved = hasImagesPermission && hasVideosPermission
                if (isApproved) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = EmeraldKeep.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldKeep),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Approved",
                                tint = EmeraldKeep,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.access_approved),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldKeep
                                )
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = page.accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp)
                            .testTag("onboarding_grant_permission_button")
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.grant_storage_access),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
