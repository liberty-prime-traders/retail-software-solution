package me.ezra_home.retail_software_solution.support

object TestConstants {

  object Tokens {
    const val TOKEN_HEADER = "X-Auth-Token"
    const val PLATFORM_ADMIN = "mock-platform-admin-token"
    const val ORG_USER = "mock-user-token"
  }

  object Okta {
    const val PLATFORM_USER = "SERVICE_ACCOUNT_RECORD_INITIALIZER"
    const val ORGANIZATION_USER = "okta-organization-user"
  }

  object Timeouts {
    const val KAFKA_EVENT_MS = 15_000L
    const val KAFKA_SYNC_MS = 20_000L
    const val KAFKA_POLL_INTERVAL_MS = 500L
  }

  object Seed {
    const val ORG_SCHEMA = "org_test"
    const val LOCATION_SCHEMA = "loc_test"
  }

}
