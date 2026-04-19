package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional

object DateTimes {

    fun isValidTimezone(tz: String): Boolean = runCatching { ZoneId.of(tz) }.isSuccess

    fun validateTimezone(timezone: String?) {
        if (timezone != null && !isValidTimezone(timezone))
            throw RtsGenericException("'$timezone' is not a valid timezone")
    }

    fun validateTimezone(timezone: Optional<String>?) {
        timezone?.ifPresent { validateTimezone(it) }
    }

    object Local {
        object Now {
            fun system(): LocalDate = LocalDate.now()
            fun organization(): LocalDate = LocalDate.now(organizationZoneId())
        }
    }

    fun organizationZoneId(): ZoneId {
        return ZoneId.of(SessionContextProvider.getOrgTimezone())
    }
}
