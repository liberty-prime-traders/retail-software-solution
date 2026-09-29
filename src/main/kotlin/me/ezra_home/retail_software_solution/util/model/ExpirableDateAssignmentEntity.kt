package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.business.DateTimes
import java.time.LocalDate

@MappedSuperclass
abstract class ExpirableDateAssignmentEntity(
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null

) : HasReferenceEntity() {

    fun isActive(): Boolean {
        val today = DateTimes.Local.Now.organization()
        return !startDate.isAfter(today) && (endDate == null || !endDate!!.isBefore(today))
    }
}
