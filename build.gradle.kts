val kafkaClientsVersion = "3.9.0"
val logstashLogbackEncoderVersion = "8.0"
val jacksonVersion= "2.17.2"
val tokenSupportVersion = "5.0.19"
val googleCloudStorageVersion = "2.50.0"
val prometheusVersion = "0.16.0"
val mockitoKotlinVersion = "5.4.0"
val testContainersVersion = "1.20.6"
val mockkVersion = "1.13.17"

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.nav.tsm"
version = "0.0.2"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.hibernate.validator:hibernate-validator")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
	implementation("io.prometheus:simpleclient_common:$prometheusVersion")
	implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
	implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
	implementation("no.nav.security:token-support:$tokenSupportVersion")
	implementation("no.nav.security:token-validation-core:$tokenSupportVersion")
	implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
	implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
	implementation("com.google.cloud:google-cloud-storage:$googleCloudStorageVersion")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
	testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
	testImplementation("io.mockk:mockk:$mockkVersion")
	testImplementation(kotlin("test"))
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
	bootJar {
		archiveFileName = "app.jar"
	}
	test {
		useJUnitPlatform()
		testLogging {
			events("skipped", "failed")
			showStackTraces = true
			exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		}
	}
}
