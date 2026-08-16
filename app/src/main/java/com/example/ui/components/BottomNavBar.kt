package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoAccentCyan
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted

sealed class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Sports : BottomTab("sports", "Sports", Icons.Filled.SportsSoccer, Icons.Outlined.SportsSoccer)
    object Explore : BottomTab("explore", "Explore", Icons.Filled.Explore, Icons.Outlined.Explore)
    object Downloads : BottomTab("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download)
    object Account : BottomTab("account", "Account", Icons.Filled.Person, Icons.Outlined.Person)
}

/**
 * Floating glass nav bar — sits with margin on all sides rather than
 * spanning edge-to-edge, semi-transparent surface + hairline border to
 * read as "glass" without leaning on real blur (RenderEffect needs API 31,
 * minSdk here is 24). Each tab gets its own animated pill behind the icon
 * rather than a shared sliding indicator, which keeps this simple and
 * still reads as deliberate rather than a stock NavigationBar.
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onTabSelected: (BottomTab) -> Unit
) {
    val tabs = listOf(
        BottomTab.Home,
        BottomTab.Sports,
        BottomTab.Explore,
        BottomTab.Downloads,
        BottomTab.Account
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(YoSurface.copy(alpha = 0.92f))
                .border(1.dp, YoBorder, RoundedCornerShape(26.dp)),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route
                NavTabItem(
                    tab = tab,
                    isSelected = isSelected,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    tab: BottomTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillColor by animateColorAsState(
        targetValue = if (isSelected) YoPrimaryViolet.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(220),
        label = "pill"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) YoPrimaryViolet else YoTextMuted,
        animationSpec = tween(220),
        label = "iconColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                tint = iconColor,
                modifier = Modifier.size(21.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 1.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(YoAccentCyan)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = iconColor
        )
    }
}
