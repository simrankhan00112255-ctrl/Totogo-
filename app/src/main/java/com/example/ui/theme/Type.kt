package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val NotoSansBengaliFamily = FontFamily(
  Font(R.font.noto_sans_bengali, FontWeight.Normal)
)

fun totoTypography(isBengali: Boolean): Typography {
  val family = if (isBengali) NotoSansBengaliFamily else PlusJakartaSansFamily
  return Typography(
    displaySmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Bold,
      fontSize = 30.sp,
      lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp,
      lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 14.sp,
    ),
  )
}
