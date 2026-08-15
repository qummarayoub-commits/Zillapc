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

    /** Sets the profile's picture to a picked content URI (persisted as a string). */
    fun setAvatar(uriString: String) = updateActiveProfile { it.copy(avatarRes = uriString) }

    /** Sets the profile's cover/banner image to a picked content URI. */
    fun setBanner(uriString: String) = updateActiveProfile { it.copy(bannerRes = uriString, bannerOffsetX = 0f, bannerOffsetY = 0f, bannerScale = 1f) }

    /** Called when the user finishes pinch/drag-adjusting the banner position. */
    fun setBannerAdjustment(scale: Float, offsetX: Float, offsetY: Float) =
        updateActiveProfile { it.copy(bannerScale = scale, bannerOffsetX = offsetX, bannerOffsetY = offsetY) }

    fun setUsername(name: String) {
        if (name.isBlank()) return
        updateActiveProfile { it.copy(name = name.trim()) }
    }

    private val restrictionOptions = listOf("None", "7+", "13+", "16+", "18+")
    private val languageOptions = listOf("English", "Urdu", "Spanish", "Hindi")

    fun cycleContentRestriction() = updateActiveProfile { profile ->
        val next = nextInCycle(restrictionOptions, profile.contentRestriction)
        profile.copy(contentRestriction = next)
    }

    fun cycleAudioLanguage() = updateActiveProfile { profile ->
        val next = nextInCycle(languageOptions, profile.audioLanguage)
        profile.copy(audioLanguage = next)
    }

    fun cycleSubtitleLanguage() = updateActiveProfile { profile ->
        val next = nextInCycle(languageOptions, profile.subtitleLanguage)
        profile.copy(subtitleLanguage = next)
    }

    private fun nextInCycle(options: List<String>, current: String): String {
        val index = options.indexOf(current).let { if (it == -1) 0 else it }
        return options[(index + 1) % options.size]
    }

    private fun updateActiveProfile(transform: (ProfileEntity) -> ProfileEntity) {
        val current = _uiState.value.activeProfile ?: return
        val updated = transform(current)
        _uiState.value = _uiState.value.copy(activeProfile = updated)
        viewModelScope.launch { profileRepository.updateProfile(updated) }
    }
}
