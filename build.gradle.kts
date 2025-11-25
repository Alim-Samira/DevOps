import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    application
}

repositories {
    mavenCentral()
}

// Ensure Java 17 toolchain for the project
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

// Ensure the Jupiter engine is available at runtime
dependencies {
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}
// Add JUnit Platform launcher in case Gradle needs it on the runtime classpath
dependencies {
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass = "Main"
}
