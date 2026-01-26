package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseRevisionEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  @Column(name = "rev")
  var id: Long? = null,

  @Column(name = "created_on")
  @RevisionTimestamp
  var createdOn: Long? = null,

  @Column(name = "created_by_id")
  @CreatedBy
  var createdById: UUID? = null,

  @Column(name = "correlation_id")
  var correlationId: UUID? = null
)


