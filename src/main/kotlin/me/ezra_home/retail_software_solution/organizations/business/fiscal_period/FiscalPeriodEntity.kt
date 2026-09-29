package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.FISCAL_PERIOD)
class FiscalPeriodEntity(

    @Column(name = "name", length = 50, nullable = false)
    var name: String,

    @NotAudited
    @Column(name = "start_date", nullable = false, updatable = false)
    var startDate: LocalDate,

    @NotAudited
    @Column(name = "end_date", nullable = false, updatable = false)
    var endDate: LocalDate,

    @NotAudited
    @Column(name = "is_year_end", nullable = false, updatable = false)
    var yearEnd: Boolean = false,

    @NotAudited
    @Column(name = "is_stub", nullable = false, updatable = false)
    var stub: Boolean = false,

    @Column(name = "closed_at")
    var closedAt: Instant? = null,

    @Column(name = "closed_by")
    var closedBy: UUID? = null

) : HasCreatorEntity() {
    val isClosed get() = closedAt != null
}
