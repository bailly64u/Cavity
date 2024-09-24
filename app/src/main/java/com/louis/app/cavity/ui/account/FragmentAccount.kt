package com.louis.app.cavity.ui.account

import android.Manifest
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import com.google.android.material.transition.MaterialSharedAxis
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentAccountBinding
import com.louis.app.cavity.ui.SimpleInputDialog
import com.louis.app.cavity.domain.worker.AutoUploadWorker
import com.louis.app.cavity.domain.worker.PruneWorker
import com.louis.app.cavity.domain.worker.UploadWorker
import com.louis.app.cavity.ui.settings.SettingsViewModel
import com.louis.app.cavity.util.DateFormatter
import com.louis.app.cavity.util.PermissionChecker
import com.louis.app.cavity.util.TransitionHelper
import com.louis.app.cavity.util.setVisible
import com.louis.app.cavity.util.setupNavigation
import com.louis.app.cavity.util.showSnackbar
import com.louis.app.cavity.util.spToPx
import com.robinhood.ticker.TickerUtils

class FragmentAccount : Fragment(R.layout.fragment_account) {
    private lateinit var readPermissionChecker: PermissionChecker
    private lateinit var writePermissionChecker: PermissionChecker
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private val importExportViewModel: ImportExportViewModel by activityViewModels()

    private lateinit var transitionHelper: TransitionHelper

    private var wannaImport = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postponeEnterTransition()
        transitionHelper = TransitionHelper(this).apply {
            setFadeThroughOnEnterAndExit()
        }

        readPermissionChecker =
            object : PermissionChecker(this, arrayOf(READ_PERMISSION)) {
                override fun onPermissionsAccepted() {
                    navigateToImportFiles()
                }

                override fun onPermissionsDenied() {
                    binding.coordinator.showSnackbar(R.string.permissions_denied_external)
                }
            }

        writePermissionChecker = object : PermissionChecker(this, arrayOf(WRITE_PERMISSION)) {
            override fun onPermissionsAccepted() {
                navigateToImportExport(wannaImport)
            }

            override fun onPermissionsDenied() {
                binding.coordinator.showSnackbar(R.string.permissions_denied_external)
            }
        }

        val currentBackStackEntry = findNavController().currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle

        savedStateHandle.getLiveData<Boolean>(FragmentLogin.LOGIN_SUCCESSFUL)
            .observe(currentBackStackEntry) {
                if (!it) {
                    val startDestination = findNavController().graph.startDestinationId
                    val action = FragmentAccountDirections.accountToHome()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(startDestination, true)
                        .build()

                    findNavController().navigate(action, navOptions)
                } else {
                    startPostponedEnterTransition()
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountBinding.bind(view)

        setupNavigation(binding.toolbar)

        if (!settingsViewModel.getAutoBackup()) {
            updateAutoBackupStatus(AutoUploadWorker.HEALTH_STATE_USER_DISABLED)
        } else if (!importExportViewModel.preventHealthCheckSpam) {
            importExportViewModel.autoBackupHealthCheck()
        }

        observe()
        initTickerView()
        setListeners()
        setupToolbar()
    }

    private fun observe() {
        loginViewModel.account.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.email.text = it.email

                val date = DateFormatter.formatDate(it.lastUpdateTime, "dd MMMM yyyy, HH:mm")
                binding.lastBackupDate.text = date
                startPostponedEnterTransition()
            } else {
                val action = FragmentAccountDirections.accountToLogin()
                findNavController().navigate(action)
            }
        }

