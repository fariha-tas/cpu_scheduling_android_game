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
fun SignupScreen(
    authError: String,
    onSignup: (username: String, password: String) -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var localError      by remember { mutableStateOf("") }
    val focusManager    = LocalFocusManager.current

    // Show either local validation error or server-side authError
    val displayError = if (localError.isNotBlank()) localError else authError

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
                        colors = listOf(GoldenBright.copy(alpha = 0.06f), Color.Transparent),
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

            // Signup card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "CREATE ACCOUNT",
                    color = GoldenBright, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp
                )

                // Username field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("USERNAME", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; localError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Min. 3 characters",
                                color = TextSecondary.copy(alpha = 0.4f), fontSize = 13.sp
                            )
                        },
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
                        onValueChange = { password = it; localError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Min. 6 characters",
                                color = TextSecondary.copy(alpha = 0.4f), fontSize = 13.sp
                            )
                        },
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
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
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

                // Confirm password field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CONFIRM PASSWORD", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; localError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Re-enter password",
                                color = TextSecondary.copy(alpha = 0.4f), fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (showPassword)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        // Highlight red if passwords don't match yet
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = if (confirmPassword.isNotEmpty() && confirmPassword != password)
                                DangerRed else GoldenBright,
                            unfocusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password)
                                DangerRed else DarkBorder,
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
                                handleSignup(
                                    username, password, confirmPassword,
                                    setLocalError = { localError = it },
                                    onSignup = onSignup
                                )
                            }
                        )
                    )
                }

                // Error message
                if (displayError.isNotBlank()) {
                    Text(
                        displayError,
                        color = DangerRed, fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Signup button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        handleSignup(
                            username, password, confirmPassword,
                            setLocalError = { localError = it },
                            onSignup = onSignup
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor   = TextPrimary
                    )
                ) {
                    Text(
                        "✔  CREATE ACCOUNT",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Go to login
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ", color = TextSecondary, fontSize = 12.sp)
                TextButton(onClick = onGoToLogin) {
                    Text(
                        "LOGIN",
                        color = GoldenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Local validation before calling onSignup.
 * Checks passwords match before hitting the database.
 */
private fun handleSignup(
    username: String,
    password: String,
    confirmPassword: String,
    setLocalError: (String) -> Unit,
    onSignup: (String, String) -> Unit
) {
    when {
        username.isBlank()            -> setLocalError("Username cannot be empty.")
        username.length < 3           -> setLocalError("Username must be at least 3 characters.")
        password.length < 6           -> setLocalError("Password must be at least 6 characters.")
        password != confirmPassword   -> setLocalError("Passwords do not match.")
        else                          -> onSignup(username, password)
    }
}