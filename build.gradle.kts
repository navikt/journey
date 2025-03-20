plugins {
	kotlin("jvm") version "2.1.20"
	kotlin("plugin.spring") version "2.1.20"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "no.nav.tsm"
version = "0.0.2"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
	shadowJar {
		archiveBaseName.set("app")
		archiveClassifier.set("")
		isZip64 = true
		manifest {
			attributes["Main-Class"] = "no.nav.tsm.mottak.ApplicationKt"
		}
	}

	bootJar {
		archiveFileName = "app.jar"
	}
}