        loginViewModel.deletedEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let {
                importExportViewModel.cleanAccountDatabase()
                loginViewModel.logout()
            }
        }

        importExportViewModel.workProgress.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                        binding.progressBar.setVisible(true)
                    }

                    WorkInfo.State.FAILED -> {
                        binding.progressBar.setVisible(false)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        binding.progressBar.setVisible(false)

                        if (it.tags.contains(UploadWorker.WORK_TAG)) {
                            loginViewModel.updateAccountLastUpdateLocally()
                            updateAutoBackupStatus(AutoUploadWorker.HEALTH_STATE_SUCCESS)
                        }

                        if (it.tags.contains(PruneWorker.WORK_TAG)) {
                            loginViewModel.logout()
                        }
                    }

                    else -> Unit
                }
            } else {
                binding.progressBar.setVisible(false)
            }
        }

        importExportViewModel.autoBackupWorkProgress.observe(viewLifecycleOwner) {
            if (it !== null && it.state == WorkInfo.State.RUNNING) {
                val healthState =
                    it.progress.getInt(AutoUploadWorker.WORK_DATA_HEALTH_STATE_KEY, -1)

                updateAutoBackupStatus(healthState)

                if (healthState == AutoUploadWorker.HEALTH_STATE_SUCCESS) {
                    loginViewModel.updateAccountLastUpdateLocally()
                }
            }
        }

        importExportViewModel.healthCheckWorkProgress.observe(viewLifecycleOwner) {
            if (it !== null && it.state.isFinished && settingsViewModel.getAutoBackup()) {
                val healthState =
                    it.outputData.getInt(AutoUploadWorker.WORK_DATA_HEALTH_STATE_KEY, -1)

                updateAutoBackupStatus(healthState)
            }
        }
    }

    private fun initTickerView() {
        val textAppearanceApplier = AppCompatTextView(requireContext()).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Cavity_Caption)
        }

        binding.lastBackupDate.apply {
            textPaint.typeface = textAppearanceApplier.paint.typeface
            textPaint.textSize = requireContext().spToPx(15f)
            setCharacterLists(TickerUtils.provideAlphabeticalList())
        }
    }

    private fun setListeners() {
        binding.exportBtn.setOnClickListener {
            wannaImport = false
            writePermissionChecker.askPermissionsIfNecessary()
        }

        binding.importBtn.setOnClickListener {
            wannaImport = true
            writePermissionChecker.askPermissionsIfNecessary()
        }

        binding.imageBtn.setOnClickListener {
            readPermissionChecker.askPermissionsIfNecessary()
        }

        binding.deleteBtn.setOnClickListener {
            val resources = SimpleInputDialog.DialogContent(
                title = R.string.delete_account,
                hint = R.string.password,
                icon = R.drawable.ic_password,
            ) {
                loginViewModel.deleteAccount(it)
            }

            SimpleInputDialog(
                requireContext(),
                layoutInflater,
                viewLifecycleOwner,
                passwordInput = true
            ).show(resources)
        }

        binding.toggleAutoBackup.apply {
            thumbDrawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.switch_thumb,
                requireContext().theme
            )

            isChecked = settingsViewModel.getAutoBackup()

            setOnCheckedChangeListener { _, isChecked ->
                settingsViewModel.setAutoBackup(isChecked)

                if (isChecked) {
                    importExportViewModel.autoBackupHealthCheck()
                    importExportViewModel.enableAutoBackups()
                } else {
                    importExportViewModel.cancelCurrentAutoBackup()
                    updateAutoBackupStatus(AutoUploadWorker.HEALTH_STATE_USER_DISABLED)
                }
            }
        }

        binding.toggleDeleteAccount.setOnClickListener {
            with(binding) {
                deleteAccountText.setVisible(true)
                deleteBtn.setVisible(true)
                scrollView.postDelayed({ binding.scrollView.fullScroll(View.FOCUS_DOWN) }, 500)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.logout) {
                loginViewModel.logout()
                return@setOnMenuItemClickListener true
            }

            false
        }
    }

    private fun navigateToImportExport(isImport: Boolean) {
        transitionHelper.setSharedAxisTransition(MaterialSharedAxis.Z, true)

        val title = if (isImport) R.string.import_ else R.string.export
        val action = FragmentAccountDirections.accountToImportExport(
            isImport = isImport,
            title = getString(title)
        )

        findNavController().navigate(action)
    }

    private fun navigateToImportFiles() {
        transitionHelper.setSharedAxisTransition(MaterialSharedAxis.Z, true)

        val action = FragmentAccountDirections.accountToImportFiles()
        findNavController().navigate(action)
    }

    private fun updateAutoBackupStatus(healthState: Int) {
        val success = healthState == AutoUploadWorker.HEALTH_STATE_SUCCESS
                || healthState == AutoUploadWorker.HEALTH_STATE_USER_DISABLED

        if (healthState != -1) {
            binding.backupStatusDetails.setVisible(!success)
        }

        binding.backupStatuProgressBar.setVisible(false)

        val uiInfo: BackupStatusUi? = when (healthState) {
            AutoUploadWorker.HEALTH_STATE_SUCCESS ->
                BackupStatusUi(
                    R.string.backup_status_active,
                    R.string.auto_backup_done,
                    R.color.cavity_green
                )

            AutoUploadWorker.HEALTH_STATE_FAILED ->
                BackupStatusUi(
                    R.string.backup_status_error,
                    R.string.auto_backup_unavailable,
                    R.color.cavity_red
                )

            AutoUploadWorker.HEALTH_STATE_UNAUTHORIZED ->
                BackupStatusUi(
                    R.string.backup_status_pause,
                    R.string.auto_backup_unauthorized,
                    R.color.cavity_yellow
                )

            AutoUploadWorker.HEALTH_STATE_PREVENT_ACCOUNT_SWITCH ->
                BackupStatusUi(
                    R.string.backup_status_pause,
                    R.string.auto_backup_not_matching,
                    R.color.cavity_yellow
                )

            AutoUploadWorker.HEALTH_STATE_PREVENT_OVERWRITE ->
                BackupStatusUi(
                    R.string.backup_status_pause,
                    R.string.auto_backup_overwrite_data,
                    R.color.cavity_yellow
                )

            AutoUploadWorker.HEALTH_STATE_USER_DISABLED ->
                BackupStatusUi(
                    R.string.backup_status_disabled,
                    null,
                    R.color.cavity_grey
                )

            else -> null
        }

        uiInfo?.let { backupStatusUI ->
            with(binding) {
                backupStatusMore.text = getString(backupStatusUI.title)
                backupStatusDetails.text = backupStatusUI.text?.let { getString(it) }
                backupStateIcon.imageTintList =
                    ColorStateList.valueOf(requireContext().getColor(backupStatusUI.color))
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        importExportViewModel.preventHealthCheckSpam = false
    }

    data class BackupStatusUi(
        @StringRes val title: Int,
        @StringRes val text: Int?,
        @ColorRes val color: Int
    )

    companion object {
        private const val WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
