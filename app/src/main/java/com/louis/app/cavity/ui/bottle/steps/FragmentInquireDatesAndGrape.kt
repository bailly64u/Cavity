package com.louis.app.cavity.ui.bottle.steps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentInquireDatesAndGrapeBinding
import com.louis.app.cavity.model.Grape
import com.louis.app.cavity.ui.bottle.AddBottleViewModel
import com.louis.app.cavity.ui.bottle.GrapeRecyclerAdapter

class FragmentInquireDatesAndGrape : Fragment(R.layout.fragment_inquire_dates_and_grape) {
    private lateinit var binding: FragmentInquireDatesAndGrapeBinding
    private lateinit var grapeAdapter: GrapeRecyclerAdapter
    private val addBottleViewModel : AddBottleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInquireDatesAndGrapeBinding.bind(view)

        initRecyclerView()
        setListener()
    }

    private fun initRecyclerView() {
        grapeAdapter = GrapeRecyclerAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = grapeAdapter
        }
    }

    private fun setListener() {
        binding.buttonAddGrape.setOnClickListener {
            grapeAdapter.addGrape(Grape(0, "Cabernet-sauvignon", 10, 0))
        }
    }
}