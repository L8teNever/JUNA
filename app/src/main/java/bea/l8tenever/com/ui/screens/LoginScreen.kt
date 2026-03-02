package bea.l8tenever.com.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bea.l8tenever.com.ui.theme.*
import bea.l8tenever.com.viewmodel.MainViewModel

// ─── Validierungs-Hilfsfunktionen ─────────────────────────────────────────────

private fun validateServerUrl(url: String): String? = when {
    url.isBlank()                              -> "Server-URL darf nicht leer sein"
    !url.startsWith("https://") &&
    !url.startsWith("http://")                 -> "URL muss mit https:// beginnen"
    !url.contains(".")                         -> "Ungültige URL (kein Punkt gefunden)"
    else                                       -> null
}

private fun validateSchool(school: String): String? = when {
    school.isBlank()                           -> "Schulkürzel darf nicht leer sein"
    school.contains(" ")                       -> "Schulkürzel darf keine Leerzeichen enthalten"
    else                                       -> null
}

private fun validateUsername(username: String): String? = when {
    username.isBlank()                         -> "Benutzername darf nicht leer sein"
    else                                       -> null
}

private fun validatePassword(password: String): String? = when {
    password.isBlank()                         -> "Passwort darf nicht leer sein"
    password.length < 3                        -> "Passwort ist zu kurz"
    else                                       -> null
}

private fun validateAll(s: String, sc: String, u: String, p: String): String? =
    validateServerUrl(s) ?: validateSchool(sc) ?: validateUsername(u) ?: validatePassword(p)

