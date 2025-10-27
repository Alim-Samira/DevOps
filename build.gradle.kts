import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
}

subprojects {
    apply(plugin = "java")
    
    repositories {
        mavenCentral()
    }

    // Ensure Java 17 toolchain for java projects
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    // Force release 17 for JavaCompile tasks
    tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
        options.release.set(17)
    }
}