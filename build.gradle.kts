val kafkaClientsVersion = "3.9.0"
val logstashLogbackEncoderVersion = "8.0"
val jackson_version= "2.17.2"

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.nav.tsm"
version = "0.0.2"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
	implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
	bootJar {
		archiveFileName = "app.jar"
	}
}
