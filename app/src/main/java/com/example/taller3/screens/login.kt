package com.example.taller3.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taller3.model.UserAuthViewModel
import com.example.taller3.navigation.AppScreens
import com.example.taller3.utils.validateForm
import com.google.firebase.auth.FirebaseAuth

@Composable
fun Login(
    modifier: Modifier = Modifier,
    mAuth: FirebaseAuth,
    controller: NavController,
    authViewModel: UserAuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        mAuth.currentUser?.let {
            controller.navigate(AppScreens.Home.name)
        }
    }

    fun login() {
        if (validateForm(user.email, user.password)) {
            mAuth.signInWithEmailAndPassword(user.email, user.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        controller.navigate(AppScreens.Home.name)
                    } else {
                        Toast.makeText(
                            context,
                            "Login error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = user.email,
            onValueChange = { authViewModel.updateEmailClass(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = user.emailError.isNotEmpty()
        )

        if (user.emailError.isNotEmpty()) {
            Text(text = user.emailError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = user.password,
            onValueChange = { authViewModel.updatePassClass(it) },
            label = { Text("Password") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password
                ),
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            supportingText = {
                Text(user.passError, color = Color.Red)
            },
            singleLine = true,
            isError = user.passError.isNotEmpty()
        )

        if (user.passError.isNotEmpty()) {
            Text(text = user.passError, color = MaterialTheme.colorScheme.error)
        }

        Text(
            text = buildAnnotatedString {
                append("Doesn't have an account yet? ")
                // Add a clickable, styled link
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "register",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        linkInteractionListener = {
                            controller.navigate(AppScreens.Register.name)
                        }
                    )
                ) {
                    append("Register here")
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { login() }
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(32.dp))



    }
}
