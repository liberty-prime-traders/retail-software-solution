package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Optional

object DateTimes {

    data class DateRange(val start: LocalDate, val end: LocalDate)

    fun validateTimezone(timezone: Optional<String>?) {
        timezone?.ifPresent { validateTimezone(it) }
    }

    fun validateTimezone(timezone: String?) {
        if (timezone != null && !isValidTimezone(timezone))
            throw RtsGenericException("'$timezone' is not a valid timezone")
    }

    fun isValidTimezone(tz: String): Boolean = runCatching { ZoneId.of(tz) }.isSuccess

    object Local {
        object Now {
            fun system(): LocalDate = LocalDate.now()
            fun organization(): LocalDate = LocalDate.now(organizationZoneId())
        }

        fun atOrganizationZone(offsetDateTime: OffsetDateTime): LocalDate {
            return offsetDateTime.atZoneSameInstant(organizationZoneId()).toLocalDate()
        }
    }

    object Offset {
        object Now {
            fun system(): OffsetDateTime = OffsetDateTime.now()
            fun organization(): OffsetDateTime = OffsetDateTime.now(organizationZoneId())
        }
    }

    private fun organizationZoneId(): ZoneId {
        return ZoneId.of(SessionContextProvider.getOrgTimezone())
    }

    fun todayIsInRange(dateRange: DateRange): Boolean {
        return dateRange.start.isBefore(Local.Now.organization())
                && dateRange.end.isAfter(Local.Now.organization())
    }

}
