import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.tasks.Jar

plugins {
    java
    application
}

group = "com.devops"
version = "0.2.0"

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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Configure the application entry point
application {
    // Main class is in the default package
    mainClass.set("Main")
}

// Make the built JAR executable with `java -jar`
tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "Main"
    }

}

// Attach standard input to the Gradle run task for interactive apps
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
