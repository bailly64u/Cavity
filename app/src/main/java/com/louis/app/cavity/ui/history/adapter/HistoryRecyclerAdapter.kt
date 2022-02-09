package com.louis.app.cavity.ui.history.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.ItemHistorySeparatorBinding
import com.louis.app.cavity.databinding.ItemHistoryUseBinding
import com.louis.app.cavity.ui.history.HistoryUiModel
import com.louis.app.cavity.util.ColorUtil
import com.louis.app.cavity.util.DateFormatter
import com.louis.app.cavity.util.setVisible

class HistoryRecyclerAdapter(
    private val context: Context,
    private val colorUtil: ColorUtil,
    private val onHeaderClick: () -> Unit,
    private val onItemClick: (HistoryUiModel.EntryModel) -> Unit,
    private val onSwiped: (HistoryUiModel.EntryModel) -> Unit,
) :
    PagingDataAdapter<HistoryUiModel, RecyclerView.ViewHolder>(HistoryEntryDiffItemCallback()) {

    companion object {
        const val TYPE_SEPARATOR = 0
        const val TYPE_NORMAL = 1
    }

    // Only lightweight drawables here
    private val drawables = mutableMapOf<Int, Drawable>()

    private fun getDrawable(@DrawableRes id: Int): Drawable? {
        return drawables[id] ?: ContextCompat.getDrawable(context, id)?.also { drawables[id] = it }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is HistoryEntryViewHolder -> holder.bind(item as HistoryUiModel.EntryModel?)
            is HistorySeparatorViewHolder -> holder.bind(item as HistoryUiModel.HeaderModel?)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_SEPARATOR -> HistorySeparatorViewHolder(
                ItemHistorySeparatorBinding.inflate(inflater, parent, false)
            )
            /* TYPE_NORMAL */ else -> HistoryEntryViewHolder(
                ItemHistoryUseBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryUiModel.HeaderModel -> TYPE_SEPARATOR
            is HistoryUiModel.EntryModel -> TYPE_NORMAL
            null -> TYPE_NORMAL
        }
    }

    class HistoryEntryDiffItemCallback : DiffUtil.ItemCallback<HistoryUiModel>() {
        override fun areItemsTheSame(oldItem: HistoryUiModel, newItem: HistoryUiModel): Boolean {
            val isSameEntry = oldItem is HistoryUiModel.EntryModel
                && newItem is HistoryUiModel.EntryModel
                && oldItem.model.historyEntry.id == newItem.model.historyEntry.id

            val isSameSeparator = oldItem is HistoryUiModel.HeaderModel
                && newItem is HistoryUiModel.HeaderModel
                && DateFormatter.roundToDay(oldItem.date) == DateFormatter.roundToDay(newItem.date)

            return isSameEntry || isSameSeparator
        }

        override fun areContentsTheSame(oldItem: HistoryUiModel, newItem: HistoryUiModel) =
            oldItem == newItem
    }

    inner class HistoryEntryViewHolder(private val binding: ItemHistoryUseBinding) :
        ReboundableViewHolder(binding) {

        fun bind(entry: HistoryUiModel.EntryModel?) {
            super.bind(entry ?: return)

            val (markerColor, icon, label, _, showFriends) = entry.model.historyEntry.getResources()
            val (bottle, wine) = entry.model.bottleAndWine
            val wineColor = ContextCompat.getColor(context, wine.color.colorRes)
            val colorCategory =
                if (entry.model.historyEntry.type == 4)
                    ColorUtil.ColorCategory.PRIMARY
                else
                    ColorUtil.ColorCategory.OTHER

            val resolvedMarkerColor = colorUtil.getColor(markerColor, colorCategory)

            with(binding) {
                wineColorNameNaming.wineColorIndicator.setColorFilter(wineColor)
                wineColorNameNaming.wineNaming.text = wine.naming
                wineColorNameNaming.wineName.text = wine.name
                vintage.text = bottle.vintage.toString()

                friends.setVisible(showFriends && entry.model.friends.isNotEmpty())
                friends.text = entry.model.friends.size.toString()

                marker.setBackgroundColor(resolvedMarkerColor)

                comment.apply {
                    when (entry.model.historyEntry.type) {
                        0 -> {
                            val comment = entry.model.historyEntry.comment

                            text = if (comment.isBlank()) {
                                setTextAppearance(R.style.TextAppearance_Cavity_Body2_Italic)
                                context.getString(R.string.no_description)
                            } else {
                                setTextAppearance(R.style.TextAppearance_Cavity_Body2)
                                comment
                            }
                        }
                        4 -> {
                            setTextAppearance(R.style.TextAppearance_Cavity_Body2)
                            text = entry.model.tastingWithBottles?.tasting?.opportunity
                        }
                        else -> {
                            setTextAppearance(R.style.TextAppearance_Cavity_Body2)

                            val data = if (entry.model.historyEntry.type == 1) {
                                entry.model.bottleAndWine.bottle.buyLocation
                            } else {
                                entry.model.friends.firstOrNull()?.name ?: ""
                            }

                            text = context.getString(label, data)
                        }
                    }

                    setCompoundDrawablesWithIntrinsicBounds(getDrawable(icon), null, null, null)
                }

                cardView.setOnClickListener {
                    onItemClick(entry)
                }
            }
        }

        override fun onRebounded(position: Int) {
            onSwiped(getItem(position) as HistoryUiModel.EntryModel)
        }
    }

    inner class HistorySeparatorViewHolder(private val binding: ItemHistorySeparatorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { onHeaderClick() }
        }

        fun bind(header: HistoryUiModel.HeaderModel?) {
            header?.let { binding.date.text = DateFormatter.formatDate(it.date) }
        }
    }
}
