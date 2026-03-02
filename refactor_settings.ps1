$content = Get-Content -Path "app/src/main/java/bea/l8tenever/com/ui/screens/SettingsScreen.kt" -Raw -Encoding UTF8

function Get-Block ($startMarker, $endMarker) {
    if (-not $startMarker) { return "" }
    $startIndex = $content.IndexOf($startMarker)
    if ($startIndex -eq -1) { return "" }
    
    if (-not $endMarker) {
        return $content.Substring($startIndex)
    }
    
    $endIndex = $content.IndexOf($endMarker, $startIndex)
    if ($endIndex -eq -1) { return $content.Substring($startIndex) }
    return $content.Substring($startIndex, $endIndex - $startIndex)
}

$blockAlarm = Get-Block "// --- ALARM SEKTION ---" "            SettingsCategoryTitle(`"BENACHRICHTIGUNGEN`")"
$blockLive = Get-Block "            SettingsCategoryTitle(`"BENACHRICHTIGUNGEN`")" "            SettingsCategoryTitle(`"SYSTEM & BERECHTIGUNGEN`")"
$blockBg = Get-Block "            SettingsCategoryTitle(`"SYSTEM & BERECHTIGUNGEN`")" "            SettingsCategoryTitle(`"KONTO & INFO`")"
$blockAccount = Get-Block "            SettingsCategoryTitle(`"KONTO & INFO`")" "            Spacer(modifier = Modifier.height(24.dp))"

$stateInsertionIndex = $content.IndexOf("var alarmEnabled by remember")
$prefix1 = $content.Substring(0, $stateInsertionIndex)
$prefix2 = "var currentCategory by remember { mutableStateOf(SettingsCategory.MAIN) }`n    " + $content.Substring($stateInsertionIndex, $content.IndexOf("            Row(", $stateInsertionIndex) - $stateInsertionIndex)

$topBarStartIndex = $content.IndexOf("            Row(", $stateInsertionIndex)
$topBarEndIndex = $content.IndexOf("            Column(", $topBarStartIndex)

$newTopBar = @"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Close Button
                IconButton(
                    onClick = {
                        if (currentCategory == SettingsCategory.MAIN) {
                            onNavigateBack()
                        } else {
                            currentCategory = SettingsCategory.MAIN
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1A20))
                ) {
                    Icon(
                        imageVector = if (currentCategory == SettingsCategory.MAIN) Icons.Default.Close else Icons.Default.ArrowBack,
                        contentDescription = "Zurück",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = currentCategory.title.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                // Invisible spacer for centering
                Spacer(modifier = Modifier.size(42.dp))
            }

"@

$middleStart = $content.IndexOf("            Column(", $topBarStartIndex)
$middleEnd = $content.IndexOf("            SettingsCategoryTitle(`"WECK-EINSTELLUNGEN`")", $middleStart)
$middleCode = $content.Substring($middleStart, $middleEnd - $middleStart)

$newSwitch = @"
                when (currentCategory) {
                    SettingsCategory.MAIN -> {
                        SettingsMenuCard(
                            title = "Weck-Einstellungen",
                            icon = Icons.Outlined.Alarm,
                            onClick = { currentCategory = SettingsCategory.ALARM }
                        )
                        SettingsMenuCard(
                            title = "Benachrichtigungen",
                            icon = Icons.Outlined.NotificationsActive,
                            onClick = { currentCategory = SettingsCategory.NOTIFICATIONS }
                        )
                        SettingsMenuCard(
                            title = "System & Berechtigungen",
                            icon = Icons.Outlined.Security,
                            onClick = { currentCategory = SettingsCategory.SYSTEM }
                        )
                        SettingsMenuCard(
                            title = "Konto & Info",
                            icon = Icons.Outlined.Person,
                            onClick = { currentCategory = SettingsCategory.ACCOUNT }
                        )
                    }
                    SettingsCategory.ALARM -> {
$blockAlarm                    }
                    SettingsCategory.NOTIFICATIONS -> {
$blockLive                    }
                    SettingsCategory.SYSTEM -> {
$blockBg                    }
                    SettingsCategory.ACCOUNT -> {
$blockAccount                    }
                }
"@

$suffixIndex = $content.IndexOf("            Spacer(modifier = Modifier.height(24.dp))")
$suffix = $content.Substring($suffixIndex)

$newContent = $prefix1 + $prefix2 + $newTopBar + $middleCode + $newSwitch + $suffix

if (-not $newContent.Contains("enum class SettingsCategory")) {
    $newContent += @"

enum class SettingsCategory(val title: String) {
    MAIN("SETUP"),
    ALARM("WECK-EINSTELLUNGEN"),
    NOTIFICATIONS("BENACHRICHTIGUNGEN"),
    SYSTEM("SYSTEM & BERECHTIGUNGEN"),
    ACCOUNT("KONTO & INFO")
}

@Composable
fun SettingsMenuCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF262329))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3B2577).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color(0xFFD3BAFF), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Öffnen",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
"@
}

Set-Content -Path "app/src/main/java/bea/l8tenever/com/ui/screens/SettingsScreen.kt" -Value $newContent -Encoding UTF8
