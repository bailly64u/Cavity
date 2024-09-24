package com.louis.app.cavity.ui.manager.friend

import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.ItemFriendBinding
import com.louis.app.cavity.model.Friend

class FriendRecyclerAdapter(
    private val onRename: (Friend) -> Unit,
    private val onChangeImage: (Friend) -> Unit,
    private val onDelete: (Friend) -> Unit
) :
    ListAdapter<Friend, FriendRecyclerAdapter.FriendViewHolder>(
        FriendItemDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding =
            ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) =
        holder.bind(getItem(position))

    class FriendItemDiffCallback : DiffUtil.ItemCallback<Friend>() {
        override fun areItemsTheSame(oldItem: Friend, newItem: Friend) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Friend, newItem: Friend) = oldItem == newItem
    }

    inner class FriendViewHolder(private val binding: ItemFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context = binding.root.context

        init {
            binding.buttonOptions.setOnClickListener {
                showPopup(it)
            }
        }

        fun bind(friend: Friend) {
            with(binding) {
                friendName.text = friend.getChipText()

                if (friend.imgPath.isNotEmpty()) {
                    avatar.imageTintList = null
                    Glide.with(context)
                        .load(Uri.parse(friend.imgPath))
                        .centerCrop()
                        .into(avatar)
                }
            }

        }

        private fun showPopup(view: View) {
            val friend = getItem(bindingAdapterPosition)

            PopupMenu(context, view).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setForceShowIcon(true)

                menuInflater.inflate(R.menu.friend_menu, menu)

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.edit_item -> onRename(friend)
                        R.id.edit_image -> onChangeImage(friend)
                        R.id.delete_item -> onDelete(friend)
                    }
                    true
                }
                show()
            }
        }
    }
}
