package com.darkjade.streamlib.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkjade.streamlib.data.db.entity.ProfileEntity
import com.darkjade.streamlib.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val activeProfile: ProfileEntity? = null,
    val allProfiles: List<ProfileEntity> = emptyList(),
)

class AccountViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val active = profileRepository.ensureDefaultProfile()
            _uiState.value = _uiState.value.copy(activeProfile = active)

            profileRepository.observeAll().collect { list ->
                _uiState.value = _uiState.value.copy(allProfiles = list)
            }
        }
    }

    fun addProfile(name: String) {
        viewModelScope.launch { profileRepository.addProfile(name) }
    }

    fun switchProfile(profile: ProfileEntity) {
        _uiState.value = _uiState.value.copy(activeProfile = profile)
    }
}
