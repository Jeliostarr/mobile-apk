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

/** For Brush.linearGradient(colors = YoGlowGradient, ...) — the signature 135° violet→cyan glow. */
val YoGlowGradient = listOf(YoPrimaryViolet, YoAccentCyan)

// ─────────────────────────────────────────────────────────────
// Back-compat aliases — DO NOT use these in new code.
// "Amber" no longer describes the palette; these exist only so the
// dozen+ screens still referencing the old names keep compiling
// while each one gets migrated to the real names above. Delete a
// line here once its last call site is updated.
// ─────────────────────────────────────────────────────────────

@Deprecated("Renamed in the Neon Night redesign.", ReplaceWith("YoPrimaryViolet"))
val YoPrimaryAmber = YoPrimaryViolet

@Deprecated("Renamed in the Neon Night redesign.", ReplaceWith("YoPrimaryVioletPress"))
val YoPrimaryAmberPress = YoPrimaryVioletPress

@Deprecated("Renamed in the Neon Night redesign.", ReplaceWith("YoSuccess"))
val YoSuccessGreen = YoSuccess
