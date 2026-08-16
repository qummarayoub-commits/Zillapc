package com.darkjade.streamlib.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VaultTypography = Typography(
    // Bolder, punchier headlines — closer to the confident poster-title
    // styling in the reference, without copying its exact branding/colors.
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = VaultColors.TextPrimary, letterSpacing = 0.2.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = VaultColors.TextPrimary),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = VaultColors.TextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, color = VaultColors.TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = VaultColors.TextPrimary),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = VaultColors.TextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = VaultColors.TextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = VaultColors.TextSecondary),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = VaultColors.TextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VaultColors.TextPrimary),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = VaultColors.TextSecondary),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = VaultColors.TextTertiary),
)
