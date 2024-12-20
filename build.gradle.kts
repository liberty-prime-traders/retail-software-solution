plugins {
	kotlin("kapt") version "1.9.25"
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "me.ezra-home"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	gradlePluginPortal()
}

dependencies {
	implementation("com.okta.spring:okta-spring-boot-starter:3.0.7")
	implementation("com.okta.spring:okta-spring-sdk")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("org.liquibase:liquibase-core")
	runtimeOnly("org.postgresql:postgresql")

	implementation("com.google.guava:guava:33.3.1-jre")
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
	implementation("org.springframework.boot:spring-boot-starter-cache")

	implementation("org.mapstruct:mapstruct:1.6.3")
	kapt("org.mapstruct:mapstruct-processor:1.6.3")

	compileOnly("org.projectlombok:lombok")
	kapt("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

