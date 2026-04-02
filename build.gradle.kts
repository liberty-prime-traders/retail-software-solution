plugins {
	kotlin("kapt") version "1.9.25"
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
}

ext["testcontainers.version"] = "1.20.6"

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
	implementation("org.hibernate.orm:hibernate-envers:6.5.3.Final")
	implementation("org.springframework.kafka:spring-kafka")

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

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "com.okta.spring", module = "okta-spring-boot-starter")
	}
	testImplementation("com.okta.spring:okta-spring-sdk:3.0.7")
	testImplementation("org.springframework.security:spring-security-config")
	testImplementation("org.springframework.security:spring-security-web")
	testImplementation("org.springframework.security:spring-security-oauth2-resource-server")
	testImplementation("org.springframework.security:spring-security-oauth2-jose")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.h2database:h2")
	testImplementation("io.cucumber:cucumber-java:7.18.1")
	testImplementation("io.cucumber:cucumber-spring:7.18.1")
	testImplementation("io.cucumber:cucumber-junit-platform-engine:7.18.1")
	testImplementation("io.rest-assured:rest-assured:5.4.0")
	testImplementation("io.rest-assured:kotlin-extensions:5.4.0")
	testImplementation("org.junit.platform:junit-platform-suite-api:1.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-suite-engine:1.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:kafka")
	testImplementation("org.awaitility:awaitility-kotlin:4.2.1")

}

configurations.testRuntimeClasspath {
	exclude(group = "com.okta.spring", module = "okta-spring-boot-starter")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

kapt {
	correctErrorTypes = true
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("cucumber.junit-platform.naming-strategy", "long")
	systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags") ?: "not @ignore")
	systemProperty("okta.oauth2.enabled", "false")
	systemProperty("okta.client.enabled", "false")
}

fun registerCucumberLaneTask(taskName: String, tagExpression: String, descriptionText: String) {
	tasks.register<Test>(taskName) {
		group = "verification"
		description = descriptionText
		useJUnitPlatform()
		filter {
			includeTestsMatching("me.ezra_home.retail_software_solution.cucumber.RunCucumberTest")
		}
		systemProperty("cucumber.filter.tags", tagExpression)
		systemProperty("cucumber.junit-platform.naming-strategy", "long")
		systemProperty("okta.oauth2.enabled", "false")
		systemProperty("okta.client.enabled", "false")
		systemProperty("spring.profiles.active", "test")
	}
}

registerCucumberLaneTask(
	"cucumberSmokeTest",
	"@smoke and not @ignore",
	"Runs only cucumber smoke scenarios."
)

registerCucumberLaneTask(
	"cucumberKafkaProducerTest",
	"@kafka-producer and not @kafka-consumer and not @ignore",
	"Runs cucumber kafka producer scenarios."
)

registerCucumberLaneTask(
	"cucumberKafkaConsumerTest",
	"@kafka-consumer and not @ignore",
	"Runs cucumber kafka consumer scenarios."
)

registerCucumberLaneTask(
	"cucumberRegressionTest",
	"not @ignore",
	"Runs the full cucumber regression suite."
)

tasks.matching { it.name == "kaptTestKotlin" }.configureEach {
	enabled = false
}
