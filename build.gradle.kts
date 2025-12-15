// build.gradle.kts (MODIFIÉ)

import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.tasks.Jar
import org.gradle.api.tasks.testing.Test

plugins {
    java
    id("org.springframework.boot") version "4.0.0" // Version à conserver si elle compile sans erreur
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.devops"
version = "0.2.0"

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

// Add JUnit 5 for tests
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

// JSON parsing for API client
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// Ensure the Jupiter engine is available at runtime
dependencies {
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}
// Add JUnit Platform launcher in case Gradle needs it on the runtime classpath
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform() 
}


tasks.named<Jar>("jar") {

}
