package com.louis.app.cavity.ui.tasting

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentTastingOverviewBinding
import com.louis.app.cavity.ui.tasting.notifications.TastingNotifier
import com.louis.app.cavity.util.setupNavigation
import com.louis.app.cavity.util.showSnackbar

class FragmentTastingOverview : Fragment(R.layout.fragment_tasting_overview) {
    private var _binding: FragmentTastingOverviewBinding? = null
    private val binding get() = _binding!!
    private val tastingOverviewViewModel: TastingOverviewViewModel by viewModels()
    private val args: FragmentTastingOverviewArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTastingOverviewBinding.bind(view)

        setupNavigation(binding.appBar.toolbar)
        tastingOverviewViewModel.start(args.tastingId)

        initRecyclerView()
        observe()
        setListeners()
    }

    private fun initRecyclerView() {
        val tastingOverviewAdapter = BottleActionAdapter(
            onActionCheckedChange = { tastingAction, isChecked ->
                if (isChecked) {
                    TastingNotifier.cancelNotification(requireContext(), tastingAction.id.toInt())
                }

                tastingOverviewViewModel.setActionIsChecked(tastingAction, isChecked)
            },
            onCloseIconClicked = { bottle ->
                tastingOverviewViewModel.updateBottleTasting(bottle, tastingId = null)
                binding.coordinator.showSnackbar(
                    stringRes = R.string.bottle_removed_from_tasting,
                    actionStringRes = R.string.cancel,
                    action = {
                        tastingOverviewViewModel.updateBottleTasting(
                            bottle,
                            tastingId = args.tastingId
                        )
                    }
                )
            }
        )

        binding.bottleTastingActionsList.apply {
            adapter = tastingOverviewAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        tastingOverviewViewModel.bottles.observe(viewLifecycleOwner) {
            tastingOverviewAdapter.submitList(it)
        }
    }

    private fun observe() {
        tastingOverviewViewModel.tastingConfirmed.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun setListeners() {
        binding.buttonSubmit.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_tasting)
                .setMessage(R.string.confirm_tasting_explanation)
                .setPositiveButton(R.string.ok) { _, _ ->
                    tastingOverviewViewModel.confirmTasting()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}