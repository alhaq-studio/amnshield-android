package com.alhaq.amnshield.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.R
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.ui.activity.MainActivity
import com.alhaq.amnshield.ui.screens.ProfileScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.utils.GoogleSignInHelper

class ProfileFragment : Fragment() {

    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var viewModel: AmnShieldViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AmnShieldViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                AmnShieldTheme(appTheme = state.currentTheme) {
                    ProfileScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googleSignInHelper = GoogleSignInHelper(requireContext())
        loadProfileData()
    }

    private fun loadProfileData() {
        val account = googleSignInHelper.getLastSignedInAccount()
        val defaultName = account?.displayName ?: "Alhaq DST"
        val defaultEmail = account?.email ?: "alhaq.dst@gmail.com"
        
        val sharedPrefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val name = sharedPrefs.getString("profile_name", defaultName) ?: defaultName
        val email = sharedPrefs.getString("profile_email", defaultEmail) ?: defaultEmail
        val bio = sharedPrefs.getString("profile_bio", "Digital Wellbeing Guardian • Staying mindful & focused.") ?: ""
        val profileType = sharedPrefs.getString("profile_type", "Deep Focus") ?: "Deep Focus"
        
        viewModel.updateProfile(
            name = name,
            email = email,
            bio = bio,
            goalMinutes = viewModel.state.value.userGoalMinutes,
            profileType = profileType,
            pinEnabled = viewModel.state.value.isPinProtectionEnabled,
            pin = viewModel.state.value.profilePin
        )
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }
    
    fun refreshProfile() {
        if (isAdded) {
            loadProfileData()
        }
    }

    companion object {
        const val FRAGMENT_ID = "profile"
    }
}
