package com.louis.app.cavity.ui.settings

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.louis.app.cavity.BuildConfig
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentSettingsBinding
import com.louis.app.cavity.domain.repository.PrefsRepository.Companion.MIN_TEMPLATE_SCALE
import com.louis.app.cavity.ui.addwine.FragmentCamera.Companion.TEMPLATE_ROTATION
import com.louis.app.cavity.util.TransitionHelper
import com.louis.app.cavity.util.setVisible
import com.louis.app.cavity.util.setupNavigation
import com.louis.app.cavity.util.showSnackbar
import java.math.RoundingMode
import java.text.DecimalFormat

class FragmentSettings : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TransitionHelper(this).apply {
            setFadeThrough(navigatingForward = false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupNavigation(binding.appBar.toolbar)

        observe()
        setListeners()
        setupSliderFormatter()
        setupCurrencyButtons()
        setupAppVersion()
    }

    private fun observe() {
        settingsViewModel.userFeedback.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { stringRes ->
                binding.coordinator.showSnackbar(stringRes)
            }
        }

        settingsViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.setVisible(it)
        }
    }

    private fun setListeners() {
        binding.toggleSkew.apply {
            thumbDrawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.switch_thumb,
                requireContext().theme
            )

            isChecked = settingsViewModel.getSkewBottle()

            setOnCheckedChangeListener { _, isChecked ->
                settingsViewModel.setSkewBottle(isChecked)
            }
        }

        binding.skewBottle.setOnClickListener {
            binding.toggleSkew.toggle()
        }

        binding.toggleErrorReportConsent.apply {
            thumbDrawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.switch_thumb,
                requireContext().theme
            )

            isChecked = settingsViewModel.getErrorReportingConsent()

            setOnCheckedChangeListener { _, isChecked ->
                settingsViewModel.setErrorReportingConsent(isChecked)
            }
        }

        binding.errorReportConsent.setOnClickListener {
            binding.toggleErrorReportConsent.toggle()
        }

        binding.togglePreventScreenshots.apply {
            thumbDrawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.switch_thumb,
                requireContext().theme
            )

            isChecked = settingsViewModel.getPreventScreenshots()

            setOnCheckedChangeListener { _, isChecked ->
                val secureFlag = WindowManager.LayoutParams.FLAG_SECURE
                val activity = requireActivity()

                if (isChecked) {
                    activity.window.setFlags(secureFlag, secureFlag)
                } else {
                    activity.window.clearFlags(secureFlag)
                }

                settingsViewModel.setPreventScrenshots(isChecked)
            }
        }

        binding.preventScreenshots.setOnClickListener {
            binding.togglePreventScreenshots.toggle()
        }

        val templateSize = settingsViewModel.getTemplateSize()
        binding.templateSizeSlider.apply {
            value = templateSize
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    showBottleTemplate()
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    binding.bottleTemplateDemo.setVisible(false)
                }
            })

            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    showBottleTemplate()
                }

                binding.bottleTemplateDemo.scaleX = value
                binding.bottleTemplateDemo.scaleY = value
            }
        }
    }

    private fun setupSliderFormatter() {
        binding.templateSizeSlider.setLabelFormatter {
            DecimalFormat("#.##").run {
                roundingMode = RoundingMode.CEILING
                format(it - MIN_TEMPLATE_SCALE)
            }
        }
    }

    private fun setupCurrencyButtons() {
        val currencies = resources.getStringArray(R.array.currencies)
        val defaultCurrency = settingsViewModel.getDefaultCurrency()
        val defaultIndex = currencies.indexOf(defaultCurrency)

        binding.rbGroupCurrency.children.forEachIndexed { index, view ->
            val button = view as MaterialButton

            if (index == defaultIndex) {
                button.isChecked = true
            }

            button.text = currencies[index]
        }
    }

    private fun setupAppVersion() {
        binding.appVersion.text = BuildConfig.VERSION_NAME
    }

    private fun showBottleTemplate() {
        binding.bottleTemplateDemo.setVisible(true)
        binding.bottleTemplateDemo.rotation =
            if (binding.toggleSkew.isChecked) TEMPLATE_ROTATION else 0f
    }

    override fun onPause() {
        super.onPause()

        val checkedButtonId = binding.rbGroupCurrency.checkedButtonId
        val checkedButton = binding.rbGroupCurrency.findViewById<Button>(checkedButtonId)
        val templateSize = binding.templateSizeSlider.value

        settingsViewModel.setDefaultCurrency(checkedButton.text.toString())
        settingsViewModel.setTemplateSize(templateSize)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
