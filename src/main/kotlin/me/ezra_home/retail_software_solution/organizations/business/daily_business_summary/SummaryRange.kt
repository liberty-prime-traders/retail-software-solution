package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary

import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.PeriodStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.LocalDate
import java.time.OffsetDateTime

internal class SummaryRange(val from: LocalDate, val before: LocalDate, asOf: OffsetDateTime) {

    val today: LocalDate = DateTimes.Local.atOrganizationZone(asOf)

    val dates: List<LocalDate>

    init {
        if (!from.isBefore(before)) {
            throw RtsGenericException("'from' must be before 'before'")
        }
        if (before.isAfter(today.plusDays(1))) {
            throw RtsGenericException("'before' cannot be after tomorrow")
        }
        if (from.plusDays(MAXIMUM_DAYS) < before) {
            throw RtsGenericException("The range cannot span more than $MAXIMUM_DAYS days")
        }
        dates = generateSequence(from) { date -> date.plusDays(1) }.takeWhile { date -> date.isBefore(before) }.toList()
    }

    val rangeStatus: PeriodStatus = statusOf(dates.last())

    fun statusOf(date: LocalDate): PeriodStatus =
        if (date == today) PeriodStatus.IN_PROGRESS else PeriodStatus.COMPLETE

    companion object {
        const val MAXIMUM_DAYS = 92L
    }
}
