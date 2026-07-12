package com.alhaq.deenshield.utils

import java.util.Calendar

object ScheduleUtils {

    fun isDailyWindowActive(startMinutes: Int, endMinutes: Int, nowMillis: Long): Boolean {
        if (startMinutes == endMinutes) return false
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun isWeeklyWindowActive(
        startMinutes: Int,
        endMinutes: Int,
        selectedDays: Set<Int>,
        nowMillis: Long
    ): Boolean {
        if (startMinutes == endMinutes || selectedDays.isEmpty()) return false

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1

        return if (startMinutes < endMinutes) {
            selectedDays.contains(today) && currentMinutes in startMinutes until endMinutes
        } else {
            (selectedDays.contains(today) && currentMinutes >= startMinutes) ||
                (selectedDays.contains(yesterday) && currentMinutes < endMinutes)
        }
    }

    fun getDailyWindowEndTime(startMinutes: Int, endMinutes: Int, nowMillis: Long): Long? {
        if (!isDailyWindowActive(startMinutes, endMinutes, nowMillis)) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val endCal = now.clone() as Calendar
        if (startMinutes > endMinutes && currentMinutes >= startMinutes) {
            endCal.add(Calendar.DATE, 1)
        }
        endCal.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        endCal.set(Calendar.MINUTE, endMinutes % 60)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        return endCal.timeInMillis.coerceAtLeast(nowMillis + 60_000L)
    }

    fun getWeeklyWindowEndTime(
        startMinutes: Int,
        endMinutes: Int,
        selectedDays: Set<Int>,
        nowMillis: Long
    ): Long? {
        if (!isWeeklyWindowActive(startMinutes, endMinutes, selectedDays, nowMillis)) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)

        val endCal = now.clone() as Calendar
        if (startMinutes > endMinutes && selectedDays.contains(today) && currentMinutes >= startMinutes) {
            endCal.add(Calendar.DATE, 1)
        }
        endCal.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        endCal.set(Calendar.MINUTE, endMinutes % 60)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        return endCal.timeInMillis.coerceAtLeast(nowMillis + 60_000L)
    }
}
