import builtins

with open('app/src/main/java/bea/l8tenever/com/ui/screens/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

def get_block(start_marker, end_marker=None):
    start = content.find(start_marker)
    if end_marker:
        end = content.find(end_marker, start)
    else:
        end = -1
    return content[start:end] if end != -1 else content[start:]

block_alarm = get_block("// --- ALARM SEKTION ---", "// --- LIVE INFO SEKTION ---")
block_live = get_block("// --- LIVE INFO SEKTION ---", "// --- NEXT ALARM INFO ---")
block_next = get_block("// --- NEXT ALARM INFO ---", "// --- ZUSATZWECKER ---")
block_zusatz = get_block("// --- ZUSATZWECKER ---", "// --- KONTO SEKTION ---")
block_account = get_block("// --- KONTO SEKTION ---", "// --- WECKER-TEMPLATES ---")
block_templates = get_block("// --- WECKER-TEMPLATES ---", "// --- HINTERGRUND-BERECHTIGUNGEN ---")
block_bg = get_block("// --- HINTERGRUND-BERECHTIGUNGEN ---", "// --- APP INFO ---")
block_app = get_block("// --- APP INFO ---", "Spacer(modifier = Modifier.height(24.dp))")

start_idx = content.find("// --- ALARM SEKTION ---")
end_idx = content.find("Spacer(modifier = Modifier.height(24.dp))")

prefix = content[:start_idx]
suffix = content[end_idx:]

new_order = """
            SettingsCategoryTitle("WECK-EINSTELLUNGEN")
""" + block_alarm + block_next + block_templates + block_zusatz + """
            SettingsCategoryTitle("BENACHRICHTIGUNGEN")
""" + block_live + """
            SettingsCategoryTitle("SYSTEM & BERECHTIGUNGEN")
""" + block_bg + """
            SettingsCategoryTitle("KONTO & INFO")
""" + block_account + block_app

new_content = prefix + new_order.strip() + "\n            " + suffix

# also insert the SettingsCategoryTitle composable function at the end
if "fun SettingsCategoryTitle" not in new_content:
    new_content += """
@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF533F85),
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp, start = 8.dp)
    )
}
"""

with open('app/src/main/java/bea/l8tenever/com/ui/screens/SettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)
