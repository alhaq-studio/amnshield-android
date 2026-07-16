package com.alhaq.amnshield.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    // Activity result launcher for Google Sign-In flow
    private val googleSignInLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = googleSignInHelper.handleSignInResult(result.data)
            if (account != null) {
                // Update SharedPreferences profile info
                val sharedPrefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                sharedPrefs.edit().apply {
                    putString("profile_name", account.displayName)
                    putString("profile_email", account.email)
                    apply()
                }
                // Refresh ViewModel state
                loadProfileData()
                Toast.makeText(requireContext(), getString(R.string.signed_in_as, account.email), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AmnShieldViewModel::class.java]
        googleSignInHelper = GoogleSignInHelper(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                
                // Track Google sign-in status dynamically
                val isGoogleSignedIn = remember(state.userName, state.userEmail) {
                    googleSignInHelper.isSignedIn()
                }
                
                AmnShieldTheme(appTheme = state.currentTheme) {
                    ProfileScreen(
                        state = state,
                        viewModel = viewModel,
                        isGoogleSignedIn = isGoogleSignedIn,
                        onGoogleSignIn = { signInWithGoogle() },
                        onGoogleSignOut = { signOutFromGoogle() },
                        onBack = {
                            if (!parentFragmentManager.popBackStackImmediate()) {
                                requireActivity().finish()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfileData()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInHelper.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signOutFromGoogle() {
        googleSignInHelper.signOut {
            val sharedPrefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            sharedPrefs.edit().apply {
                remove("profile_name")
                remove("profile_email")
                apply()
            }
            loadProfileData()
            Toast.makeText(requireContext(), getString(R.string.signed_out_successfully), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileData() {
        val account = googleSignInHelper.getLastSignedInAccount()
        val defaultName = account?.displayName ?: ""
        val defaultEmail = account?.email ?: ""
        
        val sharedPrefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val name = sharedPrefs.getString("profile_name", defaultName) ?: defaultName
        val email = sharedPrefs.getString("profile_email", defaultEmail) ?: defaultEmail
        val bio = sharedPrefs.getString("profile_bio", "") ?: ""
        val profileType = sharedPrefs.getString("profile_type", "Deep Focus") ?: "Deep Focus"
        
        val enforcementPrefs = requireContext().getSharedPreferences("enforcement_prefs", Context.MODE_PRIVATE)
        val isAdvanced = enforcementPrefs.getString("enforcement_mode", "SIMPLE") == "ADVANCED"
        
        viewModel.updateProfile(
            name = name,
            email = email,
            bio = bio,
            goalMinutes = viewModel.state.value.userGoalMinutes,
            profileType = profileType,
            pinEnabled = viewModel.state.value.isPinProtectionEnabled,
            pin = viewModel.state.value.profilePin
        )

        // Ensure ViewModel state has the correct premium status and advanced mode as well
        val isPremium = com.alhaq.amnshield.premium.PremiumManager.getInstance(requireContext().applicationContext).isPremium()
        if (viewModel.state.value.isPremiumUser != isPremium || viewModel.state.value.isAdvancedMode != isAdvanced) {
            viewModel.loadState(viewModel.state.value.copy(isPremiumUser = isPremium, isAdvancedMode = isAdvanced))
        }
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
