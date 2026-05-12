package com.kyovo

import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestContainer {
    val instance: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine")
        .apply { start() }
}
