package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "user_location")
class UserLocationEntity : BaseEntity() {
    @NotNull
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null

    @NotNull
    @Column(name = "location_id", nullable = false)
    var locationId: UUID? = null

    @CreationTimestamp(source = SourceType.VM)
    @Column(name = "start_on")
    var startOn: OffsetDateTime? = null

    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null
}
