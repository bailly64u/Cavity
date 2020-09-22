package com.louis.app.cavity.ui.bottle

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentAddBottleBinding
import com.louis.app.cavity.ui.bottle.stepper.AddBottlesPagerAdapter
import com.louis.app.cavity.ui.bottle.stepper.FragmentStepper
import com.louis.app.cavity.ui.home.WineOptionsBottomSheet.Companion.ARG_WINE_ID

class FragmentAddBottle : Fragment(R.layout.fragment_add_bottle) {
    private lateinit var binding: FragmentAddBottleBinding
    private val addBottleViewModel: AddBottleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddBottleBinding.bind(view)

        arguments?.getLong(ARG_WINE_ID)?.let { addBottleViewModel.setWineId(it) }
        val editWineId = arguments?.getLong("ARG_EDIT_BOTTLE_ID", -1) ?: -1

        if (editWineId != -1L) addBottleViewModel.triggerEditMode(editWineId)

        val stepperFragment = childFragmentManager.findFragmentById(R.id.stepper) as FragmentStepper

        binding.viewPager
            .apply { adapter = AddBottlesPagerAdapter(this@FragmentAddBottle) }
            .also { stepperFragment.setupWithViewPager(it) }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.viewPager.currentItem != 0) {
                binding.viewPager.currentItem = binding.viewPager.currentItem - 1
            } else {
                remove()
                requireActivity().onBackPressed()
            }
        }
    }
}
