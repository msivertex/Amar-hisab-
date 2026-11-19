package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppStrings
import com.example.ui.components.AmarHisabLogoBadge
import com.example.ui.components.CrystalCard
import com.example.ui.theme.HindSiliguriFamily
import com.example.ui.theme.LocalCrystalColors

@Composable
fun AboutSubPage(
    strings: AppStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val crystal = LocalCrystalColors.current
    val primary = MaterialTheme.colorScheme.primary
    val about = strings.aboutPage
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val minViewportHeight = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .heightIn(min = minViewportHeight)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar with back arrow and "সম্পর্কে" title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_top_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("about_back_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = crystal.glassSurfaceElevated,
                        border = BorderStroke(1.dp, crystal.glassBorder),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = about.pageTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFamily
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Centered Frosted-Glass Card
            CrystalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_main_card"),
                cornerRadius = 28.dp,
                elevated = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. App Logo Icon at top center
                    AmarHisabLogoBadge(
                        size = 86.dp,
                        modifier = Modifier.testTag("about_app_logo")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. App Title ("আমার হিসাব")
                    Text(
                        text = about.appTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 28.sp,
                            lineHeight = 34.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("about_app_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Tagline in clean blue accent color
                    Text(
                        text = about.tagline,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFamily,
                            fontSize = 15.sp
                        ),
                        color = primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("about_tagline")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Version Badge inside a soft light-blue pill badge
                    Surface(
                        shape = CircleShape,
                        color = primary.copy(alpha = if (crystal.isDark) 0.22f else 0.12f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = primary.copy(alpha = if (crystal.isDark) 0.42f else 0.28f)
                        ),
                        modifier = Modifier.testTag("about_version_badge")
                    ) {
                        Text(
                            text = about.versionBadge,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 13.sp
                            ),
                            color = primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 5. Centered High-Contrast App Description Paragraph
                    Text(
                        text = about.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = HindSiliguriFamily,
                            fontSize = 14.5.sp,
                            lineHeight = 23.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (crystal.isDark) 0.90f else 0.86f
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .testTag("about_description")
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(0.76f),
                        thickness = 1.dp,
                        color = crystal.glassBorder
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 6. Centered Creator / Developer Credit Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_developer_credits"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = about.developerLine,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 14.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = about.professionLine,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 13.5.sp
                            ),
                            color = crystal.mutedText,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = about.locationLine,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = HindSiliguriFamily,
                                fontSize = 13.5.sp
                            ),
                            color = crystal.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(24.dp))

            // 7. Subtle Centered Footer Note at the Bottom of the Screen
            Text(
                text = about.footerNote,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = HindSiliguriFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = crystal.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("about_footer_note")
            )
        }
    }
}
