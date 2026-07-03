package com.example.smarttaskmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttaskmanager.domain.model.AppUser
import com.example.smarttaskmanager.domain.repository.AuthRepository
import com.example.smarttaskmanager.domain.repository.AuthResult
import com.example.smarttaskmanager.notification.TeamActivityNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(val user: AppUser? = null, val isLoading: Boolean = true)

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isRegisterMode: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/** Komanda üzvünün giriş/qeydiyyat vəziyyətini idarə edir (Firebase Authentication). */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val teamActivityNotifier: TeamActivityNotifier
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = authRepository.currentUser
        .onEach { user ->
            if (user != null) teamActivityNotifier.start() else teamActivityNotifier.stop()
        }
        .map { AuthUiState(user = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthUiState())

    init {
        // Tətbiq açılan kimi VƏ hər 5 dəqiqədən bir hesabın Console-dan silinib-silinmədiyini
        // yoxlayır - silinibsə telefonu avtomatik çıxış etdirir (Firebase sessiyanı özü ləğv etmir).
        viewModelScope.launch {
            while (true) {
                authRepository.validateSession()
                delay(5 * 60 * 1000L)
            }
        }
    }

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    fun onEmailChanged(v: String) = update { it.copy(email = v, errorMessage = null) }
    fun onPasswordChanged(v: String) = update { it.copy(password = v, errorMessage = null) }
    fun onDisplayNameChanged(v: String) = update { it.copy(displayName = v, errorMessage = null) }
    fun toggleMode() = update { it.copy(isRegisterMode = !it.isRegisterMode, errorMessage = null) }

    fun submit() {
        val state = _formState.value
        if (state.email.isBlank() || state.password.isBlank() ||
            (state.isRegisterMode && state.displayName.isBlank())
        ) {
            update { it.copy(errorMessage = "Bütün sahələri doldurun.") }
            return
        }
        viewModelScope.launch {
            update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = if (state.isRegisterMode) {
                authRepository.signUp(state.email, state.password, state.displayName)
            } else {
                authRepository.signIn(state.email, state.password)
            }
            when (result) {
                is AuthResult.Success -> update { it.copy(isSubmitting = false) }
                is AuthResult.Error -> update { it.copy(isSubmitting = false, errorMessage = result.messageAz) }
            }
        }
    }

    fun signOut() = authRepository.signOut()

    private inline fun update(block: (AuthFormState) -> AuthFormState) {
        _formState.value = block(_formState.value)
    }
}
