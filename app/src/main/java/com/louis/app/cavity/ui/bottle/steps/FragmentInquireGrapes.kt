package com.louis.app.cavity.ui.bottle.steps

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentInquireGrapesBinding
import com.louis.app.cavity.model.Grape
import com.louis.app.cavity.ui.bottle.AddBottleViewModel
import com.louis.app.cavity.ui.bottle.stepper.FragmentStepper
import com.louis.app.cavity.util.showSnackbar

class FragmentInquireGrapes : Fragment(R.layout.fragment_inquire_grapes) {
    private lateinit var binding: FragmentInquireGrapesBinding
    private lateinit var grapeAdapter: GrapeRecyclerAdapter
    private var totalGrapePercentage: Int? = null
    private val addBottleViewModel: AddBottleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInquireGrapesBinding.bind(view)

        registerStepperWatcher()
        initRecyclerView()
        observe()
        setListeners()
    }

    private fun registerStepperWatcher() {
        val stepperFragment =
            parentFragmentManager.findFragmentById(R.id.stepper) as FragmentStepper

        stepperFragment.addListener(object : FragmentStepper.StepperWatcher {
            override fun onRequestChangePage() = validateGrapes()
        })

    }

    private fun initRecyclerView() {
        grapeAdapter = GrapeRecyclerAdapter(
            { addBottleViewModel.removeGrape(it) },
            { addBottleViewModel.updateGrape(it) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = grapeAdapter
        }

        addBottleViewModel.getAllGrapes().observe(viewLifecycleOwner) {
            totalGrapePercentage = it.map { grape -> grape.percentage }.sum()
            grapeAdapter.submitList(it)
        }
    }

    private fun observe() {
        addBottleViewModel.userFeedback.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { stringRes ->
                binding.coordinator.showSnackbar(stringRes)
            }
        }
    }

    private fun setListeners() {
        binding.grapeName.setOnEditorActionListener { textView, i, _ ->
            val query = textView.text.toString()

            if (i == EditorInfo.IME_ACTION_DONE && query.isNotEmpty()) {
                val grapeName = binding.grapeName.text.toString()
                addGrape(grapeName)
                textView.text = ""
            }

            true
        }

        binding.buttonAddGrape.setOnClickListener {
            val grapeName = binding.grapeName.text.toString()
            addGrape(grapeName)
            binding.grapeName.setText("")
        }
    }

    private fun addGrape(grapeName: String) {
        if (grapeName.isEmpty()) {
            binding.coordinator.showSnackbar(R.string.empty_grape_name)
            return
        }

        if (grapeName == resources.getString(R.string.grape_other)) {
            binding.coordinator.showSnackbar(R.string.reserved_name)
            return
        }

        if (grapeAdapter.currentList.map { it.name }.any { it == grapeName }) {
            binding.coordinator.showSnackbar(R.string.grape_already_exist)
            return
        }

        val defaultPercentage = if (grapeAdapter.currentList.size >= 1) 0 else 25
        addBottleViewModel.addGrape(Grape(0, grapeName, defaultPercentage, 0))
    }

    private fun validateGrapes(): Boolean {
        return if (grapeAdapter.currentList.any { it.percentage == 0 }) {
            binding.coordinator.showSnackbar(R.string.empty_grape_percent)
            false
        } else {
            true
        }
    }
}
