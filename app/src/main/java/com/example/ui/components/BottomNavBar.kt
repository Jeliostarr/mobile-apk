package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
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

    NavigationBar(
        modifier = Modifier
            .background(YoSurface)
            .shadow(4.dp, clip = false)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = YoSurface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) YoPrimaryAmber.copy(alpha = 0.15f) else YoSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = if (isSelected) YoPrimaryAmber else YoTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) YoPrimaryAmber else YoTextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = YoSurface,
                    selectedIconColor = YoPrimaryAmber,
                    unselectedIconColor = YoTextMuted,
                    selectedTextColor = YoPrimaryAmber,
                    unselectedTextColor = YoTextMuted
                )
            )
        }
    }
}
