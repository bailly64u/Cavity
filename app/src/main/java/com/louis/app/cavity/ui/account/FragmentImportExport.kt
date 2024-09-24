package com.louis.app.cavity.ui.account

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.WorkInfo
import com.google.android.material.transition.MaterialSharedAxis
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentImportExportBinding
import com.louis.app.cavity.domain.Environment
import com.louis.app.cavity.domain.worker.UploadWorker
import com.louis.app.cavity.util.DateFormatter
import com.louis.app.cavity.util.TransitionHelper
import com.louis.app.cavity.util.setVisible
import com.louis.app.cavity.util.setupNavigation
import com.louis.app.cavity.util.showSnackbar
import com.robinhood.ticker.TickerUtils

class FragmentImportExport : Fragment(R.layout.fragment_import_export) {
    private var _binding: FragmentImportExportBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by activityViewModels()
    private val importExportViewModel: ImportExportViewModel by activityViewModels()
    private val args: FragmentImportExportArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransitionHelper(this).setSharedAxisTransition(
            MaterialSharedAxis.Z,
            navigatingForward = false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImportExportBinding.bind(view)

        setupNavigation(binding.appBar.toolbar)

        with(importExportViewModel) {
            fetchHealth(args.isImport)
            fetchDistantBottleCount()
            fetchLocalBottleCount()
        }

        initTickerViews()
        updateUiState()
        observe()
        setListeners()
    }

    private fun initTickerViews() {
        val textAppearanceApplier = AppCompatTextView(requireContext()).apply {
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Cavity_Caption)
        }

        binding.bottles.apply {
            textPaint.typeface = textAppearanceApplier.paint.typeface
            setCharacterLists(TickerUtils.provideNumberList())
        }

        binding.deviceBottles.apply {
            textPaint.typeface = textAppearanceApplier.paint.typeface
            setCharacterLists(TickerUtils.provideNumberList())
        }

        binding.lastAction.apply {
            textPaint.typeface = textAppearanceApplier.paint.typeface
            setCharacterLists(TickerUtils.provideAlphabeticalList())
        }
    }

    private fun updateUiState() {
        val import = args.isImport
        val arrowSrc = if (import) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up
        val btn = if (import) R.string.import_ else R.string.export
        val btnSrc = if (import) R.drawable.ic_import else R.drawable.ic_export
        val healthcheck =
            if (import) R.string.healthcheck_import_warn else R.string.healthcheck_export_warn
        val explanation =
            if (import) R.string.backup_erasure_warn_import else R.string.backup_erasure_warn_export

        with(binding) {
            arrow.setImageResource(arrowSrc)
            erasureWarn.text = getString(explanation)
            warn.text = getString(healthcheck)
            submit.text = getString(btn)
            submit.setIconResource(btnSrc)
            cellar.text = getString(R.string.current_device, Environment.getDeviceName())
        }
    }

    private fun observe() {
        loginViewModel.account.observe(viewLifecycleOwner) {
            val fallback = getString(R.string.unknown)
            binding.backup.text = getString(R.string.backup_device_name, it?.lastUser ?: fallback)

            val date = DateFormatter.formatDate(it?.lastUpdateTime, "dd MMMM yyyy, HH:mm")
            binding.lastAction.text = getString(R.string.last_action, date)
        }

        importExportViewModel.health.observe(viewLifecycleOwner) { stringRes ->
            changeWarningVisibilty(stringRes)
        }

        importExportViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.setVisible(it, invisible = true)
        }

        importExportViewModel.navigateToLogin.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let {
                val action = FragmentImportExportDirections.importExportToLogin()
                findNavController().navigate(action)
            }
        }

        importExportViewModel.distantBottleCount.observe(viewLifecycleOwner) {
            val text = resources.getQuantityString(R.plurals.bottles, it, it)
            binding.bottles.text = text
        }

        importExportViewModel.localBottleCount.observe(viewLifecycleOwner) {
            val text = resources.getQuantityString(R.plurals.bottles, it, it)
            binding.deviceBottles.text = text
        }

        importExportViewModel.userFeedback.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { stringRes ->
                binding.coordinator.showSnackbar(stringRes)
            }
        }

        importExportViewModel.userFeedbackString.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { string ->
                binding.coordinator.showSnackbar(string)
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
                        binding.coordinator.showSnackbar(R.string.base_error)
                        importExportViewModel.pruneWorks()
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        val message: Int
                        val isUpload = it.tags.contains(UploadWorker.WORK_TAG)

                        importExportViewModel.preventHealthCheckSpam = false

                        message = if (isUpload) {
                            loginViewModel.updateAccountLastUpdateLocally()
                            R.string.export_done
                        } else {
                            R.string.import_done
                        }

                        binding.progressBar.setVisible(false)
                        binding.coordinator.showSnackbar(message)

                        with(importExportViewModel) {
                            fetchLocalBottleCount()
                            fetchDistantBottleCount()
                            pruneWorks()
                        }
                    }

                    WorkInfo.State.CANCELLED -> {
                        importExportViewModel.pruneWorks()
                    }

                    else -> Unit
                }
            } else {
                binding.progressBar.setVisible(false)
            }
        }
    }

    private fun setListeners() {
        binding.confirmDanger.setOnClickListener {
            changeWarningVisibilty(null)
        }

        binding.submit.setOnClickListener {
            importExportViewModel.run {
                if (args.isImport) import() else export()
            }
        }
    }

    private fun changeWarningVisibilty(@StringRes text: Int?) {
        val isWarn = text !== null

        with(binding) {
            submit.isEnabled = !isWarn
            text?.let { warn.text = getString(it) }
            healthcheckWarn.setVisible(isWarn)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
