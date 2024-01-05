plugins {
    kotlin("jvm") version "1.9.21"
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

    tasks.test {
        useJUnitPlatform()
    }
}

