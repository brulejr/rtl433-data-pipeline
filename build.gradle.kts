val ksbCommonsVersion: String by project

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
	id("org.springframework.boot") version "3.5.8"
	id("io.spring.dependency-management") version "1.1.7"
    id("com.google.cloud.tools.jib") version "3.5.1"
}

group = "io.jrb.labs"
version = "0.0.1-SNAPSHOT"
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
