package com.louis.app.cavity.ui.addbottle

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.DialogAddReviewBinding
import com.louis.app.cavity.databinding.FragmentInquireReviewBinding
import com.louis.app.cavity.ui.LifecycleMaterialDialogBuilder
import com.louis.app.cavity.ui.addbottle.adapter.FilledReviewRecyclerAdapter
import com.louis.app.cavity.ui.addbottle.viewmodel.AddBottleViewModel
import com.louis.app.cavity.ui.addbottle.viewmodel.ReviewManager
import com.louis.app.cavity.ui.stepper.Step
import com.louis.app.cavity.util.ColorUtil
import com.louis.app.cavity.util.hideKeyboard
import com.louis.app.cavity.util.prepareWindowInsets
import com.louis.app.cavity.util.setVisible
import com.louis.app.cavity.util.showKeyboard

class FragmentInquireReviews : Step(R.layout.fragment_inquire_review) {
    private lateinit var reviewManager: ReviewManager
    private var _binding: FragmentInquireReviewBinding? = null
    private val binding get() = _binding!!
    private val addBottleViewModel: AddBottleViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInquireReviewBinding.bind(view)

        reviewManager = addBottleViewModel.reviewManager

        applyInsets()
        initRecyclerView()
        observe()
        setListeners()
    }

    private fun applyInsets() {
        binding.reviewList.prepareWindowInsets { view, _, _, _, _, bottom ->
            view.updatePadding(bottom = bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initRecyclerView() {
        val reviewAdapter = FilledReviewRecyclerAdapter(
            ColorUtil(requireContext()),
            onValueChangedListener = { fReview, value ->
                reviewManager.updateFilledReview(fReview, value)
            },
            onDeleteListener = {
                reviewManager.removeFilledReview(it)
            }
        )

        binding.reviewList.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = reviewAdapter
        }

        reviewManager.fReviews.observe(viewLifecycleOwner) {
            binding.emptyState.setVisible((it.isEmpty()))
            reviewAdapter.submitList(it.toMutableList())
        }
    }

    private fun observe() {
        reviewManager.reviewDialogEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { checkableReviews ->
                val copy = checkableReviews.map { it.copy() }.toMutableList()
                val names = checkableReviews.map { it.name }.toTypedArray()
                val bool = checkableReviews.map { it.isChecked }.toBooleanArray()

                LifecycleMaterialDialogBuilder(requireContext(), viewLifecycleOwner)
                    .setTitle(R.string.select_reviews)
                    .setMultiChoiceItems(names, bool) { _, pos, checked ->
                        copy[pos].isChecked = checked
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                    }
                    .setPositiveButton(R.string.submit) { _, _ ->
                        reviewManager.submitCheckedReviews(copy)
                    }
                    .show()
            }
        }
    }

    private fun setListeners() {
        with(binding) {
            buttonAddReview.setOnClickListener { showAddReviewDialog() }
            buttonSelectReview.setOnClickListener { reviewManager.requestReviewDialog() }
            emptyState.setOnActionClickListener { reviewManager.requestReviewDialog() }
            emptyState.setOnSecondaryActionClickListener { stepperFragment?.goToNextPage() }
        }
    }

    private fun showAddReviewDialog() {
        val dialogBinding = DialogAddReviewBinding.inflate(layoutInflater)

        LifecycleMaterialDialogBuilder(requireContext(), viewLifecycleOwner)
            .setTitle(R.string.add_review)
            .setNegativeButton(R.string.cancel) { _, _ ->
            }
            .setPositiveButton(R.string.submit) { _, _ ->
                val name = dialogBinding.contestName.text.toString().trim()
                val type = getReviewType(dialogBinding.rbGroupType.checkedButtonId)

                reviewManager.addReviewAndFReview(name, type)
            }
            .setView(dialogBinding.root)
            .setOnDismissListener { dialogBinding.root.hideKeyboard() }
            .show()

        dialogBinding.contestName.post { dialogBinding.contestName.showKeyboard() }
        dialogBinding.rbMedal.performClick()
    }

    private fun getReviewType(@IdRes button: Int) = when (button) {
        R.id.rbMedal -> 0
        R.id.rbRate20 -> 1
        R.id.rbRate100 -> 2
        else -> 3
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
