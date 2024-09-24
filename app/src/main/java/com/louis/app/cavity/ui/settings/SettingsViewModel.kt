package com.louis.app.cavity.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.louis.app.cavity.domain.repository.PrefsRepository
import com.louis.app.cavity.util.Event

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefsRepository = PrefsRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun setSkewBottle(skew: Boolean) {
        prefsRepository.setSkewBottle(skew)
    }

    fun setDefaultCurrency(currency: String) {
        prefsRepository.setDefaultCurrency(currency)
    }

    fun setTemplateSize(templateSize: Float) {
        prefsRepository.setTemplateSize(templateSize)
    }

    fun setAutoBackup(autoBackup: Boolean) {
        prefsRepository.setAutoBackup(autoBackup)
    }

    fun setErrorReportingConsent(consent: Boolean) {
        prefsRepository.setErrorReportingConsent(consent)
    }

    fun setPreventScrenshots(preventScreenshots: Boolean) {
        prefsRepository.setPreventScreenshots(preventScreenshots)
    }

    fun getSkewBottle() = prefsRepository.getSkewBottle()

    fun getDefaultCurrency() = prefsRepository.getDefaultCurrency()

    fun getTemplateSize() = prefsRepository.getTemplateSize()

    fun getAutoBackup() = prefsRepository.getAutoBackup()

    fun getErrorReportingConsent() = prefsRepository.getErrorReportingConsent()

    fun getPreventScreenshots() = prefsRepository.getPreventScreenshots()
}
