package gg.essential.installer.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logging {

    // Make sure this doesn't initialize anything before we set the system property for log4j
    val logger: Logger by lazy { LoggerFactory.getLogger("Installer") }

}
