val ksbCommonsVersion: String by project

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
	id("org.springframework.boot") version "3.5.8"
	id("io.spring.dependency-management") version "1.1.7"
    id("com.google.cloud.tools.jib") version "3.5.1"
}

group = "io.jrb.labs"
version = "0.1.0"
description = "Demo project for Spring Boot"

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
                        ?: "brulejr" // fallback, not super important

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
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

springBoot {
    buildInfo()
}

jib {
    from {
        image = "eclipse-temurin:21-jre-jammy"
    }

    to {
        image = "brulejr/rtl433-data-pipeline:${project.version}"
        tags = setOf("latest")
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
                // ⬇⬇ THIS is the key: use setFrom(...) instead of from = ...
                setFrom(file("docker/jib"))
                into = "/opt/docker"

                // permissions is a MapProperty<String, String> in newer Jib
                permissions.set(
                    mapOf("/opt/docker/entrypoint.sh" to "755")
                )
            }
        }
    }
}
