val kafkaClientsVersion = "3.9.1"
val logstashLogbackEncoderVersion = "9.0"
val jacksonVersion= "2.21.0"
val googleCloudStorageVersion = "2.62.0"
val prometheusVersion = "0.16.0"
val mockitoKotlinVersion = "6.2.2"
val testContainersVersion = "1.21.4"
val mockkVersion = "1.14.7"
val syfoXmlCodegenVersion = "2.0.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val jaxbApiVersion = "2.4.0-b180830.0359"
val javaTimeAdapterVersion = "1.1.3"
val pdfgencoreVersion = "1.1.69"
val verapdfVersion = "1.28.2"
val openHtmlToPdfVersion = "1.1.37"
val handlebarsVersion = "4.5.0"
val opentelemetryLogbackMdcVersion = "2.24.0-alpha"
val tikaVersion = "3.2.3"
val sykmelidngInputVersion = "23"

plugins {
	kotlin("jvm") version "2.3.0"
	kotlin("plugin.spring") version "2.3.0"
	id("org.springframework.boot") version "4.0.1"
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
	implementation("no.nav.pdfgen:pdfgen-core:$pdfgencoreVersion")
	implementation("org.verapdf:validation-model:$verapdfVersion")
	implementation("com.github.jknack:handlebars:$handlebarsVersion")
	implementation("io.github.openhtmltopdf:openhtmltopdf-slf4j:${openHtmlToPdfVersion}")
	implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:$opentelemetryLogbackMdcVersion")
	implementation("no.nav.tsm.sykmelding", "input", sykmelidngInputVersion)

	testImplementation("org.apache.tika:tika-core:$tikaVersion")
	testImplementation("org.apache.tika:tika-parsers-standard-package:$tikaVersion")
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
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
