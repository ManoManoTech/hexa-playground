group = "org.hexa-stacks"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":heroesdesk"))
    implementation(libs.jooq)
    implementation(libs.postgresql)

    testImplementation(project(":heroesdesk-test"))
    testImplementation(project(":heroesdesk-inmemory-adapters"))
    testImplementation(libs.jooq.meta)
    testImplementation(libs.jooq.codegen)
    testImplementation(libs.testcontainer.jupiter)
    testImplementation(libs.testcontainer.postgresql)
}
