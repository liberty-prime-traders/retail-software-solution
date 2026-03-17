package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.LocalDate

@MappedSuperclass
abstract class ExpirableDateAssignmentEntity(
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null

) : HasReferenceEntity()
