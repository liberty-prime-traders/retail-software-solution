package me.ezra_home.retail_software_solution.cucumber.support

import java.util.UUID

object TestConstants {

  val DEFAULT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

  const val DEFAULT_ORG_SCHEMA = "public"
  const val DEFAULT_LOCATION_SCHEMA = "public"

  object Tokens {
    const val PLATFORM_ADMIN = "mock-platform-admin-token"
    const val ORG_ADMIN = "mock-org-admin-token"
    const val ORG_USER = "mock-user-token"
  }

  object Timeouts {
    const val KAFKA_EVENT_MS = 15_000L
    const val KAFKA_SYNC_MS = 20_000L
    const val KAFKA_POLL_INTERVAL_MS = 500L
  }
}