// ─── LoginScreen ──────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val state        by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    var serverUrl by remember { mutableStateOf(state.serverUrl.ifBlank { "https://hepta.webuntis.com" }) }
    var school    by remember { mutableStateOf(state.school) }
    var username  by remember { mutableStateOf(state.username) }
    var password  by remember { mutableStateOf(state.password) }
    var showPass  by remember { mutableStateOf(false) }

    // Zeigt Fehler erst nach dem ersten Login-Versuch
    var submitAttempted by remember { mutableStateOf(false) }

    val errServer   = if (submitAttempted) validateServerUrl(serverUrl) else null
    val errSchool   = if (submitAttempted) validateSchool(school)       else null
    val errUsername = if (submitAttempted) validateUsername(username)   else null
    val errPassword = if (submitAttempted) validatePassword(password)   else null

    // --- ANIMATIONS-LOGIK ---
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    // Hintergrund-Animation (langsame schwebende Blobs)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val blobOffset1 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 40f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "blob1"
    )
    val blobOffset2 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = -40f,
        animationSpec = infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "blob2"
    )

    // Logo-Pulsanimation
    val logoScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "logoScale"
    )

    // Login-Helper
    fun doLogin() {
        submitAttempted = true
        viewModel.clearError()
        if (validateAll(serverUrl, school, username, password) == null) {
            focusManager.clearFocus()
            viewModel.login(serverUrl.trim(), school.trim(), username.trim(), password)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- ANIMIERTER HINTERGRUND ---
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 100.dp + blobOffset1.dp, y = (-150).dp + blobOffset2.dp)
                .background(
                    Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp + blobOffset2.dp, y = 100.dp + blobOffset1.dp)
                .background(
                    Brush.radialGradient(listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo & Titel (Staggered Entry 1)
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer(scaleX = logoScale, scaleY = logoScale)
                            .size(110.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = bea.l8tenever.com.R.drawable.ic_launcher_foreground),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize().padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "YUNA", 
                        fontSize = 48.sp, 
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        letterSpacing = 12.sp
                    )
                    Text(
                        "Your Untis Notification Alarm".uppercase(), 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onBackground, 
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Glass-Form (Staggered Entry 2)
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 40 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // --- Server-URL ---
                        BeaTextField(
                            value         = serverUrl,
                            onValueChange = { serverUrl = it; if (submitAttempted) viewModel.clearError() },
                            label         = "Server-URL",
                            placeholder   = "https://hepta.webuntis.com",
                            icon          = Icons.Outlined.Public,
                            keyboardType  = KeyboardType.Uri,
                            imeAction     = ImeAction.Next,
                            onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                            isError       = errServer != null,
                            errorText     = errServer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Schulkürzel ---
                        BeaTextField(
                            value         = school,
                            onValueChange = { school = it; if (submitAttempted) viewModel.clearError() },
                            label         = "Schulkürzel",
                            placeholder   = "schul-kürzel",
                            icon          = Icons.Outlined.School,
                            imeAction     = ImeAction.Next,
                            onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                            isError       = errSchool != null,
                            errorText     = errSchool
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Benutzername ---
                        BeaTextField(
                            value         = username,
                            onValueChange = { username = it; if (submitAttempted) viewModel.clearError() },
                            label         = "Benutzername",
                            placeholder   = "name",
                            icon          = Icons.Outlined.Person,
                            keyboardType  = KeyboardType.Email,
                            imeAction     = ImeAction.Next,
                            onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                            isError       = errUsername != null,
                            errorText     = errUsername
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Passwort ---
                        Column {
                            OutlinedTextField(
                                value         = password,
                                onValueChange = { password = it; if (submitAttempted) viewModel.clearError() },
                                modifier      = Modifier.fillMaxWidth(),
                                label         = { Text("Passwort") },
                                leadingIcon   = { 
                                    Icon(Icons.Outlined.Lock, null, tint = if (errPassword != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) 
                                },
                                trailingIcon  = {
                                    IconButton(onClick = { showPass = !showPass }) {
                                        Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                                    }
                                },
                                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { doLogin() }),
                                isError = errPassword != null,
                                shape   = RoundedCornerShape(28.dp),
                                colors  = beaTextFieldColors()
                            )
                            if (errPassword != null) {
                                Text(errPassword, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // API-Fehlermeldung
                        AnimatedVisibility(visible = state.error != null) {
                            state.error?.let { err ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                    shape    = RoundedCornerShape(16.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(friendlyError(err), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        // Login-Button
                        Button(
                            onClick  = { doLogin() },
                            enabled  = !state.isLoading,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape    = CircleShape,
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.LockOpen, null)
                                Spacer(Modifier.width(12.dp))
                                Text("Premium Login", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer (Staggered Entry 3)
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(800, 400))
            ) {
                Text(
                    "Sichere Verbindung via WebUntis API",
                    fontSize  = 12.sp,
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── Fehlermeldungen übersetzen ───────────────────────────────────────────────

private fun friendlyError(err: String): String = when {
    err.contains("-32601") || err.contains("Method not found")
        -> "Anmeldung fehlgeschlagen – Schulkürzel und Server-URL prüfen"
    err.contains("-8503") || err.contains("bad credentials", ignoreCase = true)
        -> "Falscher Benutzername oder falsches Passwort"
    err.contains("-8512") || err.contains("not authenticated", ignoreCase = true)
        -> "Sitzung abgelaufen – bitte erneut anmelden"
    err.contains("Verbindungsfehler") || err.contains("UnknownHostException")
        -> "Keine Verbindung – Server nicht erreichbar"
    err.contains("HTTP 4")
        -> "Server antwortet nicht – URL prüfen"
    else -> err
}

// ─── Wiederverwendbare Komponenten ────────────────────────────────────────────

@Composable
fun BeaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String   = "",
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction       = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isError: Boolean           = false,
    errorText: String?         = null
) {
    Column {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text(label) },
            placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
            leadingIcon   = { Icon(icon, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() }
            ),
            singleLine = true,
            isError    = isError,
            shape      = RoundedCornerShape(28.dp),
            colors     = beaTextFieldColors()
        )
        if (isError && errorText != null) {
            Text(
                errorText,
                color    = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun beaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    errorBorderColor        = MaterialTheme.colorScheme.error,
    focusedLabelColor       = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor     = MaterialTheme.colorScheme.onBackground,
    errorLabelColor         = MaterialTheme.colorScheme.error,
    cursorColor             = MaterialTheme.colorScheme.primary,
    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
    errorTextColor          = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor   = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor     = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
)
