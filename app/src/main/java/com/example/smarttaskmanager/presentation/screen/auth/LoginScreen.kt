package com.example.smarttaskmanager.presentation.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttaskmanager.presentation.viewmodel.AuthViewModel

/**
 * Komanda üzvləri üçün giriş/qeydiyyat ekranı (email + parol).
 * Qeydiyyatda göstərilən ad, digər üzvlərə "kim yaratdı/kim tamamladı" kimi görünür.
 */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(authState.user) {
        if (authState.user != null) onAuthenticated()
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "PSD / Performance Management Team",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                if (formState.isRegisterMode) "Yeni hesab yaradın" else "Hesabınıza daxil olun",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (formState.isRegisterMode) {
                OutlinedTextField(
                    value = formState.displayName,
                    onValueChange = viewModel::onDisplayNameChanged,
                    label = { Text("Adınız") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = formState.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("E-poçt") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = formState.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("Parol") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            formState.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isSubmitting
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(if (formState.isRegisterMode) "Qeydiyyatdan keç" else "Daxil ol")
                }
            }

            TextButton(onClick = viewModel::toggleMode, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (formState.isRegisterMode) "Artıq hesabınız var? Daxil olun"
                    else "Hesabınız yoxdur? Qeydiyyatdan keçin"
                )
            }
        }
    }
}
