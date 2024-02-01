apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

group = "org.hexa-stacks"
version = "1.0-SNAPSHOT"

dependencies{
    implementation(project(":heroesdesk"))
    implementation(project(":heroesdesk-inmemory-adapters"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.graphql)

    testImplementation(project(":heroesdesk-test"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.serialization.json)
}