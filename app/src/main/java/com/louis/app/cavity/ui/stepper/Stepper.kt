package com.louis.app.cavity.ui.stepper

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentStepperBinding
import com.louis.app.cavity.databinding.StepperBinding
import com.louis.app.cavity.util.setVisible

abstract class Stepper : Fragment(R.layout.fragment_stepper) {

    private lateinit var pagerAdapter: StepperPagerAdapter

    // Subclasses would be confusing to read
    @Suppress("PropertyName")
    protected var _binding: FragmentStepperBinding? = null
    val binding get() = _binding!!

    @Suppress("PropertyName")
    protected var _topBinding: StepperBinding? = null
    private val topBinding get() = _topBinding!!

    abstract val showStepperProgress: Boolean
    abstract val steps: List<() -> Step>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStepperBinding.bind(view)
        _topBinding = StepperBinding.bind(binding.root)

        init()
        setupCustomBackNav()
        setupStepper()
    }

    private fun init() {
        pagerAdapter = StepperPagerAdapter(this, steps)

        binding.viewPager.apply {
            adapter = pagerAdapter
            isUserInputEnabled = false
        }
    }

    private fun setupCustomBackNav() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.viewPager.currentItem != 0) {
                goToPreviousPage()
            } else {
                remove()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupStepper() {
        if (showStepperProgress) {
            with(topBinding) {
                previous.setOnClickListener { goToPreviousPage() }
                next.setOnClickListener { goToNextPage() }
            }
        } else {
            topBinding.next.setVisible(false)
            topBinding.previous.setVisible(false)
        }
    }

    private fun updateIcons(pagerPosition: Int) {
        val isLastPage = pagerPosition == steps.size - 1
        topBinding.next.isActivated = isLastPage

        val isFirstPage = pagerPosition == 0
        topBinding.previous.isEnabled = !isFirstPage
    }

    override fun onResume() {
        super.onResume()
        updateIcons(binding.viewPager.currentItem)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun goToNextPage(): Int {
        val currentPage = binding.viewPager.currentItem
        val ok = pagerAdapter.getFragmentAtPosition(currentPage)?.requestNextPage()

        return if (ok == true) {
            val nextPage = ++binding.viewPager.currentItem
            updateIcons(nextPage)

            nextPage
        } else {
            currentPage
        }
    }

    fun goToPreviousPage(): Int {
        val previousPage = --binding.viewPager.currentItem
        updateIcons(previousPage)

        return previousPage
    }
}
