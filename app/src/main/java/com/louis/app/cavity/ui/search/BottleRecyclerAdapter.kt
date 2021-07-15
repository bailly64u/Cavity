package com.louis.app.cavity.ui.search

import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.louis.app.cavity.databinding.ItemBottleBinding
import com.louis.app.cavity.db.dao.BoundedBottle
import com.louis.app.cavity.util.setVisible
import com.louis.app.cavity.util.toBoolean

class BottleRecyclerAdapter(
    private val pickMode: Boolean,
    private val onClickListener: (Long, Long) -> Unit
) :
    ListAdapter<BoundedBottle, BottleRecyclerAdapter.BottleViewHolder>(BottleItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottleViewHolder {
        val binding = ItemBottleBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return BottleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int) = currentList[position].bottle.id

    class BottleItemDiffCallback : DiffUtil.ItemCallback<BoundedBottle>() {
        override fun areItemsTheSame(oldItem: BoundedBottle, newItem: BoundedBottle) =
            oldItem.bottle.id == newItem.bottle.id

        override fun areContentsTheSame(oldItem: BoundedBottle, newItem: BoundedBottle) =
            oldItem == newItem
    }

    inner class BottleViewHolder(private val binding: ItemBottleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(boundedBottle: BoundedBottle) {
            val (bottle, wine) = boundedBottle
            val wineColor = ContextCompat.getColor(itemView.context, wine.color.colorRes)

            binding.checkedIcon.setVisible(bottle.isSelected)

            with(binding.wineColorNameNaming) {
                wineName.text = wine.name
                wineNaming.text = wine.naming
                organicImage.setVisible(wine.isOrganic.toBoolean())
                wineColorIndicator.setColorFilter(wineColor)
            }

            binding.root.setOnClickListener {
                if (pickMode) {
                    bottle.isSelected = !bottle.isSelected
                    TransitionManager.beginDelayedTransition(it as ViewGroup)
                    binding.checkedIcon.setVisible(bottle.isSelected)
                } else {
                    onClickListener(wine.id, bottle.id)
                }
            }

            binding.vintage.text = bottle.vintage.toString()
        }
    }
}
