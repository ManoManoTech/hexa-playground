plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
rootProject.name = "hexa-playground"
include("heroesdesk", "heroesdesk-test", "heroesdesk-inmemory-adapters")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val kotlinVersion = "1.9.21"
            val arrowVersion = "1.2.0"
            val jupiterVersion = "5.9.1"
            val coroutinesVersion = "1.7.3"
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            library("kotlin-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            library("arrow-core", "io.arrow-kt:arrow-core:$arrowVersion")
            library("kotlin-test", "org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
            library("jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
            library("jupiter-api", "org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
        }
    }
}
