package com.louis.app.cavity.ui.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import com.louis.app.cavity.R
import com.louis.app.cavity.databinding.FragmentLoginBinding
import com.louis.app.cavity.util.setVisible
import com.louis.app.cavity.util.setupNavigation

class FragmentLogin : Fragment(R.layout.fragment_login) {
    companion object {
        const val LOGIN_SUCCESSFUL: String = "com.louis.app.cavity.LOGIN_SUCCESSFUL"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginViewModel: LoginViewModel by activityViewModels()
    private lateinit var savedStateHandle: SavedStateHandle


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginBinding.bind(view)
        savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        savedStateHandle.set(LOGIN_SUCCESSFUL, false)

        setupNavigation(binding.appBar.toolbar)

        observe()
        setListeners()
    }

    private fun observe() {
        loginViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.setVisible(it, invisible = true)
        }

        loginViewModel.navigateToConfirm.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let {
                val action = FragmentLoginDirections.loginToConfirm()
                findNavController().navigate(action)
            }
        }

        loginViewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                savedStateHandle.set(LOGIN_SUCCESSFUL, true)
                findNavController().popBackStack()
            }
        }
    }

    private fun setListeners() {
        binding.buttonSubmit.setOnClickListener {
            with(binding) {
                val email = login.text.toString()
                val password = password.text.toString()
                val apiUrl = "http://${ip.text.toString()}"
                loginViewModel.setApiUrl(apiUrl)

                when (newAccount.isChecked) {
                    true -> loginViewModel.register(email, password)
                    else -> loginViewModel.login(email, password)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
