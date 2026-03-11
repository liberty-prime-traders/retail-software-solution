package me.ezra_home.retail_software_solution.util.enums

import java.util.UUID

enum class ServiceAccount(val uniqueId: UUID) {
    CATALOG_SYNC(UUID.fromString("11111111-1111-1111-1111-111111111111")),
    RECORD_INITIALIZER(UUID.fromString("22222222-2222-2222-2222-222222222222")),
    SECURITY_MONITOR(UUID.fromString("33333333-3333-3333-3333-333333333333")),
    INVENTORY_PROCESSOR(UUID.fromString("44444444-4444-4444-4444-444444444444")),
}
