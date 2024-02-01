import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinToolingVersion = "1.9.22"
    kotlin("jvm") version  kotlinToolingVersion
    id("test-report-aggregation")
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("plugin.spring") version kotlinToolingVersion

}

group = "org.hexastacks"
version = "1.0-SNAPSHOT"

allprojects {

    repositories {
        mavenLocal()
        mavenCentral()
    }

}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "21"
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    tasks.check {
        dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
    }

}

