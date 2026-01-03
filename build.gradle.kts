
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.testing.Test

plugins {
    java
    id("org.springframework.boot") version "4.0.0" 
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.devops"
version = "0.3"

repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Force release 17 for JavaCompile tasks
tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // JSON parsing for API client
    implementation("com.google.code.gson:gson:2.10.1")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Use Spring Boot's managed test dependencies (keeps JUnit Platform/Jupiter aligned)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform() 
}


tasks.named<Jar>("jar") {

}
