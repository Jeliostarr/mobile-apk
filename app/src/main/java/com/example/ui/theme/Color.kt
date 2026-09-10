package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// Neon Night — near-black cinematic canvas, violet→cyan accents.
// Replaces the previous amber system. Token names below are the
// ones every screen should reference going forward.
// ─────────────────────────────────────────────────────────────

val YoBaseBackground = Color(0xFF08080F)
val YoSurface = Color(0xFF12121E)
val YoSurfaceVariant = Color(0xFF1B1B2C)
val YoBorder = Color(0xFF262640)

val YoPrimaryViolet = Color(0xFF7C5CFF)
val YoPrimaryVioletPress = Color(0xFF6647E6)
val YoAccentCyan = Color(0xFF22D3EE)

val YoTextPrimary = Color(0xFFF2F2F7)
val YoTextMuted = Color(0xFF9A9AB4)

val YoDestructive = Color(0xFFFF5A6E)
val YoSuccess = Color(0xFF34D399)

// Deliberately NOT part of the violet/cyan system — star ratings read as
// gold/amber everywhere as a convention; recoloring them on-brand would
// actually hurt recognizability rather than help the redesign. Reuses the
// old primary's exact value, so this also isn't a totally arbitrary choice.
val YoRatingGold = Color(0xFFE8B44A)

// Also deliberately outside the violet/cyan system, same reasoning as
// YoRatingGold — a "LIVE" indicator reads universally as red, and that's
// worth more than brand consistency here. Single source of truth for it;
// was previously copy-pasted as a raw hex in three different files.
val YoLiveRed = Color(0xFFE53935)

/** For Brush.linearGradient(colors = YoGlowGradient, ...) — the signature 135° violet→cyan glow. */
val YoGlowGradient = listOf(YoPrimaryViolet, YoAccentCyan)

