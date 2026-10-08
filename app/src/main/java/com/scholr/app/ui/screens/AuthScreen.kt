package com.scholr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.scholr.app.data.firebase.GoogleSignInHelper
import com.scholr.app.ui.clay.ClayButton
import com.scholr.app.ui.clay.ClayDividerLabel
import com.scholr.app.ui.clay.ClaySocialButton
import com.scholr.app.ui.clay.ClayTextField
import com.scholr.app.ui.components.ScholrWordmark
import com.scholr.app.ui.illustration.KineticBackground
import com.scholr.app.ui.theme.AcademicGreen
import com.scholr.app.ui.theme.FieldMedicine
import com.scholr.app.ui.theme.ScholrPurple
import com.scholr.app.ui.theme.TextBody
import kotlinx.coroutines.launch

enum class AuthMode { SignIn, SignUp }

/**
 * Sign In / Sign Up. The two modes share one layout because the only real
 * differences are the name field and the copy — keeping them in one
 * composable means the kinetic background never has to re-initialise when
 * the user flips between them.
 */
@Composable
fun AuthScreen(
    mode: AuthMode,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (name: String, email: String, password: String) -> Unit,
    onGoogleToken: (idToken: String) -> Unit,
    onSwitchMode: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isSignUp = mode == AuthMode.SignUp

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignInHelper = remember { GoogleSignInHelper(context) }
    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    fun launchGoogleSignIn() {
        if (googleLoading || isLoading) return
        scope.launch {
            googleLoading = true
            googleError = null
            try {
                val idToken = googleSignInHelper.requestIdToken()
                onGoogleToken(idToken)
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the account picker — nothing to report.
            } catch (e: GetCredentialException) {
                googleError = "Couldn't sign in with Google. Please try again."
            } catch (e: Exception) {
                googleError = "Couldn't sign in with Google. Please try again."
            } finally {
                googleLoading = false
            }
        }
    }

    KineticBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            ScholrWordmark()

            Spacer(Modifier.height(26.dp))

            Text(
                text = if (isSignUp) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isSignUp)
                    "Four million papers, organised around what you care about."
                else
                    "Your library, your conferences, your feed — right where you left them.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(Modifier.height(32.dp))

            if (isSignUp) {
                ClayTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full name",
                    placeholder = "Ada Lovelace",
                    leadingIcon = Icons.Rounded.PersonOutline
                )
                Spacer(Modifier.height(18.dp))
            }

            ClayTextField(
                value = email,
                onValueChange = { email = it },
                label = if (isSignUp) "Academic email" else "Email",
                placeholder = "you@university.edu",
                leadingIcon = Icons.Rounded.AlternateEmail,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )

            Spacer(Modifier.height(18.dp))

            ClayTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true
            )

            if (!isSignUp) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        style = MaterialTheme.typography.labelMedium,
                        color = ScholrPurple,
                        modifier = Modifier
                            .clickable { }
                            .padding(6.dp)
                    )
                }
            }

            val displayError = errorMessage ?: googleError
            if (displayError != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = displayError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(if (isSignUp) 30.dp else 20.dp))

            ClayButton(
                text = when {
                    isLoading && isSignUp -> "Creating account…"
                    isLoading -> "Signing in…"
                    isSignUp -> "Create account"
                    else -> "Sign in"
                },
                onClick = { onSubmit(name, email, password) },
                trailingIcon = Icons.Rounded.ArrowForward,
                enabled = !isLoading && !googleLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            ClayDividerLabel("or continue with")

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ClaySocialButton("G", onClick = { launchGoogleSignIn() }, accent = FieldMedicine)
                ClaySocialButton("in", onClick = { }, accent = ScholrPurple)
                ClaySocialButton("iD", onClick = { }, accent = AcademicGreen)
            }

            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSignUp) "Already have an account?"
                    else "New to Scholr?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isSignUp) "Sign in" else "Create one",
                    style = MaterialTheme.typography.labelMedium,
                    color = ScholrPurple,
                    modifier = Modifier
                        .clickable { onSwitchMode() }
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(36.dp))

            Box(Modifier.fillMaxWidth())
        }
    }
}
