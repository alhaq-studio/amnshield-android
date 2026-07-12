package com.alhaq.deenshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.alhaq.deenshield.R
import com.alhaq.deenshield.data.blockers.AppBlockScheduleRule
import com.alhaq.deenshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.deenshield.databinding.FragmentManageBlockSchedulesBinding
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.data.blockers.AppLaunchLimitRule
import com.alhaq.deenshield.ui.adapters.BlockScheduleAdapter
import com.alhaq.deenshield.ui.adapters.LaunchLimitAdapter
import com.alhaq.deenshield.ui.dialogs.SetLaunchLimitDialog
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.TimeTools
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar
import java.util.UUID

class ManageBlockSchedulesFragment : Fragment() {

    private enum class EditableScopeType {
        APP_SINGLE,
        APP_GROUP,
        FEATURE_SINGLE,
        FEATURE_GROUP
    }

    private enum class RecurrenceOption {
        HOURLY,
        DAILY,
        WEEKLY,
        ALWAYS
    }

    private data class RecurrenceSelection(
        val option: RecurrenceOption,
        val startMinute: Int = 0,
        val endMinute: Int = 0,
        val selectedDays: Set<Int> = emptySet(),
        val durationHours: Int = 0,
        val activeUntilMillis: Long = 0L
    )

    private data class EditableScope(
        val type: EditableScopeType,
        val id: String,
        val title: String
    )

    private data class ScopeStats(
        val itemCount: Int,
        val memberCount: Int,
        val summary: String
    )

    private var _binding: FragmentManageBlockSchedulesBinding? = null
    private val binding get() = _binding!!
    private lateinit var savedPreferencesLoader: SavedPreferencesLoader
    private lateinit var scheduleAdapter: BlockScheduleAdapter
    private var launchLimitAdapter: LaunchLimitAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBlockSchedulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!PremiumManager.getInstance(requireContext().applicationContext).isPremium()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Premium Required")
                .setMessage("Schedules and launch limits are available for premium users.")
                .setPositiveButton("View Plans") { _, _ ->
                    val intent = Intent(requireContext(), com.alhaq.deenshield.ui.activity.FragmentActivity::class.java)
                    intent.putExtra("feature_type", "premium_features")
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Close") { _, _ -> requireActivity().finish() }
                .setCancelable(false)
                .show()
            return
        }
        
        savedPreferencesLoader = SavedPreferencesLoader(requireContext())
        
        // Setup Block Schedules RecyclerView
        scheduleAdapter = BlockScheduleAdapter(
            onEdit = { rule -> editSchedule(rule) },
            onDelete = { rule -> deleteSchedule(rule) }
        )
        binding.schedulesList.layoutManager = LinearLayoutManager(requireContext())
        binding.schedulesList.adapter = scheduleAdapter

        // Setup Launch Limits RecyclerView
        launchLimitAdapter = LaunchLimitAdapter(
            context = requireContext(),
            onEdit = { rule -> editLaunchLimit(rule) },
            onDelete = { rule -> deleteLaunchLimit(rule) }
        )
        binding.launchLimitsList.layoutManager = LinearLayoutManager(requireContext())
        binding.launchLimitsList.adapter = launchLimitAdapter

        binding.btnBackArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddUnifiedSchedule.setOnClickListener {
            showUnifiedScheduleEntryDialog()
        }

