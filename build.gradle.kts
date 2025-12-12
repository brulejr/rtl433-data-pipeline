import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ksbCommonsVersion: String by project
val projectVersion: String by project

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.cloud.tools.jib") version "3.5.1"
}

group = "io.jrb.labs"
description = "RTL433 data processing pipeline for rtl_433 MQTT events"

/**
 * Resolve the project version:
 *
 * - In CI on a tag build, GITHUB_REF_NAME will be something like "v0.3.1".
 *   We strip the leading "v" and use "0.3.1" as the version.
 * - Otherwise, fall back to projectVersion from gradle.properties
 */
version = System.getenv("GITHUB_REF_NAME")
    ?.let { refName ->
        // In GitHub Actions, ref_name is already just "v0.3.1"
        if (refName.matches(Regex("""v\d+\.\d+\.\d+"""))) {
            refName.removePrefix("v")
        } else {
            projectVersion
        }
    }
    ?: projectVersion

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/brulejr/ksb-commons")
        credentials {
            // Local dev: ~/.gradle/gradle.properties
            username = findProperty("gpr.user") as String?
                ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.key") as String?
                ?: System.getenv("GITHUB_TOKEN")
                        ?: System.getenv("GITHUB_PACKAGES_TOKEN")
        }
    }
}

dependencies {
    implementation(platform("io.jrb.labs:ksb-dependency-bom:$ksbCommonsVersion"))

    implementation("io.jrb.labs:ksb-spring-boot-starter-reactive")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("io.github.reactivecircus.cache4k:cache4k:0.14.0")

    testImplementation("io.jrb.labs:ksb-spring-boot-starter-reactive-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test> {
    useJUnitPlatform()
}

springBoot {
    // info.app.version will now be aligned with tag-derived project.version
    buildInfo()
}

jib {
    from {
        image = "eclipse-temurin:21-jre-jammy"
    }
    to {
        // Use GHCR and the current repo by default
        val repo = System.getenv("GITHUB_REPOSITORY") ?: "brulejr/rtl433-data-pipeline"
        image = "ghcr.io/$repo"

        // Tag with the Gradle project version (derived from tag) and "latest"
        tags = setOf(project.version.toString(), "latest")

        // Auth for CI (GitHub Actions)
        auth {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        user = "1000:1000"
        ports = listOf("5001")
        environment = mapOf(
            "APP_MAIN_CLASS" to "io.jrb.labs.rtl433dp.Rtl433DataPipelineApplicationKt"
        )
        entrypoint = listOf("/bin/bash", "/opt/docker/entrypoint.sh")
    }
    extraDirectories {
        paths {
            path {
                // Use setFrom(...) for newer Jib versions
                setFrom(file("docker/jib"))
                into = "/opt/docker"
                permissions.set(
                    mapOf("/opt/docker/entrypoint.sh" to "755")
                )
            }
        }
    }
}
