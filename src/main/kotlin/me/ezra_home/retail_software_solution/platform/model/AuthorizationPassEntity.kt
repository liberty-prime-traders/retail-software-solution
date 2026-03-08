package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.enums.PassStatus
import me.ezra_home.retail_software_solution.util.enums.PassStatusConverter
import me.ezra_home.retail_software_solution.util.enums.PassType
import me.ezra_home.retail_software_solution.util.enums.PassTypeConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.AUTHORIZATION_PASS)
@HasReference(tableName = TableName.AUTHORIZATION_PASS)
class AuthorizationPassEntity(

    @NotAudited
    @Column(name = "code", nullable = false, unique = true, updatable = false)
    var code: UUID = UUID.randomUUID(),

    @Column(name = "pass_type", nullable = false, length = 5)
    @Convert(converter = PassTypeConverter::class)
    var passType: PassType,

    @Column(name = "max_use_count", nullable = false)
    var maxUseCount: Int,

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0,

    @Column(name = "assigned_to_id", nullable = false)
    var assignedToId: UUID,

    @Column(name = "pass_status", nullable = false, length = 5)
    @Convert(converter = PassStatusConverter::class)
    var passStatus: PassStatus = PassStatus.ACTIVE,

    @Column(name = "expires_on")
    var expiresOn: OffsetDateTime? = null

) : HasReferenceEntity()
