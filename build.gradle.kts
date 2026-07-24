import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.gradle.versions)
}

group = "no.nav.tsm"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

tasks {
    configure<SpotlessExtension> {
        kotlin { ktfmt("0.64").kotlinlangStyle().configure {
            it.setMaxWidth(120)
            it.setContinuationIndent(4)
        } }
        check {
            dependsOn("spotlessApply")
        }
    }
}

dependencies {
    implementation(ktorLibs.serialization.jackson)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.apache5)
    implementation(ktorLibs.client.contentNegotiation)

    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.hayden.khealth)
    implementation(libs.logback.classic)
    implementation(libs.logback.encoder)
    implementation(libs.apache.pdfbox)
    implementation(libs.apache.xmpbox)
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.google.cloud.storage)

    implementation(libs.tsm.sykmeldinger.input)
    implementation(libs.tsm.ktor)

    // old xml libs we need for vedlegg
    implementation(libs.helse.xml.sm2013)
    implementation(libs.helse.xml.fellesformat)
    implementation(libs.helse.xml.hodemelding)
    implementation(libs.helse.xml.apprec)
    implementation(libs.jaxb.api)
    implementation(libs.jaxb.java.time.adapters)
    implementation(libs.jaxb.runtime)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.kafka)
}
