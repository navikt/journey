val kafkaClientsVersion = "7.9.0-ce"
val logstashLogbackEncoderVersion = "8.1"
val jacksonVersion= "2.18.3"
val tokenSupportVersion = "5.0.19"
val googleCloudStorageVersion = "2.50.0"
val prometheusVersion = "0.16.0"
val mockitoKotlinVersion = "5.4.0"
val testContainersVersion = "1.20.6"
val mockkVersion = "1.14.0"
val syfoXmlCodegenVersion = "2.0.1"
val jaxbRuntimeVersion = "4.0.5"
val jaxbApiVersion = "2.4.0-b180830.0359"
val javaTimeAdapterVersion = "1.1.3"

plugins {
	kotlin("jvm") version "2.1.20"
	kotlin("plugin.spring") version "2.1.20"
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.nav.tsm"
version = "0.0.2"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
	mavenCentral()
	maven(url = "https://packages.confluent.io/maven/")
	maven {
		url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
	}
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
	implementation("no.nav.helse.xml:sm2013:$syfoXmlCodegenVersion")
	implementation("no.nav.helse.xml:xmlfellesformat:$syfoXmlCodegenVersion")
	implementation("no.nav.helse.xml:kith-hodemelding:$syfoXmlCodegenVersion")
	implementation("no.nav.helse.xml:kith-apprec:$syfoXmlCodegenVersion")
	implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
	implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
	implementation("com.migesok", "jaxb-java-time-adapters", javaTimeAdapterVersion)
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
