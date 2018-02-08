package be.jessa.ws.okapi.logging

import org.slf4j.Logger

inline fun Logger.debug(messageGenerator: () -> String) {
    if (this.isDebugEnabled) {
        this.debug(messageGenerator())
    }
}

inline fun Logger.info(messageGenerator: () -> String) {
    if (this.isInfoEnabled) {
        this.info(messageGenerator())
    }
}