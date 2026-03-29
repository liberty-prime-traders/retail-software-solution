package me.ezra_home.retail_software_solution.locations.rest.endpoints.sync

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncInitiator
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncLogFetcher
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncLogUpdater
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.dto.SyncLogResponseDto
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.dto.SyncRequestDto
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncInitiateType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/sync")
class SyncEndpoint(
  private val syncInitiator: SyncInitiator,
  private val syncLogFetcher: SyncLogFetcher,
  private val syncLogUpdater: SyncLogUpdater
) {

  @PostMapping
  fun initiateSync(@RequestBody request: SyncRequestDto): SyncLogResponseDto {
    return syncInitiator.initiate(request.tableName, request.syncMode, SyncInitiateType.USER)
  }

  @GetMapping
  fun getRecentSyncs(@RequestParam(defaultValue = "10") limit: Int): List<SyncLogResponseDto> {
    return syncLogFetcher.findTopN(limit)
  }

  @GetMapping("{syncLogId}")
  fun getSyncProgress(@PathVariable syncLogId: UUID): ResponseEntity<SyncLogResponseDto> {
    val response = syncLogFetcher.getSyncLogById(syncLogId)
    return if (response != null) {
      ResponseEntity.ok(response)
    } else {
      ResponseEntity.noContent().build()
    }
  }

  @PostMapping("{syncLogId}/cancel")
  fun cancelSync(@PathVariable syncLogId: UUID) {
    syncLogUpdater.requestCancellation(syncLogId)
  }
}
