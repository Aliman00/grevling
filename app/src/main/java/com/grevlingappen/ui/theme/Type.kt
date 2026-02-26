package com.grevlingappen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography - Definerer tekststiler for hele appen.
 * 
 * Material Design 3 har et typografi-system med forhåndsdefinerte størrelser:
 * - displayLarge/Medium/Small: Store overskrifter (hero-tekst)
 * - headlineLarge/Medium/Small: Seksjon-overskrifter
 * - titleLarge/Medium/Small: Kort innhold som knapper, titler
 * - bodyLarge/Medium/Small: Brødtekst (standard lesing)
 * - labelLarge/Medium/Small: Små tekster som statuser, tags
 */

val Typography = Typography(
    // DISPLAY - Største tekst (sjelden brukt, kun for hero-seksjoner)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),

    // HEADLINE - Store overskrifter (brukes til skjerm-titler)
    // headlineLarge brukes til "GrevlingAppen" tittel
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),

    // TITLE - Medium overskrifter (brukes til seksjons-titler)
    // titleLarge brukes til "Account Settings", "Permissions" etc.
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // titleMedium brukes til kortlenker, mindre viktige overskrifter
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // BODY - Standard brødtekst (det meste av teksten i appen)
    // bodyLarge brukes til viktig informasjon, beskrivelser
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // bodyMedium brukes til standard tekst i cards, lister
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // LABEL - Små tekster (knapper, status-indikatorer, tags)
    // labelLarge brukes til knapper ("AKTIVER VIDERESENDING")
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // labelMedium brukes til små statuser ("Lagret", "Sendt")
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),

    // labelSmall brukes til timestamps, hjelpe-tekst
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)