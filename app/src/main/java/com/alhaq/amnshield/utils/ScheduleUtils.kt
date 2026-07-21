package com.alhaq.amnshield.utils

import android.content.Context
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.ui.state.SchedulePeriod
import java.util.Calendar
import java.util.UUID

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
        selectedDays: Set<Int>?,
        nowMillis: Long
    ): Boolean {
        if (startMinutes == endMinutes || selectedDays == null || selectedDays.isEmpty()) return false

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
        selectedDays: Set<Int>?,
        nowMillis: Long
    ): Long? {
        if (selectedDays == null || !isWeeklyWindowActive(startMinutes, endMinutes, selectedDays, nowMillis)) return null

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

    fun periodsOverlap(p1: SchedulePeriod, p2: SchedulePeriod): Boolean {
        val commonDays = p1.days.intersect(p2.days.toSet())
        if (commonDays.isEmpty()) return false

        val s1 = timeToMinutes(p1.startTime)
        val e1 = timeToMinutes(p1.endTime)
        val s2 = timeToMinutes(p2.startTime)
        val e2 = timeToMinutes(p2.endTime)

        fun getIntervals(s: Int, e: Int): List<Pair<Int, Int>> {
            return if (e >= s) {
                listOf(Pair(s, e))
            } else {
                listOf(Pair(s, 1440), Pair(0, e))
            }
        }

        val intervals1 = getIntervals(s1, e1)
        val intervals2 = getIntervals(s2, e2)

        for (i1 in intervals1) {
            for (i2 in intervals2) {
                if (i1.first < i2.second && i2.first < i1.second) {
                    return true
                }
            }
        }
        return false
    }

    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun dayToCalendarInt(day: String): Int {
        return when (day.lowercase()) {
            "sun" -> Calendar.SUNDAY
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            else -> Calendar.MONDAY
        }
    }

    fun calendarIntToDay(dayInt: Int): String {
        return when (dayInt) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon"
        }
    }

    fun mergeSchedules(periods: List<SchedulePeriod>): List<SchedulePeriod> {
        if (periods.size <= 1) return periods

        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayIntervals = daysOfWeek.associateWith { mutableListOf<Pair<Int, Int>>() }.toMutableMap()

        for (p in periods) {
            val s = timeToMinutes(p.startTime)
            val e = timeToMinutes(p.endTime)
            val intervals = if (e >= s) {
                listOf(Pair(s, e))
            } else {
                listOf(Pair(s, 1440), Pair(0, e))
            }
            for (day in p.days) {
                dayIntervals[day]?.addAll(intervals)
            }
        }

        val mergedDayIntervals = mutableMapOf<String, List<Pair<Int, Int>>>()
        for ((day, intervals) in dayIntervals) {
            if (intervals.isEmpty()) continue
            intervals.sortBy { it.first }
            val merged = mutableListOf<Pair<Int, Int>>()
            var current = intervals[0]
            for (i in 1 until intervals.size) {
                val next = intervals[i]
                if (next.first <= current.second) {
                    current = Pair(current.first, maxOf(current.second, next.second))
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            mergedDayIntervals[day] = merged
        }

        val intervalToDays = mutableMapOf<Pair<Int, Int>, MutableList<String>>()
        for ((day, intervals) in mergedDayIntervals) {
            for (interval in intervals) {
                intervalToDays.getOrPut(interval) { mutableListOf() }.add(day)
            }
        }

        val result = mutableListOf<SchedulePeriod>()
        for ((interval, days) in intervalToDays) {
            val startStr = String.format("%02d:%02d", interval.first / 60, interval.first % 60)
            val endStr = if (interval.second >= 1440) "00:00" else String.format("%02d:%02d", interval.second / 60, interval.second % 60)
            result.add(SchedulePeriod(startStr, endStr, days))
        }

        return result
    }


}

