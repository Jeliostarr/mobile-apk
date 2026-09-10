package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DASHBOARD_URL
import com.example.data.model.KeyInfo
import com.example.data.model.WATCH_WEB_URL
import com.example.data.model.formatUgx
import com.example.repository.AccountBundle
import com.example.repository.YocinemaRepository
import com.example.ui.components.RequestDialog
import com.example.ui.theme.YoAccentCyan
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoDestructive
import com.example.ui.theme.YoGlowGradient
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoSuccess
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.theme.YoWarningAmber
import com.example.ui.util.BiometricCopyHelper
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    repository: YocinemaRepository,
    onLoginClick: () -> Unit,
    onLibraryClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val scope = rememberCoroutineScope()

    val isLoggedIn by repository.isLoggedInFlow.collectAsState(initial = repository.isLoggedIn())

    // Paint instantly from whatever's cached (possibly nothing on a first-ever
    // open), then silently refresh. This is what stops the screen showing a
    // full-screen spinner every single time the user taps this tab.
    var bundle by remember { mutableStateOf(repository.getCachedAccountBundle() ?: AccountBundle()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var switchingKeyId by remember { mutableStateOf<String?>(null) }

    // Plain `repository.tokenManager.getApiKey()` used to be read once per
    // recomposition with no reactive tie to anything — in practice that
    // meant "switched key" only visibly took effect after leaving and
    // re-entering this screen forced a fresh read. Two fixes together:
    // collecting apiKeyFlow makes this properly reactive to the persisted
    // value, and optimisticActiveKey makes the switch feel instant instead
    // of waiting on that flow (or the follow-up network refresh) to catch up.
    val persistedActiveKey by repository.tokenManager.apiKeyFlow.collectAsState(
        initial = repository.tokenManager.getApiKey()
    )
    var optimisticActiveKey by remember { mutableStateOf<String?>(null) }
    val activeKey = optimisticActiveKey ?: persistedActiveKey

    suspend fun load(force: Boolean) {
        if (!repository.isLoggedIn()) return
        isRefreshing = true
        bundle = repository.getAccountBundle(forceRefresh = force)
        isRefreshing = false
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            // Instant paint already happened above from cache; this call returns
            // the cached copy immediately if it's still fresh, or fetches once.
            load(force = false)
        } else {
            bundle = AccountBundle()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextPrimary
                    )
                    if (isLoggedIn) {
                        IconButton(onClick = { scope.launch { load(force = true) } }) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = YoPrimaryViolet
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = YoTextMuted)
                            }
                        }
                    }
                }
            }

            item {
                ProfileCard(
                    isLoggedIn = isLoggedIn,
                    name = bundle.user?.name,
                    email = bundle.user?.email,
                    onLoginClick = onLoginClick,
                    onLogoutClick = { repository.logout() }
                )
            }

            if (isLoggedIn) {
                item {
                    BalanceCard(
                        balance = bundle.user?.balance,
                        onTopUpClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$DASHBOARD_URL/wallet")))
                        }
                    )
                }

                item {
                    WatchOnWebCard(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WATCH_WEB_URL)))
                        }
                    )
                }

                item {
                    LibraryQuickLinkCard(onClick = onLibraryClick)
                }

                item {
                    Text(
                        text = "Your API Keys",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                if (bundle.keys.isEmpty() && !isRefreshing) {
                    item {
                        Text(
                            text = "No API keys yet — create one from the dashboard.",
                            fontSize = 12.sp,
                            color = YoTextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(bundle.keys, key = { it.id ?: it.key ?: it.hashCode().toString() }) { keyInfo ->
                        KeyCard(
                            keyInfo = keyInfo,
                            isActive = !activeKey.isNullOrBlank() && keyInfo.key == activeKey,
                            isSwitching = switchingKeyId == (keyInfo.id ?: keyInfo.key),
                            onCopy = {
                                val k = keyInfo.key ?: return@KeyCard
                                if (activity != null) {
                                    BiometricCopyHelper.copyWithAuthentication(
                                        activity = activity,
                                        label = "YOCINEMA API Key",
                                        textToCopy = k
                                    )
                                }
                            },
                            onUseThisKey = {
                                val k = keyInfo.key ?: return@KeyCard
                                switchingKeyId = keyInfo.id ?: keyInfo.key
                                optimisticActiveKey = k
                                scope.launch {
                                    repository.switchActiveKey(k)
                                    load(force = true)
                                    switchingKeyId = null
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(6.dp)) }
            }

            item {
                Button(
                    onClick = { showRequestDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YoPrimaryViolet,
                        contentColor = YoBaseBackground
                    )
                ) {
                    Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request a Movie or Series", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = YoTextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "YOCINEMA v${com.example.BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = YoTextMuted.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showRequestDialog) {
        RequestDialog(onDismiss = { showRequestDialog = false })
    }
}

@Composable
private fun ProfileCard(
    isLoggedIn: Boolean,
    name: String?,
    email: String?,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(YoGlowGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    // Was a generic Person icon — swapped for the app's own
                    // logo (same drawable LoginScreen uses) since there's no
                    // per-user avatar photo from the backend to show instead.
                    Image(
                        painter = painterResource(id = R.drawable.yocinema_logo_1786014644709),
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) (name ?: "YOCINEMA Subscriber") else "Guest Mode",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isLoggedIn) (email ?: "API Key Active") else "Enter your key for unlimited access",
                        fontSize = 12.sp,
                        color = YoTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoggedIn) {
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YoDestructive.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YoDestructive)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Out", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YoPrimaryViolet,
                        contentColor = YoBaseBackground
                    )
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enter API Key", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun BalanceCard(
    balance: Double?,
    onTopUpClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(YoGlowGradient))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wallet Balance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = YoBaseBackground.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatUgx(balance),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = YoBaseBackground
                )
            }
            Button(
                onClick = onTopUpClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YoBaseBackground,
                    contentColor = YoPrimaryViolet
                )
            ) {
                Text("Top Up", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun WatchOnWebCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(YoAccentCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = YoAccentCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Watch on the Web", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
            Text("watch.yocinema.dpdns.org", fontSize = 11.sp, color = YoTextMuted)
        }
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = YoTextMuted, modifier = Modifier.size(16.dp))
    }

    Spacer(modifier = Modifier.height(6.dp))
}

/**
 * Library moved off the bottom nav (see BottomNavBar.kt — that slot now
 * goes to Sports) and onto a shortcut from Home's search bar, but Account
 * is the other place someone naturally looks for "my stuff", so it gets a
 * quick link here too rather than losing a second point of entry.
 */
@Composable
private fun LibraryQuickLinkCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(YoPrimaryViolet.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = YoPrimaryViolet, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("My Library", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
            Text("Watchlist and watch history", fontSize = 11.sp, color = YoTextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = YoTextMuted, modifier = Modifier.size(13.dp))
    }

    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun KeyCard(
    keyInfo: KeyInfo,
    isActive: Boolean,
    isSwitching: Boolean,
    onCopy: () -> Unit,
    onUseThisKey: () -> Unit
) {
    val statusColor = when {
        keyInfo.isExpired -> YoDestructive
        keyInfo.isUsedUp -> YoWarningAmber
        keyInfo.isUsable -> YoSuccess
        else -> YoTextMuted
    }
    val statusLabel = when {
        keyInfo.isExpired -> "Expired"
        keyInfo.isUsedUp -> "Daily limit used up"
        keyInfo.isUsable -> "Active"
        else -> (keyInfo.status ?: "Unknown").replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(YoSurface)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) YoPrimaryViolet else YoBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = keyInfo.name?.takeIf { it.isNotBlank() } ?: "API Key",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "In use", tint = YoPrimaryViolet, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = keyInfo.maskedKey,
                        fontSize = 12.sp,
                        color = YoTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }

                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy (requires biometrics)",
                        tint = YoTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val limit = keyInfo.dailyLimit ?: 0
            if (limit > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usage today",
                        fontSize = 11.sp,
                        color = YoTextMuted
                    )
                    Text(
                        text = "${keyInfo.usageToday ?: 0} / $limit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YoTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { keyInfo.usageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = YoSurfaceVariant
                )
            }

            if (!keyInfo.expiresAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Expires ${keyInfo.expiresAt.take(10)}",
                    fontSize = 11.sp,
                    color = YoTextMuted
                )
            }

            if (!isActive && keyInfo.isUsable) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onUseThisKey,
                    enabled = !isSwitching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YoPrimaryViolet.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YoPrimaryViolet)
                ) {
                    if (isSwitching) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = YoPrimaryViolet)
                    } else {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use this key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
