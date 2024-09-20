package com.louis.app.cavity.ui.addbottle.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Checkable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.ItemChipablePickBinding
import com.louis.app.cavity.databinding.ItemCountyManagerBinding
import com.louis.app.cavity.databinding.ItemGrapeBinding
import com.louis.app.cavity.model.Chipable
import com.louis.app.cavity.ui.addbottle.viewmodel.QGrapeUiModel
import com.louis.app.cavity.util.setVisible

class ChipableRecyclerAdapter(private val onSingleItemSelected: ((item: Chipable) -> Unit)?) :
    RecyclerView.Adapter<ChipableViewHolder>() {

    private val currentList = mutableListOf<CheckableChipable>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipableViewHolder {
        val binding =
            ItemChipablePickBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ChipableViewHolder(binding, onSingleItemSelected)
    }

    override fun onBindViewHolder(holder: ChipableViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    fun setList(chipables: List<Chipable>) {
        currentList.clear()
        currentList.addAll(chipables.map { CheckableChipable(it, checked = false) })
        notifyDataSetChanged()
    }

    fun getSelectedItems() = currentList.filter { it.checked }
}

class ChipableViewHolder(
    private val binding: ItemChipablePickBinding,
    private val onSingleItemSelected: ((item: Chipable) -> Unit)?
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(chipable: CheckableChipable) = with(binding) {
        chipable.chipable.getIcon()?.let {
            image.setImageDrawable(binding.root.resources.getDrawable(it))
        }

        val handleMultipleChoices = onSingleItemSelected == null
        checkbox.setVisible(handleMultipleChoices)
        text.text = chipable.chipable.getChipText()

        binding.root.setOnClickListener {
            onSingleItemSelected?.invoke(chipable.chipable) ?: checkbox.toggle()
        }
    }
}

data class CheckableChipable(val chipable: Chipable, val checked: Boolean)

