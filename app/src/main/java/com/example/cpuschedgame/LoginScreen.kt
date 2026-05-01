package com.example.cpuschedgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cpuschedgame.ui.theme.*

@Composable
fun LoginScreen(
    authError: String,
    onLogin: (username: String, password: String) -> Unit,
    onGoToSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    val focusManager    = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GreenAccent.copy(alpha = 0.07f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Title
            Text(
                "[ OS SIMULATION v1.0 ]",
                color = TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "CPU", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                color = GreenBright, letterSpacing = 8.sp
            )
            Text(
                "SCHEDULER", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = GoldenBright, letterSpacing = 4.sp
            )

            Spacer(Modifier.height(32.dp))

            // Login card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "LOGIN",
                    color = GoldenBright, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                )

                // Username field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("USERNAME", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter username", color = TextSecondary.copy(alpha = 0.4f), fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GoldenBright,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = GoldenBright
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Password field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PASSWORD", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter password", color = TextSecondary.copy(alpha = 0.4f), fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (showPassword)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GoldenBright,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = GoldenBright
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onLogin(username, password)
                            }
                        ),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    if (showPassword) "HIDE" else "SHOW",
                                    color = TextSecondary, fontSize = 9.sp
                                )
                            }
                        }
                    )
                }

                // Error message
                if (authError.isNotBlank()) {
                    Text(
                        authError,
                        color = DangerRed, fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Login button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onLogin(username, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor   = TextPrimary
                    )
                ) {
                    Text(
                        "▶  LOGIN",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Go to signup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("No account? ", color = TextSecondary, fontSize = 12.sp)
                TextButton(onClick = onGoToSignup) {
                    Text(
                        "SIGN UP",
                        color = GoldenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}