        loadSchedulesAndLimits()
    }

    override fun onResume() {
        super.onResume()
        loadSchedulesAndLimits()
    }

    private fun loadSchedulesAndLimits() {
        // Load block schedules (app + unified feature schedules)
        val appSchedules = savedPreferencesLoader.loadAppBlockerScheduleRules()
        val featureSchedules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules()
        val schedules = buildDisplaySchedules(appSchedules, featureSchedules)
        if (schedules.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.schedulesList.visibility = View.GONE
            binding.emptyStateText.text = "No block schedules yet.\n\nTap 'Add Rule' to create a scheduled block window."
        } else {
            binding.emptyState.visibility = View.GONE
            binding.schedulesList.visibility = View.VISIBLE
            scheduleAdapter.submitList(schedules)
        }

        // Load launch limits
        val launchLimits = savedPreferencesLoader.loadAppLaunchLimitRules()
        if (launchLimits.isEmpty()) {
            binding.launchLimitsEmptyState.visibility = View.VISIBLE
            binding.launchLimitsList.visibility = View.GONE
            launchLimitAdapter?.submitList(emptyList())
        } else {
            binding.launchLimitsEmptyState.visibility = View.GONE
            binding.launchLimitsList.visibility = View.VISIBLE
            launchLimitAdapter?.submitList(launchLimits)
        }

        // Update summary stats header
        binding.txtScheduleCount.text = schedules.size.toString()
        binding.txtLimitsCount.text = launchLimits.size.toString()
        val totalRules = schedules.size + launchLimits.size
        binding.txtScheduleSubtitle.text = if (totalRules > 0)
            "$totalRules active rules protecting you"
        else
            "Schedules & Launch Limits"
    }

    private fun showUnifiedScheduleEntryDialog() {
        val options = arrayOf(
            "Schedule multiple apps",
            "Schedule blocker features"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create unified schedule")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showUnifiedScheduleScopeDialog()
                    1 -> showUnifiedFeatureTargetDialog()
                }
            }
            .show()
    }

    private fun editSchedule(rule: AppBlockScheduleRule) {
        val scope = resolveEditableScope(rule) ?: run {
            Toast.makeText(requireContext(), "Schedule not found", Toast.LENGTH_SHORT).show()
            return
        }

        val stats = resolveScopeStats(scope)

        val isGroup = scope.type == EditableScopeType.APP_GROUP || scope.type == EditableScopeType.FEATURE_GROUP
        val isFeature = scope.type == EditableScopeType.FEATURE_SINGLE || scope.type == EditableScopeType.FEATURE_GROUP
        val dialogTitle = when (scope.type) {
            EditableScopeType.APP_SINGLE -> "Manage App Schedule"
            EditableScopeType.APP_GROUP -> "Manage App Group (${stats.itemCount} rules)"
            EditableScopeType.FEATURE_SINGLE -> "Manage Feature Schedule"
            EditableScopeType.FEATURE_GROUP -> "Manage Feature Group (${stats.itemCount} rules)"
        }

        val options = if (isGroup) {
            arrayOf(
                "View group members",
                "Change type (Block/Cheat)",
                "Change recurrence/time",
                "Duplicate group (${stats.itemCount} rules)",
                "Delete entire group (${stats.itemCount} rules)"
            )
        } else {
            arrayOf(
                "Change type (Block/Cheat)",
                "Change recurrence/time",
                "Duplicate",
                "Delete schedule"
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setMessage("${scope.title}\n\n${stats.summary}")
            .setItems(options) { _, which ->
                if (isGroup) {
                    when (which) {
                        0 -> showGroupMembersDialog(scope)
                        1 -> showChangeTypeDialog(scope)
                        2 -> showRecurrenceChangeDialog(scope)
                        3 -> duplicateScope(scope)
                        4 -> deleteSchedule(rule)
                    }
                } else {
                    when (which) {
                        0 -> showChangeTypeDialog(scope)
                        1 -> showRecurrenceChangeDialog(scope)
                        2 -> duplicateScope(scope)
                        3 -> deleteSchedule(rule)
                    }
                }
            }
            .show()
    }

    private fun resolveScopeStats(scope: EditableScope): ScopeStats {
        return when (scope.type) {
            EditableScopeType.APP_SINGLE -> ScopeStats(
                itemCount = 1,
                memberCount = 1,
                summary = "Single app schedule"
            )

            EditableScopeType.FEATURE_SINGLE -> ScopeStats(
                itemCount = 1,
                memberCount = 1,
                summary = "Single feature schedule"
            )

            EditableScopeType.APP_GROUP -> {
                val rules = savedPreferencesLoader.loadAppBlockerScheduleRules().filter { it.groupId == scope.id }
                val apps = rules.map { it.packageName }.toSet().size
                ScopeStats(
                    itemCount = rules.size,
                    memberCount = apps,
                    summary = "Group schedule • ${rules.size} rules • $apps apps"
                )
            }

            EditableScopeType.FEATURE_GROUP -> {
                val rules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().filter { it.groupId == scope.id }
                val features = rules.flatMap { it.targets }.toSet().size
                ScopeStats(
                    itemCount = rules.size,
                    memberCount = features,
                    summary = "Feature group • ${rules.size} rules • $features features"
                )
            }
        }
    }

    private fun showGroupMembersDialog(scope: EditableScope) {
        when (scope.type) {
            EditableScopeType.APP_GROUP -> {
                val rules = savedPreferencesLoader.loadAppBlockerScheduleRules().filter { it.groupId == scope.id }
                if (rules.isEmpty()) {
                    Toast.makeText(requireContext(), "No group members found", Toast.LENGTH_SHORT).show()
                    return
                }
                val appNames = rules
                    .map { it.packageName }
                    .toSet()
                    .map { packageName ->
                        try {
                            requireContext().packageManager.getApplicationLabel(
                                requireContext().packageManager.getApplicationInfo(packageName, 0)
                            ).toString()
                        } catch (_: Exception) {
                            packageName
                        }
                    }
                    .sorted()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Group members (${appNames.size})")
                    .setItems(appNames.toTypedArray(), null)
                    .setPositiveButton("Close", null)
                    .show()
            }

            EditableScopeType.FEATURE_GROUP -> {
                val rules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().filter { it.groupId == scope.id }
                if (rules.isEmpty()) {
                    Toast.makeText(requireContext(), "No group members found", Toast.LENGTH_SHORT).show()
                    return
                }
                val featureNames = rules.flatMap { it.targets }
                    .toSet()
                    .map { target ->
                        when (target) {
                            UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER -> "App Blocker"
                            UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER -> "Keyword Blocker"
                            UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER -> "Reel Blocker"
                            UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE -> "Focus Mode"
                        }
                    }
                    .sorted()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Feature targets (${featureNames.size})")
                    .setItems(featureNames.toTypedArray(), null)
                    .setPositiveButton("Close", null)
                    .show()
            }

            else -> {
                Toast.makeText(requireContext(), "Available for groups only", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveEditableScope(rule: AppBlockScheduleRule): EditableScope? {
        return when {
            rule.id.startsWith(APP_GROUP_RULE_PREFIX) -> {
                EditableScope(
                    type = EditableScopeType.APP_GROUP,
                    id = rule.id.removePrefix(APP_GROUP_RULE_PREFIX),
                    title = rule.title
                )
            }

            rule.id.startsWith(FEATURE_GROUP_RULE_PREFIX) -> {
                EditableScope(
                    type = EditableScopeType.FEATURE_GROUP,
                    id = rule.id.removePrefix(FEATURE_GROUP_RULE_PREFIX),
                    title = rule.title
                )
            }

            rule.id.startsWith(FEATURE_RULE_PREFIX) -> {
                EditableScope(
                    type = EditableScopeType.FEATURE_SINGLE,
                    id = rule.id.removePrefix(FEATURE_RULE_PREFIX),
                    title = rule.title
                )
            }

            else -> {
                EditableScope(
                    type = EditableScopeType.APP_SINGLE,
                    id = rule.id,
                    title = rule.title
                )
            }
        }
    }

    private fun showChangeTypeDialog(scope: EditableScope) {
        val options = arrayOf("Block", "Cheat")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select schedule type")
            .setItems(options) { _, which ->
                val isCheat = which == 1
                val updated = when (scope.type) {
                    EditableScopeType.APP_SINGLE, EditableScopeType.APP_GROUP -> {
                        val appRules = savedPreferencesLoader.loadAppBlockerScheduleRules().toMutableList()
                        var count = 0
                        for (index in appRules.indices) {
                            val item = appRules[index]
                            val match = when (scope.type) {
                                EditableScopeType.APP_SINGLE -> item.id == scope.id
                                EditableScopeType.APP_GROUP -> item.groupId == scope.id
                                else -> false
                            }
                            if (match) {
                                count++
                                appRules[index] = item.copy(
                                    type = if (isCheat) AppBlockScheduleRule.RuleType.CHEAT else AppBlockScheduleRule.RuleType.BLOCK,
                                    title = buildAppRuleTitle(item.packageName, if (isCheat) AppBlockScheduleRule.RuleType.CHEAT else AppBlockScheduleRule.RuleType.BLOCK, item.recurrence)
                                )
                            }
                        }
                        if (count > 0) {
                            savedPreferencesLoader.saveAppBlockerScheduleRules(appRules)
                        }
                        count
                    }

                    EditableScopeType.FEATURE_SINGLE, EditableScopeType.FEATURE_GROUP -> {
                        val featureRules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().toMutableList()
                        var count = 0
                        for (index in featureRules.indices) {
                            val item = featureRules[index]
                            val match = when (scope.type) {
                                EditableScopeType.FEATURE_SINGLE -> item.id == scope.id
                                EditableScopeType.FEATURE_GROUP -> item.groupId == scope.id
                                else -> false
                            }
                            if (match) {
                                count++
                                val newType = if (isCheat) UnifiedFeatureScheduleRule.RuleType.CHEAT else UnifiedFeatureScheduleRule.RuleType.BLOCK
                                featureRules[index] = item.copy(
                                    type = newType,
                                    title = buildFeatureRuleTitle(item.targets, newType, item.recurrence)
                                )
                            }
                        }
                        if (count > 0) {
                            savedPreferencesLoader.saveUnifiedFeatureScheduleRules(featureRules)
                        }
                        count
                    }
                }

                onScopeUpdated(updated, "Type updated")
            }
            .show()
    }

    private fun showRecurrenceChangeDialog(scope: EditableScope) {
        val options = arrayOf("Hourly", "Daily", "Weekly", "Always")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select recurrence")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> configureScopeHourly(scope)
                    1 -> configureScopeDaily(scope)
                    2 -> configureScopeWeekly(scope)
                    3 -> applyRecurrenceToScope(
                        scope,
                        RecurrenceSelection(option = RecurrenceOption.ALWAYS)
                    )
                }
            }
            .show()
    }

    private fun configureScopeHourly(scope: EditableScope) {
        val input = EditText(requireContext())
        input.hint = "Duration in hours"
        input.setText("2")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hourly rule")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                val hours = input.text?.toString()?.toIntOrNull()?.coerceIn(1, 168) ?: 2
                val end = System.currentTimeMillis() + (hours * 60L * 60L * 1000L)
                applyRecurrenceToScope(
                    scope,
                    RecurrenceSelection(
                        option = RecurrenceOption.HOURLY,
                        durationHours = hours,
                        activeUntilMillis = end
                    )
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun configureScopeDaily(scope: EditableScope) {
        pickTimeMinutes(9 * 60) { start ->
            pickTimeMinutes(18 * 60) { end ->
                applyRecurrenceToScope(
                    scope,
                    RecurrenceSelection(
                        option = RecurrenceOption.DAILY,
                        startMinute = start,
                        endMinute = end
                    )
                )
            }
        }
    }

    private fun configureScopeWeekly(scope: EditableScope) {
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dayIds = arrayOf(
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )
        val selected = BooleanArray(dayLabels.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select days")
            .setMultiChoiceItems(dayLabels, selected) { _, index, isChecked ->
                selected[index] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selectedDays = mutableSetOf<Int>()
                selected.forEachIndexed { index, checked ->
                    if (checked) selectedDays.add(dayIds[index])
                }
                if (selectedDays.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one day", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                pickTimeMinutes(9 * 60) { start ->
                    pickTimeMinutes(18 * 60) { end ->
                        applyRecurrenceToScope(
                            scope,
                            RecurrenceSelection(
                                option = RecurrenceOption.WEEKLY,
                                startMinute = start,
                                endMinute = end,
                                selectedDays = selectedDays
                            )
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyRecurrenceToScope(scope: EditableScope, selection: RecurrenceSelection) {
        val updated = when (scope.type) {
            EditableScopeType.APP_SINGLE, EditableScopeType.APP_GROUP -> {
                val appRules = savedPreferencesLoader.loadAppBlockerScheduleRules().toMutableList()
                var count = 0
                for (index in appRules.indices) {
                    val item = appRules[index]
                    val match = when (scope.type) {
                        EditableScopeType.APP_SINGLE -> item.id == scope.id
                        EditableScopeType.APP_GROUP -> item.groupId == scope.id
                        else -> false
                    }
                    if (match) {
                        count++
                        val recurrence = selection.toAppRecurrence()
                        appRules[index] = item.copy(
                            recurrence = recurrence,
                            startMinute = selection.startMinute,
                            endMinute = selection.endMinute,
                            selectedDays = selection.selectedDays,
                            durationHours = selection.durationHours,
                            activeUntilMillis = selection.activeUntilMillis,
                            title = buildAppRuleTitle(item.packageName, item.type, recurrence)
                        )
                    }
                }
                if (count > 0) {
                    savedPreferencesLoader.saveAppBlockerScheduleRules(appRules)
                }
                count
            }

            EditableScopeType.FEATURE_SINGLE, EditableScopeType.FEATURE_GROUP -> {
                val featureRules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().toMutableList()
                var count = 0
                for (index in featureRules.indices) {
                    val item = featureRules[index]
                    val match = when (scope.type) {
                        EditableScopeType.FEATURE_SINGLE -> item.id == scope.id
                        EditableScopeType.FEATURE_GROUP -> item.groupId == scope.id
                        else -> false
                    }
                    if (match) {
                        count++
                        val recurrence = selection.toFeatureRecurrence()
                        featureRules[index] = item.copy(
                            recurrence = recurrence,
                            startMinute = selection.startMinute,
                            endMinute = selection.endMinute,
                            selectedDays = selection.selectedDays,
                            durationHours = selection.durationHours,
                            activeUntilMillis = selection.activeUntilMillis,
                            title = buildFeatureRuleTitle(item.targets, item.type, recurrence)
                        )
                    }
                }
                if (count > 0) {
                    savedPreferencesLoader.saveUnifiedFeatureScheduleRules(featureRules)
                }
                count
            }
        }

        onScopeUpdated(updated, "Recurrence updated")
    }

    private fun duplicateScope(scope: EditableScope) {
        val copied = when (scope.type) {
            EditableScopeType.APP_SINGLE -> {
                val appRules = savedPreferencesLoader.loadAppBlockerScheduleRules().toMutableList()
                val source = appRules.firstOrNull { it.id == scope.id } ?: return
                appRules.add(
                    source.copy(
                        id = UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        groupId = null,
                        groupTitle = null
                    )
                )
                savedPreferencesLoader.saveAppBlockerScheduleRules(appRules)
                1
            }

            EditableScopeType.APP_GROUP -> {
                val appRules = savedPreferencesLoader.loadAppBlockerScheduleRules().toMutableList()
                val source = appRules.filter { it.groupId == scope.id }
                if (source.isEmpty()) return
                val newGroupId = UUID.randomUUID().toString()
                source.forEach { item ->
                    appRules.add(
                        item.copy(
                            id = UUID.randomUUID().toString(),
                            createdAt = System.currentTimeMillis(),
                            groupId = newGroupId,
                            groupTitle = (item.groupTitle ?: "Unified App Batch") + " Copy"
                        )
                    )
                }
                savedPreferencesLoader.saveAppBlockerScheduleRules(appRules)
                source.size
            }

            EditableScopeType.FEATURE_SINGLE -> {
                val featureRules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().toMutableList()
                val source = featureRules.firstOrNull { it.id == scope.id } ?: return
                featureRules.add(
                    source.copy(
                        id = UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        groupId = null,
                        groupTitle = null
                    )
                )
                savedPreferencesLoader.saveUnifiedFeatureScheduleRules(featureRules)
                1
            }

            EditableScopeType.FEATURE_GROUP -> {
                val featureRules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().toMutableList()
                val source = featureRules.filter { it.groupId == scope.id }
                if (source.isEmpty()) return
                val newGroupId = UUID.randomUUID().toString()
                source.forEach { item ->
                    featureRules.add(
                        item.copy(
                            id = UUID.randomUUID().toString(),
                            createdAt = System.currentTimeMillis(),
                            groupId = newGroupId,
                            groupTitle = (item.groupTitle ?: "Unified Feature Batch") + " Copy"
                        )
                    )
                }
                savedPreferencesLoader.saveUnifiedFeatureScheduleRules(featureRules)
                source.size
            }
        }

        onScopeUpdated(copied, "Schedule duplicated")
    }

    private fun onScopeUpdated(affectedCount: Int, successMessage: String) {
        if (affectedCount <= 0) {
            Toast.makeText(requireContext(), "No schedules updated", Toast.LENGTH_SHORT).show()
            return
        }
        sendRefreshRequest()
        loadSchedulesAndLimits()
        Toast.makeText(requireContext(), "$successMessage ($affectedCount)", Toast.LENGTH_SHORT).show()
    }

    private fun buildAppRuleTitle(
        packageName: String,
        type: AppBlockScheduleRule.RuleType,
        recurrence: AppBlockScheduleRule.Recurrence
    ): String {
        val appName = try {
            requireContext().packageManager.getApplicationLabel(
                requireContext().packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) {
            packageName
        }
        return "Unified • $appName • ${type.name} • ${recurrence.name}"
    }

    private fun buildFeatureRuleTitle(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>,
        type: UnifiedFeatureScheduleRule.RuleType,
        recurrence: UnifiedFeatureScheduleRule.Recurrence
    ): String {
        return "Unified Features • ${targets.joinToString(" + " ) { it.toDisplayName() }} • ${type.name} • ${recurrence.name}"
    }

    private fun RecurrenceSelection.toAppRecurrence(): AppBlockScheduleRule.Recurrence {
        return when (option) {
            RecurrenceOption.HOURLY -> AppBlockScheduleRule.Recurrence.HOURLY
            RecurrenceOption.DAILY -> AppBlockScheduleRule.Recurrence.DAILY
            RecurrenceOption.WEEKLY -> AppBlockScheduleRule.Recurrence.WEEKLY
            RecurrenceOption.ALWAYS -> AppBlockScheduleRule.Recurrence.ALWAYS
        }
    }

    private fun RecurrenceSelection.toFeatureRecurrence(): UnifiedFeatureScheduleRule.Recurrence {
        return when (option) {
            RecurrenceOption.HOURLY -> UnifiedFeatureScheduleRule.Recurrence.HOURLY
            RecurrenceOption.DAILY -> UnifiedFeatureScheduleRule.Recurrence.DAILY
            RecurrenceOption.WEEKLY -> UnifiedFeatureScheduleRule.Recurrence.WEEKLY
            RecurrenceOption.ALWAYS -> UnifiedFeatureScheduleRule.Recurrence.ALWAYS
        }
    }

    private fun deleteSchedule(rule: AppBlockScheduleRule) {
        val isGroup = rule.id.startsWith(APP_GROUP_RULE_PREFIX) || rule.id.startsWith(FEATURE_GROUP_RULE_PREFIX)
        val scope = resolveEditableScope(rule)
        val stats = scope?.let { resolveScopeStats(it) }
        val deleteTitle = if (isGroup) "Delete Group?" else "Delete Schedule?"
        val ruleCount = stats?.itemCount ?: 0
        val deleteMessage = if (isGroup) {
            "This will permanently delete $ruleCount schedule rule${if (ruleCount != 1) "s" else ""} in this batch.\n\n${rule.title}\n${stats?.summary ?: ""}"
        } else {
            "Delete this schedule?\n\n${rule.title}"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(deleteTitle)
            .setMessage(deleteMessage)
            .setPositiveButton(if (isGroup) "Delete all $ruleCount" else "Delete") { _, _ ->
                val removed = removeSchedule(rule)
                if (removed > 0) {
                    sendRefreshRequest()
                    loadSchedulesAndLimits()
                    val deletedMsg = if (isGroup) {
                        "Deleted $removed schedule rule${if (removed != 1) "s" else ""} from batch"
                    } else {
                        "Schedule deleted"
                    }
                    Toast.makeText(requireContext(), deletedMsg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeSchedule(rule: AppBlockScheduleRule): Int {
        return when {
            rule.id.startsWith(APP_GROUP_RULE_PREFIX) -> {
                val groupId = rule.id.removePrefix(APP_GROUP_RULE_PREFIX)
                savedPreferencesLoader.removeAppBlockerScheduleGroup(groupId)
            }

            rule.id.startsWith(FEATURE_GROUP_RULE_PREFIX) -> {
                val groupId = rule.id.removePrefix(FEATURE_GROUP_RULE_PREFIX)
                savedPreferencesLoader.removeUnifiedFeatureScheduleGroup(groupId)
            }

            rule.id.startsWith(FEATURE_RULE_PREFIX) -> {
                savedPreferencesLoader.removeUnifiedFeatureScheduleRule(rule.id.removePrefix(FEATURE_RULE_PREFIX))
                1
            }

            else -> {
                savedPreferencesLoader.removeAppBlockerScheduleRule(rule.id)
                1
            }
        }
    }

    private fun showUnifiedFeatureTargetDialog() {
        val labels = arrayOf(
            "App Blocker",
            "Keyword Blocker",
            "Reel Blocker",
            "Focus Mode"
        )
        val targets = arrayOf(
            UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER,
            UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER,
            UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER,
            UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE
        )
        val checked = BooleanArray(labels.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select blocker features")
            .setMultiChoiceItems(labels, checked) { _, index, isChecked ->
                checked[index] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selectedTargets = mutableSetOf<UnifiedFeatureScheduleRule.FeatureTarget>()
                checked.forEachIndexed { index, isSelected ->
                    if (isSelected) selectedTargets.add(targets[index])
                }
                if (selectedTargets.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one feature", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                showUnifiedFeatureRuleTypeDialog(selectedTargets)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUnifiedFeatureRuleTypeDialog(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>
    ) {
        val types = arrayOf("Block Hours (feature ON)", "Cheat Hours (feature OFF)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rule type")
            .setItems(types) { _, which ->
                val type = if (which == 1) {
                    UnifiedFeatureScheduleRule.RuleType.CHEAT
                } else {
                    UnifiedFeatureScheduleRule.RuleType.BLOCK
                }
                showUnifiedFeatureRecurrenceDialog(targets, type)
            }
            .show()
    }

    private fun showUnifiedFeatureRecurrenceDialog(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>,
        type: UnifiedFeatureScheduleRule.RuleType
    ) {
        val options = arrayOf("Hourly", "Daily", "Weekly", "Always")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recurrence")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> configureUnifiedFeatureHourly(targets, type)
                    1 -> configureUnifiedFeatureDaily(targets, type)
                    2 -> configureUnifiedFeatureWeekly(targets, type)
                    3 -> saveUnifiedFeatureRule(
                        targets = targets,
                        type = type,
                        recurrence = UnifiedFeatureScheduleRule.Recurrence.ALWAYS,
                        startMinute = 0,
                        endMinute = 0,
                        selectedDays = emptySet(),
                        durationHours = 0,
                        activeUntilMillis = 0L
                    )
                }
            }
            .show()
    }

    private fun configureUnifiedFeatureHourly(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>,
        type: UnifiedFeatureScheduleRule.RuleType
    ) {
        val input = EditText(requireContext())
        input.hint = "Duration in hours"
        input.setText("2")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hourly rule")
            .setMessage("Applies from now for N hours")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val hours = input.text?.toString()?.toIntOrNull()?.coerceIn(1, 168) ?: 2
                val end = System.currentTimeMillis() + (hours * 60L * 60L * 1000L)
                saveUnifiedFeatureRule(
                    targets = targets,
                    type = type,
                    recurrence = UnifiedFeatureScheduleRule.Recurrence.HOURLY,
                    startMinute = 0,
                    endMinute = 0,
                    selectedDays = emptySet(),
                    durationHours = hours,
                    activeUntilMillis = end
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun configureUnifiedFeatureDaily(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>,
        type: UnifiedFeatureScheduleRule.RuleType
    ) {
        pickTimeMinutes(9 * 60) { start ->
            pickTimeMinutes(18 * 60) { end ->
                saveUnifiedFeatureRule(
                    targets = targets,
                    type = type,
                    recurrence = UnifiedFeatureScheduleRule.Recurrence.DAILY,
                    startMinute = start,
                    endMinute = end,
                    selectedDays = emptySet(),
                    durationHours = 0,
                    activeUntilMillis = 0L
                )
            }
        }
    }

    private fun configureUnifiedFeatureWeekly(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>,
        type: UnifiedFeatureScheduleRule.RuleType
    ) {
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dayIds = arrayOf(
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )
        val selected = BooleanArray(dayLabels.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select days")
            .setMultiChoiceItems(dayLabels, selected) { _, index, isChecked ->
                selected[index] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selectedDays = mutableSetOf<Int>()
                selected.forEachIndexed { index, checked ->
                    if (checked) selectedDays.add(dayIds[index])
                }
                if (selectedDays.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one day", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                pickTimeMinutes(9 * 60) { start ->
                    pickTimeMinutes(18 * 60) { end ->
                        saveUnifiedFeatureRule(
                            targets = targets,
                            type = type,
                            recurrence = UnifiedFeatureScheduleRule.Recurrence.WEEKLY,
                            startMinute = start,
                            endMinute = end,
                            selectedDays = selectedDays,
                            durationHours = 0,
                            activeUntilMillis = 0L
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveUnifiedFeatureRule(
        targets: Set<UnifiedFeatureScheduleRule.FeatureTarget>,
        type: UnifiedFeatureScheduleRule.RuleType,
        recurrence: UnifiedFeatureScheduleRule.Recurrence,
        startMinute: Int,
        endMinute: Int,
        selectedDays: Set<Int>,
        durationHours: Int,
        activeUntilMillis: Long
    ) {
        if (targets.isEmpty()) {
            Toast.makeText(requireContext(), "No features selected", Toast.LENGTH_SHORT).show()
            return
        }

        val title = "Unified Features • ${targets.joinToString(" + " ) { it.toDisplayName() }} • ${type.name} • ${recurrence.name}"
        val groupId = UUID.randomUUID().toString()
        val rule = UnifiedFeatureScheduleRule(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            recurrence = recurrence,
            targets = targets,
            startMinute = startMinute,
            endMinute = endMinute,
            selectedDays = selectedDays,
            durationHours = durationHours,
            activeUntilMillis = activeUntilMillis,
            createdAt = System.currentTimeMillis(),
            groupId = groupId,
            groupTitle = "Unified Feature Batch"
        )

        savedPreferencesLoader.upsertUnifiedFeatureScheduleRule(rule)
        sendRefreshRequest()
        loadSchedulesAndLimits()
        Toast.makeText(
            requireContext(),
            "Unified feature schedule saved",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showUnifiedScheduleScopeDialog() {
        val blockedApps = savedPreferencesLoader.loadBlockedApps().toMutableSet()
        val launchLimitedApps = savedPreferencesLoader.loadAppLaunchLimitRules().map { it.packageName }.toSet()
        val allCandidates = (blockedApps + launchLimitedApps)
            .filterNot { it.equals("com.alhaq.deenshield", ignoreCase = true) }
            .toSet()

        if (allCandidates.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No eligible apps found. Add blocked apps or launch limits first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val options = arrayOf(
            "All blocked apps",
            "All launch-limited apps",
            "Select specific apps"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Schedule Scope")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (blockedApps.isEmpty()) {
                            Toast.makeText(requireContext(), "No blocked apps configured", Toast.LENGTH_SHORT).show()
                        } else {
                            showUnifiedRuleTypeDialog(blockedApps)
                        }
                    }
                    1 -> {
                        if (launchLimitedApps.isEmpty()) {
                            Toast.makeText(requireContext(), "No launch-limited apps configured", Toast.LENGTH_SHORT).show()
                        } else {
                            showUnifiedRuleTypeDialog(launchLimitedApps)
                        }
                    }
                    2 -> showSpecificAppSelectionDialog(allCandidates)
                }
            }
            .show()
    }

    private fun showSpecificAppSelectionDialog(candidatePackages: Set<String>) {
        val sortedPackages = candidatePackages.sortedBy { pkg ->
            try {
                requireContext().packageManager.getApplicationLabel(
                    requireContext().packageManager.getApplicationInfo(pkg, 0)
                ).toString().lowercase()
            } catch (_: Exception) {
                pkg.lowercase()
            }
        }

        val displayNames = sortedPackages.map { pkg ->
            try {
                requireContext().packageManager.getApplicationLabel(
                    requireContext().packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (_: Exception) {
                pkg
            }
        }.toTypedArray()

        val checked = BooleanArray(displayNames.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select apps")
            .setMultiChoiceItems(displayNames, checked) { _, index, isChecked ->
                checked[index] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selected = sortedPackages.filterIndexed { index, _ -> checked[index] }.toSet()
                if (selected.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one app", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                showUnifiedRuleTypeDialog(selected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUnifiedRuleTypeDialog(targetPackages: Set<String>) {
        val types = arrayOf("Block Hours", "Cheat Hours")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rule type")
            .setItems(types) { _, which ->
                val type = if (which == 1) AppBlockScheduleRule.RuleType.CHEAT else AppBlockScheduleRule.RuleType.BLOCK
                showUnifiedRecurrenceDialog(targetPackages, type)
            }
            .show()
    }

    private fun showUnifiedRecurrenceDialog(
        targetPackages: Set<String>,
        type: AppBlockScheduleRule.RuleType
    ) {
        val options = arrayOf("Hourly", "Daily", "Weekly", "Always")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recurrence")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> configureUnifiedHourly(targetPackages, type)
                    1 -> configureUnifiedDaily(targetPackages, type)
                    2 -> configureUnifiedWeekly(targetPackages, type)
                    3 -> saveUnifiedRules(
                        targetPackages = targetPackages,
                        type = type,
                        recurrence = AppBlockScheduleRule.Recurrence.ALWAYS,
                        startMinute = 0,
                        endMinute = 0,
                        selectedDays = emptySet(),
                        durationHours = 0,
                        activeUntilMillis = 0L
                    )
                }
            }
            .show()
    }

    private fun configureUnifiedHourly(targetPackages: Set<String>, type: AppBlockScheduleRule.RuleType) {
        val input = EditText(requireContext())
        input.hint = "Duration in hours"
        input.setText("2")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hourly rule")
            .setMessage("Applies from now for N hours")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val hours = input.text?.toString()?.toIntOrNull()?.coerceIn(1, 168) ?: 2
                val end = System.currentTimeMillis() + (hours * 60L * 60L * 1000L)
                saveUnifiedRules(
                    targetPackages = targetPackages,
                    type = type,
                    recurrence = AppBlockScheduleRule.Recurrence.HOURLY,
                    startMinute = 0,
                    endMinute = 0,
                    selectedDays = emptySet(),
                    durationHours = hours,
                    activeUntilMillis = end
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun configureUnifiedDaily(targetPackages: Set<String>, type: AppBlockScheduleRule.RuleType) {
        pickTimeMinutes(9 * 60) { start ->
            pickTimeMinutes(18 * 60) { end ->
                saveUnifiedRules(
                    targetPackages = targetPackages,
                    type = type,
                    recurrence = AppBlockScheduleRule.Recurrence.DAILY,
                    startMinute = start,
                    endMinute = end,
                    selectedDays = emptySet(),
                    durationHours = 0,
                    activeUntilMillis = 0L
                )
            }
        }
    }

    private fun configureUnifiedWeekly(targetPackages: Set<String>, type: AppBlockScheduleRule.RuleType) {
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dayIds = arrayOf(
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )
        val selected = BooleanArray(dayLabels.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select days")
            .setMultiChoiceItems(dayLabels, selected) { _, index, isChecked ->
                selected[index] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selectedDays = mutableSetOf<Int>()
                selected.forEachIndexed { index, checked ->
                    if (checked) selectedDays.add(dayIds[index])
                }
                if (selectedDays.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one day", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                pickTimeMinutes(9 * 60) { start ->
                    pickTimeMinutes(18 * 60) { end ->
                        saveUnifiedRules(
                            targetPackages = targetPackages,
                            type = type,
                            recurrence = AppBlockScheduleRule.Recurrence.WEEKLY,
                            startMinute = start,
                            endMinute = end,
                            selectedDays = selectedDays,
                            durationHours = 0,
                            activeUntilMillis = 0L
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickTimeMinutes(initialMinutes: Int, onPicked: (Int) -> Unit) {
        val hour = (initialMinutes / 60).coerceIn(0, 23)
        val minute = (initialMinutes % 60).coerceIn(0, 59)
        TimePickerDialog(requireContext(), { _, h, m ->
            onPicked(h * 60 + m)
        }, hour, minute, true).show()
    }

    private fun saveUnifiedRules(
        targetPackages: Set<String>,
        type: AppBlockScheduleRule.RuleType,
        recurrence: AppBlockScheduleRule.Recurrence,
        startMinute: Int,
        endMinute: Int,
        selectedDays: Set<Int>,
        durationHours: Int,
        activeUntilMillis: Long
    ) {
        if (targetPackages.isEmpty()) {
            Toast.makeText(requireContext(), "No target apps selected", Toast.LENGTH_SHORT).show()
            return
        }

        val blocked = savedPreferencesLoader.loadBlockedApps().toMutableSet()
        blocked.addAll(targetPackages)
        savedPreferencesLoader.saveBlockedApps(blocked)

        val stamp = System.currentTimeMillis()
        val groupId = UUID.randomUUID().toString()
        val groupTitle = "Unified App Batch (${targetPackages.size} apps)"
        targetPackages.forEach { packageName ->
            val appName = try {
                requireContext().packageManager.getApplicationLabel(
                    requireContext().packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (_: Exception) {
                packageName
            }

            val title = "Unified • $appName • ${type.name} • ${recurrence.name}"
            val rule = AppBlockScheduleRule(
                id = UUID.randomUUID().toString(),
                title = title,
                packageName = packageName,
                type = type,
                recurrence = recurrence,
                startMinute = startMinute,
                endMinute = endMinute,
                selectedDays = selectedDays,
                durationHours = durationHours,
                activeUntilMillis = activeUntilMillis,
                createdAt = stamp,
                groupId = groupId,
                groupTitle = groupTitle
            )
            savedPreferencesLoader.upsertAppBlockerScheduleRule(rule)
        }

        sendRefreshRequest()
        loadSchedulesAndLimits()
        Toast.makeText(
            requireContext(),
            "Unified schedule applied to ${targetPackages.size} app(s)",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun editLaunchLimit(rule: AppLaunchLimitRule) {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(rule.packageName, 0)
            val appName = appInfo.loadLabel(requireContext().packageManager).toString()

            val dialog = SetLaunchLimitDialog(
                packageName = rule.packageName,
                appName = appName,
                onSave = { updatedRule ->
                    if (updatedRule != null) {
                        savedPreferencesLoader.addAppLaunchLimitRule(updatedRule)
                        Toast.makeText(
                            requireContext(),
                            "Launch limit updated: ${updatedRule.getDescription()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    sendRefreshRequest()
                    loadSchedulesAndLimits()
                }
            )
            dialog.show(childFragmentManager, "edit_launch_limit_${rule.id}")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error editing limit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteLaunchLimit(rule: AppLaunchLimitRule) {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(rule.packageName, 0)
            val appName = appInfo.loadLabel(requireContext().packageManager).toString()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Launch Limit?")
                .setMessage("Remove launch limit for $appName?")
                .setPositiveButton("Delete") { _, _ ->
                    savedPreferencesLoader.removeAppLaunchLimitRule(rule.packageName)
                    Toast.makeText(
                        requireContext(),
                        "Launch limit removed",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendRefreshRequest()
                    loadSchedulesAndLimits()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error deleting limit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRefreshRequest() {
        val intent = Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
        requireContext().sendBroadcast(intent.setPackage(requireContext().packageName))
        val unifiedIntent = Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        requireContext().sendBroadcast(unifiedIntent.setPackage(requireContext().packageName))
    }

    private fun toDisplayRule(rule: UnifiedFeatureScheduleRule): AppBlockScheduleRule {
        return AppBlockScheduleRule(
            id = "$FEATURE_RULE_PREFIX${rule.id}",
            title = rule.title,
            packageName = "unified.features",
            type = if (rule.type == UnifiedFeatureScheduleRule.RuleType.BLOCK) {
                AppBlockScheduleRule.RuleType.BLOCK
            } else {
                AppBlockScheduleRule.RuleType.CHEAT
            },
            recurrence = when (rule.recurrence) {
                UnifiedFeatureScheduleRule.Recurrence.HOURLY -> AppBlockScheduleRule.Recurrence.HOURLY
                UnifiedFeatureScheduleRule.Recurrence.DAILY -> AppBlockScheduleRule.Recurrence.DAILY
                UnifiedFeatureScheduleRule.Recurrence.WEEKLY -> AppBlockScheduleRule.Recurrence.WEEKLY
                UnifiedFeatureScheduleRule.Recurrence.ALWAYS -> AppBlockScheduleRule.Recurrence.ALWAYS
            },
            startMinute = rule.startMinute,
            endMinute = rule.endMinute,
            selectedDays = rule.selectedDays,
            durationHours = rule.durationHours,
            activeUntilMillis = rule.activeUntilMillis,
            createdAt = rule.createdAt,
            groupId = rule.groupId,
            groupTitle = rule.groupTitle
        )
    }

    private fun buildDisplaySchedules(
        appSchedules: List<AppBlockScheduleRule>,
        featureSchedules: List<UnifiedFeatureScheduleRule>
    ): List<AppBlockScheduleRule> {
        val rows = mutableListOf<AppBlockScheduleRule>()

        val groupedApp = appSchedules.filter { !it.groupId.isNullOrBlank() }.groupBy { it.groupId!! }
        val singleApp = appSchedules.filter { it.groupId.isNullOrBlank() }
        rows.addAll(singleApp)
        groupedApp.forEach { (groupId, rules) ->
            val first = rules.first()
            val appCount = rules.map { it.packageName }.toSet().size
            rows.add(
                first.copy(
                    id = "$APP_GROUP_RULE_PREFIX$groupId",
                    title = "${first.groupTitle ?: "Unified App Batch"} • $appCount apps",
                    packageName = "unified.apps.group"
                )
            )
        }

        val featureDisplay = featureSchedules.map { toDisplayRule(it) }
        val groupedFeature = featureDisplay.filter { !it.groupId.isNullOrBlank() }.groupBy { it.groupId!! }
        val singleFeature = featureDisplay.filter { it.groupId.isNullOrBlank() }
        rows.addAll(singleFeature)
        groupedFeature.forEach { (groupId, rules) ->
            val first = rules.first()
            val featureCount = featureSchedules
                .filter { it.groupId == groupId }
                .flatMap { it.targets }
                .toSet()
                .size

            rows.add(
                first.copy(
                    id = "$FEATURE_GROUP_RULE_PREFIX$groupId",
                    title = "${first.groupTitle ?: "Unified Feature Batch"} • $featureCount features",
                    packageName = "unified.features.group"
                )
            )
        }

        return rows.sortedByDescending { it.createdAt }
    }

    private fun UnifiedFeatureScheduleRule.FeatureTarget.toDisplayName(): String {
        return when (this) {
            UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER -> "App"
            UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER -> "Keyword"
            UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER -> "Reel"
            UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE -> "Focus"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val FEATURE_RULE_PREFIX = "ufs::"
        private const val APP_GROUP_RULE_PREFIX = "app_group::"
        private const val FEATURE_GROUP_RULE_PREFIX = "feature_group::"
        const val FRAGMENT_ID = "manage_block_schedules"
    }
}
