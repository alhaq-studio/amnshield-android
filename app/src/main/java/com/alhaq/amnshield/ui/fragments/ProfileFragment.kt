package com.alhaq.amnshield.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentProfileBinding
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.utils.GoogleSignInHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = googleSignInHelper.handleSignInResult(result.data)
            if (account != null) {
                Toast.makeText(requireContext(), getString(R.string.signed_in_as, account.email), Toast.LENGTH_SHORT).show()
                loadProfileData()
                
                // Notify MainActivity to update drawer
                activity?.let { act ->
                    if (act is com.alhaq.amnshield.ui.activity.MainActivity) {
                        // MainActivity will handle updating the drawer header
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googleSignInHelper = GoogleSignInHelper(requireContext())
        
        loadProfileData()
        setupClickListeners()
    }

    private fun loadProfileData() {
        val account = googleSignInHelper.getLastSignedInAccount()
        
        if (account != null) {
            // Signed in - show sign out button
            binding.cardSignIn.visibility = View.GONE
            binding.cardSignOut.visibility = View.VISIBLE
            
            // Display user info
            binding.profileName.text = account.displayName ?: account.email?.split("@")?.get(0) ?: getString(R.string.guest_user)
            binding.profileEmail.text = account.email ?: getString(R.string.not_signed_in)
            
            // Load profile photo if available
            account.photoUrl?.let { photoUri ->
                // Could use Glide or Coil here to load image
                // For now, keeping default icon
            }
            
            // Show member since date
            val sharedPrefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val firstDate = sharedPrefs.getString("first_date", null)
            if (firstDate != null) {
                val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(firstDate)
                binding.memberSince.text = date?.let { dateFormat.format(it) } ?: getString(R.string.unknown)
            } else {
                binding.memberSince.text = getString(R.string.unknown)
            }
        } else {
            // Not signed in - show sign in button
            binding.cardSignIn.visibility = View.VISIBLE
            binding.cardSignOut.visibility = View.GONE
            
            // Display guest info
            binding.profileName.text = getString(R.string.guest_user)
            binding.profileEmail.text = getString(R.string.not_signed_in)
            binding.memberSince.text = getString(R.string.unknown)
        }
        
        // Update premium status
        updatePremiumStatus()
    }

    private fun updatePremiumStatus() {
        val userType = premiumManager.getUserType()
        
        when (userType) {
            PremiumManager.UserType.SPECIAL -> {
                binding.premiumBadge.visibility = View.VISIBLE
                binding.premiumStatusTitle.text = getString(R.string.special_access_active)
                binding.premiumStatusDescription.text = getString(R.string.special_access_active_description)
                binding.accountType.text = getString(R.string.special_user)
            }
            PremiumManager.UserType.COMPASSIONATE -> {
                binding.premiumBadge.visibility = View.VISIBLE
                binding.premiumStatusTitle.text = getString(R.string.compassionate_access_active)
                binding.premiumStatusDescription.text = getString(R.string.compassionate_access_active_description)
                binding.accountType.text = getString(R.string.compassionate_access_account_type)
            }
            PremiumManager.UserType.PREMIUM -> {
                binding.premiumBadge.visibility = View.VISIBLE
                binding.premiumStatusTitle.text = getString(R.string.premium_active)
                binding.premiumStatusDescription.text = getString(R.string.premium_active_description)
                binding.accountType.text = getString(R.string.premium)
            }
            PremiumManager.UserType.FREE -> {
                binding.premiumBadge.visibility = View.GONE
                binding.premiumStatusTitle.text = getString(R.string.upgrade_to_premium)
                binding.premiumStatusDescription.text = getString(R.string.premium_upgrade_description)
                binding.accountType.text = getString(R.string.free)
            }
        }
    }

    private fun setupClickListeners() {
        // Sign In
        binding.cardSignIn.setOnClickListener {
            val signInIntent = googleSignInHelper.getSignInIntent()
            googleSignInLauncher.launch(signInIntent)
        }
        
        // Sign Out
        binding.cardSignOut.setOnClickListener {
            showSignOutDialog()
        }
        
        // Premium Card - navigate to premium features or enter special ID
        binding.cardPremium.setOnClickListener {
            if (premiumManager.getUserType() == PremiumManager.UserType.FREE) {
                showPremiumOptionsDialog()
            } else {
                val intent = Intent(requireContext(), FragmentActivity::class.java)
                intent.putExtra("feature_type", "premium_features")
                startActivity(intent)
            }
        }
    }

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.sign_out_confirmation))
            .setPositiveButton(getString(R.string.sign_out)) { _, _ ->
                googleSignInHelper.signOut {
                    Toast.makeText(requireContext(), getString(R.string.signed_out_successfully), Toast.LENGTH_SHORT).show()
                    // Refresh UI
                    loadProfileData()
                    
                    // Notify MainActivity to update drawer
                    activity?.let { act ->
                        if (act is com.alhaq.amnshield.ui.activity.MainActivity) {
                            // MainActivity will handle updating the drawer header
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showPremiumOptionsDialog() {
        val options = arrayOf(
            getString(R.string.upgrade_to_premium),
            getString(R.string.profile_compassionate_access_option),
            getString(R.string.profile_special_access_option),
            getString(R.string.cancel)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_premium_access_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Navigate to premium features/purchase
                        val intent = Intent(requireContext(), FragmentActivity::class.java)
                        intent.putExtra("feature_type", "premium_features")
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(requireContext(), FragmentActivity::class.java)
                        intent.putExtra("feature_type", "premium_features")
                        startActivity(intent)
                    }
                    2 -> {
                        showSpecialAccessDialog()
                    }
                    // 3 is cancel, do nothing
                }
            }
            .show()
    }

    private fun showSpecialAccessDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.special_access_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.special_access_title)
            .setMessage(getString(R.string.special_access_message))
            .setView(input)
            .setPositiveButton(R.string.special_access_activate) { _, _ ->
                val accessId = input.text.toString()
                if (premiumManager.setSpecialAccessId(accessId)) {
                    Toast.makeText(
                        requireContext(),
                        R.string.special_access_success,
                        Toast.LENGTH_LONG
                    ).show()
                    loadProfileData()
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.special_access_invalid,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to profile
        loadProfileData()
    }
    
    /**
     * Public method to refresh profile data.
     * Called by MainActivity when sign-in completes.
     */
    fun refreshProfile() {
        if (isAdded && _binding != null) {
            loadProfileData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FRAGMENT_ID = "profile"
    }
}
