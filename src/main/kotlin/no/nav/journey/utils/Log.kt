package no.nav.journey.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.applog(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}
inline fun <reified T> T.securelog(): Logger {
    return LoggerFactory.getLogger("securelog")
}



