package com.louis.app.cavity.ui.addbottle

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentStepperBinding
import com.louis.app.cavity.ui.SnackbarProvider
import com.louis.app.cavity.ui.Stepper
import com.louis.app.cavity.ui.addbottle.viewmodel.*
import com.louis.app.cavity.util.showSnackbar

class FragmentAddBottle : Fragment(R.layout.fragment_stepper), Stepper {
    lateinit var snackbarProvider: SnackbarProvider
    private var _binding: FragmentStepperBinding? = null
    private val binding get() = _binding!!
    private val addBottleViewModel: AddBottleViewModel by viewModels()
    private val args: FragmentAddBottleArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStepperBinding.bind(view)

        snackbarProvider = activity as SnackbarProvider

        // editedBottleId is equal to 0 if user is not editing a bottle, but adding a new one
        addBottleViewModel.start(args.wineId, args.editedBottleId)

        initStepper()
        setupCustomBackNav()
        observe()
    }

    private fun initStepper() {
        binding.viewPager.apply {
            adapter = AddBottlesPagerAdapter(this@FragmentAddBottle)
            isUserInputEnabled = false
        }
    }

    private fun setupCustomBackNav() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.viewPager.currentItem != 0) {
                binding.viewPager.currentItem = binding.viewPager.currentItem - 1
            } else {
                remove()
                requireActivity().onBackPressed()
            }
        }
    }

    private fun observe() {
        addBottleViewModel.userFeedback.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { stringRes ->
                binding.coordinator.showSnackbar(stringRes)
            }
        }

        addBottleViewModel.completedEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { stringRes ->
                findNavController().popBackStack()
                // Using snackbar provider since we are quitting this fragment
                snackbarProvider.onShowSnackbarRequested(stringRes)
            }
        }
    }

    override fun requestNextPage() {
        binding.viewPager.currentItem++
    }

    override fun requestPreviousPage() {
        binding.viewPager.currentItem--
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
