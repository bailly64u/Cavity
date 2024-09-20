package com.louis.app.cavity.ui.addbottle

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.DialogChipablePickBinding
import com.louis.app.cavity.databinding.FragmentInquireOtherInfoBinding
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.BottleSize
import com.louis.app.cavity.model.Chipable
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.ui.ActivityMain
import com.louis.app.cavity.ui.ChipLoader
import com.louis.app.cavity.ui.LifecycleMaterialDialogBuilder
import com.louis.app.cavity.ui.SimpleInputDialog
import com.louis.app.cavity.ui.addbottle.adapter.ChipableRecyclerAdapter
import com.louis.app.cavity.ui.addbottle.viewmodel.AddBottleViewModel
import com.louis.app.cavity.ui.addbottle.viewmodel.OtherInfoManager
import com.louis.app.cavity.ui.home.HoneycombLayoutManager
import com.louis.app.cavity.ui.manager.AddItemViewModel
import com.louis.app.cavity.ui.manager.friend.FriendRecyclerAdapter
import com.louis.app.cavity.ui.stepper.Step
import com.louis.app.cavity.util.*

class FragmentInquireOtherInfo : Step(R.layout.fragment_inquire_other_info) {
    private lateinit var otherInfoManager: OtherInfoManager
    private lateinit var pickPdf: ActivityResultLauncher<Array<String>>
    private var _binding: FragmentInquireOtherInfoBinding? = null
    private val binding get() = _binding!!
    private val addItemViewModel: AddItemViewModel by activityViewModels()
    private val addBottleViewModel: AddBottleViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { pdfUri ->
            onPdfSelected(pdfUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInquireOtherInfoBinding.bind(view)

        otherInfoManager = addBottleViewModel.otherInfoManager

        binding.autoAnimate.layoutTransition.setAnimateParentHierarchy(false)
        binding.rbNormal.isChecked = true

        setListeners()
        observe()
        initFriendsChips()
    }

    private fun setListeners() {
        binding.buttonAddPdf.setOnClickListener {
            if (!otherInfoManager.hasPdf) {
                try {
                    pickPdf.launch(arrayOf("application/pdf"))
                } catch (e: ActivityNotFoundException) {
                    binding.coordinator.showSnackbar(R.string.no_file_explorer)
                }
            } else {
                onPdfRemoved()
            }
        }

        with(binding) {
            giftedBy.setOnCheckedChangeListener { _, isChecked ->
                buttonAddFriend.setVisible(isChecked)
                friendTitle.setVisible(isChecked)
                friend.root.setVisible(isChecked)

                if (isChecked) {
                    showPickFriendDialog()
                }
            }

            buttonAddFriend.setOnClickListener { showAddFriendDialog() }
        }

    }

    private fun observe() {
        addBottleViewModel.editedBottle.observe(viewLifecycleOwner) {
            if (it != null) updateFields(it)
        }

        addBottleViewModel.editedBottleHistoryEntry.observe(viewLifecycleOwner) { entry ->
            entry?.let {
                val isAGift = entry.friends.isNotEmpty()

                binding.giftedBy.isChecked = isAGift

                if (isAGift) {
                    val friend = entry.friends.first()

                    /*binding.friendChipGroup.doOnEachNextLayout {
                        it as ViewGroup
                        it.forEach { chip ->
                            val id = (chip.getTag(R.string.tag_chip_id) as Chipable).getItemId()
                            (chip as Chip).isChecked = friendId == id
                        }
                    }*/

                    bindFriend(friend)
                }
            }
        }
    }

    private fun bindFriend(friend: Chipable) {
        with(binding.friend) {
            AvatarLoader.requestAvatar(
                requireContext(),
                ((friend as Friend).imgPath)
            ) { avatarBitmap ->
                avatarBitmap?.let { drawable ->
                    avatar.setImageDrawable(drawable)
                }
            }

            friendName.text = friend.getChipText()
        }
    }

    private fun initFriendsChips() {

        /*otherInfoManager.getAllFriends().observe(viewLifecycleOwner) {
            ChipLoader.Builder()
                .with(lifecycleScope)
                .useInflater(layoutInflater)
                .toInflate(R.layout.chip_friend_entry)
                .load(it)
                .into(binding.friendChipGroup)
                .useAvatar(true)
                .emptyText(getString(R.string.placeholder_friend))
                .build()
                .go()
        }*/
    }

    private fun updateFields(editedBottle: Bottle) {
        with(binding) {
            otherInfo.setText(editedBottle.otherInfo)
            addToFavorite.isChecked = editedBottle.isFavorite.toBoolean()
            otherInfoManager.setPdfPath(editedBottle.pdfPath)

            val checkedButtonPos = when (editedBottle.bottleSize) {
                BottleSize.SLIM -> 0
                BottleSize.SMALL -> 1
                BottleSize.NORMAL -> 2
                BottleSize.MAGNUM -> 3
            }

            (binding.rbGroupSize.getChildAt(checkedButtonPos) as Checkable).isChecked = true

            if (otherInfoManager.hasPdf) {
                buttonAddPdf.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
                buttonAddPdf.text = resources.getString(R.string.remove_pdf)
            }
        }
    }

    private fun onPdfRemoved() {
        otherInfoManager.setPdfPath("")
        binding.buttonAddPdf.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pdf)
        binding.buttonAddPdf.text = resources.getString(R.string.add_pdf)
    }

    private fun onPdfSelected(pdfUri: Uri?) {
        if (pdfUri == null) {
            binding.coordinator.showSnackbar(R.string.base_error)
            return
        }

        (activity as ActivityMain).requestMediaPersistentPermission(pdfUri)

        otherInfoManager.setPdfPath(pdfUri.toString())
        binding.buttonAddPdf.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
        binding.buttonAddPdf.text = getString(R.string.remove_pdf)
    }

    private fun showPickFriendDialog() {
        val dialogBinding = DialogChipablePickBinding.inflate(layoutInflater)
        val dialog = LifecycleMaterialDialogBuilder(requireContext(), viewLifecycleOwner)
            .setTitle(R.string.gifted_by_friend)
            .setView(dialogBinding.root)

        var actual : AlertDialog? = null

        with(dialogBinding.friendList) {
            val adapter = ChipableRecyclerAdapter { chipable ->
                bindFriend(chipable)
                actual?.dismiss()
            }

            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter

            otherInfoManager.getAllFriends().observe(viewLifecycleOwner) {
                adapter.setList(it)
                actual = dialog.show()
            }

        }
    }

    private fun showAddFriendDialog() {
        val dialogResources = SimpleInputDialog.DialogContent(
            title = R.string.add_friend,
            hint = R.string.add_friend_label,
            icon = R.drawable.ic_person,
        ) {
            addItemViewModel.insertFriend(it)
        }

        SimpleInputDialog(requireContext(), layoutInflater, viewLifecycleOwner)
            .show(dialogResources)
    }

    override fun requestNextPage(): Boolean {
        return true
        /*with(binding) {
            friendChipGroup.apply {
                val friend = if (giftedBy.isChecked) collectAsSingle<Friend>()?.id else null

                otherInfoManager.submitOtherInfo(
                    otherInfo.text.toString(),
                    rbGroupSize.checkedButtonId,
                    addToFavorite.isChecked,
                    friend
                )

                addBottleViewModel.insertBottle()
            }
        }

        return true*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
