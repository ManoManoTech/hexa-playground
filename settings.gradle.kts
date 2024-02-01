plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
rootProject.name = "hexa-playground"
include("heroesdesk", "heroesdesk-test", "heroesdesk-inmemory-adapters", "heroesdesk-missionrepo-jooq-pg-adapter", "heroesdesk-app")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val kotlinVersion = "1.9.21"
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            val coroutinesVersion = "1.7.3"
            library("kotlin-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            library("kotlin-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

            val arrowVersion = "1.2.0"
            library("arrow-core", "io.arrow-kt:arrow-core:$arrowVersion")

            val jupiterVersion = "5.9.1"
            library("kotlin-test", "org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
            library("jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
            library("jupiter-api", "org.junit.jupiter:junit-jupiter-api:$jupiterVersion")

            val jooqVersion = "3.18.5"
            library("jooq", "org.jooq:jooq:$jooqVersion")
            library("jooq-meta", "org.jooq:jooq-meta:$jooqVersion")
            library("jooq-codegen", "org.jooq:jooq-codegen:$jooqVersion")

            library("postgresql", "org.postgresql:postgresql:42.6.0")

            val testContainerVersion = "1.18.3"
            library("testcontainer-jupiter", "org.testcontainers:junit-jupiter:$testContainerVersion")
            library("testcontainer-postgresql", "org.testcontainers:postgresql:$testContainerVersion")

            val springBootVersion = "3.2.2"
            library("spring-boot-starter-web", "org.springframework.boot:spring-boot-starter-web:$springBootVersion")
            library("spring-boot-starter-graphql", "org.springframework.boot:spring-boot-starter-graphql:$springBootVersion")
            library("spring-boot-starter-test", "org.springframework.boot:spring-boot-starter-test:$springBootVersion")

            library("logback-classic", "ch.qos.logback:logback-classic:1.2.6")
        }
    }
}
