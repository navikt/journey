package no.nav.journey

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JourneyApplication

fun main(args: Array<String>) {
	runApplication<JourneyApplication>(*args)
}
