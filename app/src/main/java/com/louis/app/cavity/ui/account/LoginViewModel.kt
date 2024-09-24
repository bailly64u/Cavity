package com.louis.app.cavity.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.Environment
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.repository.PrefsRepository
import com.louis.app.cavity.domain.error.ErrorReporter
import com.louis.app.cavity.domain.error.SentryErrorReporter
import com.louis.app.cavity.network.response.ApiResponse
import com.louis.app.cavity.network.response.LoginResponse
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val prefsRepository = PrefsRepository.getInstance(app)
    private val accountRepository = AccountRepository.getInstance(app)
    private val errorReporter = SentryErrorReporter.getInstance(app)

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    private val _userFeedbackString = MutableLiveData<Event<String>>()
    val userFeedbackString: LiveData<Event<String>>
        get() = _userFeedbackString

    private val _account = MutableLiveData<LoginResponse?>(null)
    val account: LiveData<LoginResponse?>
        get() = _account

    private val _navigateToConfirm = MutableLiveData<Event<Unit>>()
    val navigateToConfirm: LiveData<Event<Unit>>
        get() = _navigateToConfirm

    private val _confirmedEvent = MutableLiveData<Event<Unit>>()
    val confirmedEvent: LiveData<Event<Unit>>
        get() = _confirmedEvent

    private val _deletedEvent = MutableLiveData<Event<Unit>>()
    val deletedEvent: LiveData<Event<Unit>>
        get() = _deletedEvent

    private var inConfirmationUser: String? = null
    private var sneakyTryCount = 0

    fun login(email: String, password: String) {
        // In case the user create an account, quit the app, and then try to re-login
        inConfirmationUser = email

        doApiCall(
            call = { accountRepository.login(email, password) },
            onSuccess = {
                prefsRepository.setApiToken(it.value.token)
                prefsRepository.setLastLogin(email)
                errorReporter.setScopeTag(ErrorReporter.USERNAME_ERROR_TAG, email)
                _account.postValue(it.value)
            }
        )
    }

    fun register(email: String, password: String) {
        doApiCall(
            call = { accountRepository.register(email, password) },
            onSuccess = {
                _userFeedback.postOnce(R.string.confirm_mail_sent)
                inConfirmationUser = email
                _navigateToConfirm.postOnce(Unit)
            }
        )
    }

    fun confirmAccount(registrationCode: String) {
        val email = inConfirmationUser

        if (email == null) {
            _userFeedback.postOnce(R.string.base_error)
            return
        }

        doApiCall(
            call = { accountRepository.confirmAccount(email, registrationCode) },
            onSuccess = {
                prefsRepository.setLastLogin(inConfirmationUser ?: "")
                inConfirmationUser = null
                prefsRepository.setApiToken(it.value.token)
                _confirmedEvent.postOnce(Unit)
                _account.postValue(it.value)
            }
        )
    }

    fun tryConnectWithSavedToken() {
        val token = prefsRepository.getApiToken()

        if (token.isBlank() || sneakyTryCount >= 1) {
            return
        }

        sneakyTryCount++

        // Do not use doApiCall(), we don't want error messages here since this action isn't user initiated
        viewModelScope.launch(IO) {
            val response = accountRepository.getAccount()

            if (response is ApiResponse.Success) {
                val email = response.value.email
                errorReporter.setScopeTag(ErrorReporter.USERNAME_ERROR_TAG, email)
                prefsRepository.setLastLogin(email)
                _account.postValue(response.value)
            }
        }
    }

    fun getLastLogin() = prefsRepository.getLastLogin()

    fun logout() {
        _account.value = null
        prefsRepository.setApiToken("")
        errorReporter.removeScopeTag(ErrorReporter.USERNAME_ERROR_TAG)
    }

    fun declareLostPassword(email: String) {
        doApiCall(
            call = { accountRepository.recoverPassword(email.trim()) },
            onSuccess = { _userFeedback.postOnce(R.string.reset_ok) }
        )
    }

    fun deleteAccount(password: String) {
        _account.value?.let { account ->
            doApiCall(
                call = { accountRepository.deleteAccount(account.email, password) },
                onSuccess = {
                    inConfirmationUser = null
                    prefsRepository.setLastLogin("")
                    prefsRepository.setApiToken("")
                    errorReporter.removeScopeTag(ErrorReporter.USERNAME_ERROR_TAG)
                    _deletedEvent.postOnce(Unit)
                }
            )
        }
    }

    fun updateAccountLastUpdateLocally() {
        val deviceName = Environment.getDeviceName()
        val copy =
            _account.value?.copy(lastUpdateTime = System.currentTimeMillis(), lastUser = deviceName)

        copy?.let {
            _account.value = it
        }
    }

    private fun <T> doApiCall(
        call: suspend () -> ApiResponse<T>,
        onSuccess: (ApiResponse.Success<T>) -> Unit
    ) {
        if (_isLoading.value == true) {
            return
        }

        _isLoading.postValue(true)

        viewModelScope.launch(IO) {
            try {
                when (val response = call()) {
                    is ApiResponse.Success -> onSuccess(response)
                    is ApiResponse.Failure -> _userFeedbackString.postOnce(response.message)
                    is ApiResponse.UnknownError -> _userFeedback.postOnce(R.string.base_error)
                    is ApiResponse.UnregisteredError -> _navigateToConfirm.postOnce(Unit)
                    is ApiResponse.UnauthorizedError -> Unit // Shouldn't happen in this ViewModel
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
