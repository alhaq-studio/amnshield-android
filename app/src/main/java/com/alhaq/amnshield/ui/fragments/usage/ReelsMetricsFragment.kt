package com.alhaq.amnshield.ui.fragments.usage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.activity.MainActivity
import com.alhaq.amnshield.ui.screens.ReelsMetricsScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.utils.ReelsStatsManager
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReelsMetricsFragment : Fragment() {

    companion object {
        const val FRAGMENT_ID = "ReelsMetricsFragment"
    }

    private lateinit var loader: SavedPreferencesLoader
    private lateinit var reelsStatsManager: ReelsStatsManager

    private val summaryState = MutableStateFlow(ReelsStatsManager.ReelsMetricsSummary())
    private val dailyLimitState = MutableStateFlow(200)
    private val isBlockerEnabledState = MutableStateFlow(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loader = SavedPreferencesLoader(requireContext())
        reelsStatsManager = ReelsStatsManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val activeTheme = com.alhaq.amnshield.utils.ThemeUtils.resolveAppTheme(requireContext())
                val summary by summaryState.collectAsState()
                val dailyLimit by dailyLimitState.collectAsState()
                val isEnabled by isBlockerEnabledState.collectAsState()

                AmnShieldTheme(appTheme = activeTheme) {
                    ReelsMetricsScreen(
                        summary = summary,
                        dailyLimit = dailyLimit,
                        isBlockerEnabled = isEnabled,
                        onBack = {
                            if (!parentFragmentManager.popBackStackImmediate()) {
                                requireActivity().finish()
                            }
                        },
                        onRefresh = { refreshMetrics() },
                        onConfigureRules = {
                            val intent = android.content.Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
                                putExtra("feature_type", "reel_blocker")
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshMetrics()
    }

    private fun refreshMetrics() {
        summaryState.value = reelsStatsManager.getFullMetricsSummary()
        dailyLimitState.value = loader.getReelBlockerDailyLimit()
        isBlockerEnabledState.value = loader.isReelBlockerEnabled()
    }
}
