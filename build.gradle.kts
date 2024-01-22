plugins {
    kotlin("jvm") version "1.9.21"
    id("test-report-aggregation")
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

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    tasks.check {
        dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
    }

}

