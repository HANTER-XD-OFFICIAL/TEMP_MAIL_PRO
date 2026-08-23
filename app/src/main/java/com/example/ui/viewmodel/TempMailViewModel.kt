package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.DomainItem
import com.example.data.api.MessageDetailResponse
import com.example.data.api.MessageHeaderItem
import com.example.data.db.AppDatabase
import com.example.data.db.SavedAccountEntity
import com.example.data.repository.TempMailRepository
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LocalizationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiNotification(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

class TempMailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TempMailRepository(db.accountDao())

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    val appStrings: StateFlow<AppStrings> = _currentLanguage.map {
        LocalizationManager.getStrings(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LocalizationManager.getStrings(AppLanguage.ENGLISH)
    )

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        showNotification("${lang.flagEmoji} Language set to ${lang.displayName} (${lang.nativeName})")
    }

    val savedAccounts: StateFlow<List<SavedAccountEntity>> = repository.allSavedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount: StateFlow<SavedAccountEntity?> = repository.activeAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accountsCount: StateFlow<Int> = repository.accountsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _messages = MutableStateFlow<List<MessageHeaderItem>>(emptyList())
    val messages: StateFlow<List<MessageHeaderItem>> = _messages.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _isGeneratingAccount = MutableStateFlow(false)
    val isGeneratingAccount: StateFlow<Boolean> = _isGeneratingAccount.asStateFlow()

    private val _activeMessageDetail = MutableStateFlow<MessageDetailResponse?>(null)
    val activeMessageDetail: StateFlow<MessageDetailResponse?> = _activeMessageDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private val _availableDomains = MutableStateFlow<List<DomainItem>>(emptyList())
    val availableDomains: StateFlow<List<DomainItem>> = _availableDomains.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _autoRefreshSeconds = MutableStateFlow(10)
    val autoRefreshSeconds: StateFlow<Int> = _autoRefreshSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(600L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isMailboxExpired = MutableStateFlow(false)
    val isMailboxExpired: StateFlow<Boolean> = _isMailboxExpired.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var hasInitializedInitialAccount = false

    init {
        loadAvailableDomains()
        observeActiveAccountAndStartSync()
    }

    private fun observeActiveAccountAndStartSync() {
        viewModelScope.launch {
            activeAccount.collectLatest { account ->
                if (account != null) {
                    hasInitializedInitialAccount = true
                    val remSec = maxOf(0L, (account.expiresAt - System.currentTimeMillis()) / 1000L)
                    _remainingSeconds.value = remSec
                    _isMailboxExpired.value = remSec <= 0L
                    refreshInbox(silent = false)
                    restartTickerAndSyncLoop()
                } else if (!hasInitializedInitialAccount) {
                    // Wait brief moment to see if DB loads existing accounts
                    delay(350)
                    if (activeAccount.value == null && savedAccounts.value.isEmpty()) {
                        hasInitializedInitialAccount = true
                        generateQuickRandomAccount()
                    }
                }
            }
        }
    }

    private fun restartTickerAndSyncLoop() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            var syncSecCount = 6
            while (isActive) {
                val current = activeAccount.value
                if (current != null) {
                    val now = System.currentTimeMillis()
                    val remSec = maxOf(0L, (current.expiresAt - now) / 1000L)
                    _remainingSeconds.value = remSec
                    _isMailboxExpired.value = remSec <= 0L
                } else {
                    _remainingSeconds.value = 600L
                    _isMailboxExpired.value = false
                }

                syncSecCount--
                if (syncSecCount <= 0) {
                    syncSecCount = 6
                    _autoRefreshSeconds.value = 0
                    if (activeAccount.value != null && !_isLoadingMessages.value) {
                        refreshInbox(silent = true)
                    }
                } else {
                    _autoRefreshSeconds.value = syncSecCount
                }

                delay(1000)
            }
        }
    }

    fun extendActiveMailboxTime(additionalMinutes: Int = 10) {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            val newExpiresAt = repository.extendAccountExpiration(current.address, additionalMinutes)
            val remSec = maxOf(0L, (newExpiresAt - System.currentTimeMillis()) / 1000L)
            _remainingSeconds.value = remSec
            _isMailboxExpired.value = false
            showNotification("⏰ Time extended by +$additionalMinutes minutes! (${formatTimer(remSec)} remaining)")
        }
    }

    fun resetActiveMailboxTime(minutes: Int = 10) {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            val newExpiresAt = repository.resetAccountExpiration(current.address, minutes)
            val remSec = maxOf(0L, (newExpiresAt - System.currentTimeMillis()) / 1000L)
            _remainingSeconds.value = remSec
            _isMailboxExpired.value = false
            showNotification("⏱️ Timer reset to $minutes minutes")
        }
    }

    private fun formatTimer(totalSeconds: Long): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }

    fun loadAvailableDomains() {

        viewModelScope.launch {
            val result = repository.getAvailableDomains()
            result.onSuccess { domains ->
                _availableDomains.value = domains
            }.onFailure { err ->
                // Keep trying or fallback
            }
        }
    }

    fun refreshInbox(silent: Boolean = false) {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            if (!silent) _isLoadingMessages.value = true
            val token = repository.ensureValidToken(current) ?: "local_${System.currentTimeMillis()}"

            val result = repository.fetchMessages(token, current.address)
            if (!silent) _isLoadingMessages.value = false

            result.onSuccess { list ->
                _messages.value = list
            }.onFailure { e ->
                if (!silent) {
                    showNotification("Inbox sync failed: ${e.localizedMessage ?: "Network error"}", isError = true)
                }
            }
        }
    }

    fun sendSampleTestEmail(serviceType: String) {
        val current = activeAccount.value
        if (current == null) {
            showNotification("Please create or select an email account first.", isError = true)
            return
        }
        val sample = repository.injectSampleVerificationEmail(current.address, serviceType)
        refreshInbox(silent = true)
        showNotification("📥 New ${serviceType.replaceFirstChar { it.uppercase() }} test verification email arrived in your inbox!")
    }

    fun generateQuickRandomAccount(domain: String? = null, label: String = "") {
        viewModelScope.launch {
            _isGeneratingAccount.value = true
            val result = repository.createRandomAccount(customDomain = domain, label = label)
            _isGeneratingAccount.value = false

            result.onSuccess { account ->
                _messages.value = emptyList()
                showNotification("New temporary mailbox generated: ${account.address}")
            }.onFailure { e ->
                showNotification("Failed to generate mailbox: ${e.localizedMessage ?: "Error"}", isError = true)
            }
        }
    }

    fun createCustomAccount(username: String, domain: String, pass: String, label: String = "") {
        viewModelScope.launch {
            _isGeneratingAccount.value = true
            val result = repository.createCustomAccount(username, domain, pass, label)
            _isGeneratingAccount.value = false

            result.onSuccess { account ->
                _messages.value = emptyList()
                showNotification("Custom mailbox created: ${account.address}")
            }.onFailure { e ->
                showNotification("Error: ${e.localizedMessage ?: "Could not create account"}", isError = true)
            }
        }
    }

    fun loginExistingAccount(address: String, pass: String, label: String = "") {
        viewModelScope.launch {
            _isGeneratingAccount.value = true
            val result = repository.loginExistingAccount(address, pass, label)
            _isGeneratingAccount.value = false

            result.onSuccess { account ->
                _messages.value = emptyList()
                showNotification("Logged in to ${account.address}")
            }.onFailure { e ->
                showNotification("Login failed: ${e.localizedMessage ?: "Check email/password"}", isError = true)
            }
        }
    }

    fun switchActiveAccount(account: SavedAccountEntity) {
        viewModelScope.launch {
            repository.switchActiveAccount(account.address)
            showNotification("Switched to ${account.address}")
        }
    }

    fun deleteAccount(account: SavedAccountEntity, deleteFromServer: Boolean = false) {
        viewModelScope.launch {
            repository.deleteSavedAccount(account.address, deleteFromServer)
            showNotification("Account removed from vault")
            if (activeAccount.value?.address == account.address) {
                _messages.value = emptyList()
                _activeMessageDetail.value = null
                val remaining = savedAccounts.value.filter { it.address != account.address }
                if (remaining.isNotEmpty()) {
                    switchActiveAccount(remaining.first())
                } else {
                    generateQuickRandomAccount()
                }
            }
        }
    }

    fun updateAccountLabel(address: String, label: String) {
        viewModelScope.launch {
            repository.updateAccountLabel(address, label)
            showNotification("Tag updated")
        }
    }

    fun openMessage(messageId: String) {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            _isLoadingDetail.value = true
            _activeMessageDetail.value = null

            val token = repository.ensureValidToken(current)
            if (token == null) {
                _isLoadingDetail.value = false
                showNotification("Authentication token expired", isError = true)
                return@launch
            }

            val result = repository.fetchMessageDetail(token, messageId, current.address)
            _isLoadingDetail.value = false

            result.onSuccess { detail ->
                _activeMessageDetail.value = detail
                // Mark seen locally
                _messages.value = _messages.value.map {
                    if (it.id == messageId) it.copy(seen = true) else it
                }
            }.onFailure { e ->
                showNotification("Could not read message: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun closeMessageDetail() {
        _activeMessageDetail.value = null
    }

    fun deleteCurrentMessage(messageId: String) {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            val token = repository.ensureValidToken(current) ?: return@launch
            val result = repository.deleteMessage(token, messageId)
            result.onSuccess {
                _messages.value = _messages.value.filter { it.id != messageId }
                if (_activeMessageDetail.value?.id == messageId) {
                    _activeMessageDetail.value = null
                }
                showNotification("Message deleted")
            }.onFailure {
                showNotification("Failed to delete message", isError = true)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Copied to clipboard") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TempMail", text)
            clipboard.setPrimaryClip(clip)
            showNotification(label)
        } catch (e: Exception) {
            showNotification("Failed to copy", isError = true)
        }
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message = message, isError = isError)
    }

    fun clearNotification() {
        _notification.value = null
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
