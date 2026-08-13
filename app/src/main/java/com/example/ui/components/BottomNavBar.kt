package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

sealed class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
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
        BottomTab.Explore,
        BottomTab.Downloads,
        BottomTab.Account
    )

    NavigationBar(
        modifier = Modifier
            .background(YoSurface)
            .drawBehind {
                // A same-color, zero-elevation bar sitting directly on
                // scrolling content has no visual edge to it — this hairline
                // is what actually separates "nav bar" from "page content"
                // when both use the same surface color.
                drawLine(
                    color = YoBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
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
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (isSelected) YoPrimaryAmber else YoTextMuted
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) YoPrimaryAmber else YoTextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = YoBorder,
                    selectedIconColor = YoPrimaryAmber,
                    unselectedIconColor = YoTextMuted,
                    selectedTextColor = YoPrimaryAmber,
                    unselectedTextColor = YoTextMuted
                )
            )
        }
    }
}
