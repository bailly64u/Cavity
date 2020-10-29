package com.louis.app.cavity.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class FragmentHome : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupScrollableTab()
        setListeners()
        observe()
    }

    private fun setupScrollableTab() {
        binding.tab.addOnLongClickListener {
            // show dialog info for county
        }

        homeViewModel.getAllCounties().observe(viewLifecycleOwner) {
            lifecycleScope.launch(Main) {
                with(binding) {
                    tab.addTabs(it)
                    viewPager.adapter = WinesPagerAdapter(this@FragmentHome, it)
                    viewPager.offscreenPageLimit = 1 // was 5
                    tab.setUpWithViewPager(viewPager)
                }
            }
        }
    }

    private fun setListeners() {
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.homeToAddWine)
        }
    }

    private fun observe() {
        homeViewModel.isScrollingToTop.observe(viewLifecycleOwner) {
            with(binding) {
                if (it) fab.run { if (!isShown) show() }
                else fab.run { if (isShown) hide() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
