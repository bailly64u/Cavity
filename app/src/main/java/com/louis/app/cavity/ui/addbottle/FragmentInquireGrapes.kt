package com.louis.app.cavity.ui.addbottle

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentInquireGrapesBinding
import com.louis.app.cavity.ui.LifecycleMaterialDialogBuilder
import com.louis.app.cavity.ui.SimpleInputDialog
import com.louis.app.cavity.ui.addbottle.adapter.QuantifiedGrapeRecyclerAdapter
import com.louis.app.cavity.ui.addbottle.viewmodel.AddBottleViewModel
import com.louis.app.cavity.ui.addbottle.viewmodel.GrapeManager
import com.louis.app.cavity.ui.stepper.Step
import com.louis.app.cavity.util.prepareWindowInsets
import com.louis.app.cavity.util.setVisible

class FragmentInquireGrapes : Step(R.layout.fragment_inquire_grapes) {
    private lateinit var grapeManager: GrapeManager
    private var _binding: FragmentInquireGrapesBinding? = null
    private val binding get() = _binding!!
    private val addBottleViewModel: AddBottleViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInquireGrapesBinding.bind(view)

        grapeManager = addBottleViewModel.grapeManager

        applyInsets()
        initRecyclerView()
        observe()
        setListeners()
    }

    private fun applyInsets() {
        binding.grapeList.prepareWindowInsets { view, _, _, _, _, bottom ->
            view.updatePadding(bottom = bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initRecyclerView() {
        val quantifiedGrapeAdapter = QuantifiedGrapeRecyclerAdapter(
            onDeleteListener = { grapeManager.removeQuantifiedGrape(it) },
            onValueChangeListener = { qGrape, newValue ->
                grapeManager.updateQuantifiedGrape(qGrape, newValue)
            },
        )

        binding.grapeList.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = quantifiedGrapeAdapter
        }

        grapeManager.qGrapes.observe(viewLifecycleOwner) {
            binding.emptyState.setVisible(it.isEmpty())
            quantifiedGrapeAdapter.submitList(it.toMutableList())
        }
    }

    private fun observe() {
        grapeManager.grapeDialogEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { checkableGrapes ->
                val copy = checkableGrapes.map { it.copy() }.toMutableList()
                val names = checkableGrapes.map { it.name }.toTypedArray()
                val bool = checkableGrapes.map { it.isChecked }.toBooleanArray()

                LifecycleMaterialDialogBuilder(requireContext(), viewLifecycleOwner)
                    .setTitle(R.string.select_grapes)
                    .setMultiChoiceItems(names, bool) { _, pos, checked ->
                        copy[pos].isChecked = checked
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                    }
                    .setPositiveButton(R.string.submit) { _, _ ->
                        grapeManager.submitCheckedGrapes(copy)
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun setListeners() {
        with(binding) {
            buttonSelectGrape.setOnClickListener { grapeManager.requestGrapeDialog() }
            emptyState.setOnActionClickListener { grapeManager.requestGrapeDialog() }
            emptyState.setOnSecondaryActionClickListener { stepperFragment?.goToNextPage() }
            buttonAddGrape.setOnClickListener { showAddGrapeDialog() }
        }
    }

    private fun showAddGrapeDialog() {
        val dialogResources = SimpleInputDialog.DialogContent(
            title = R.string.add_grape,
            hint = R.string.grape_name,
            icon = R.drawable.ic_grape
        ) {
            grapeManager.addGrapeAndQGrape(it)
        }

        SimpleInputDialog(requireContext(), layoutInflater, viewLifecycleOwner)
            .show(dialogResources)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
