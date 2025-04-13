package net.eyecu.dyn

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Route53a4k {

    val logger: Logger = LoggerFactory.getLogger("route53a4k")

    @JvmStatic
    fun main(args: Array<String>) {
        val configuration = Configuration()
        val application = Application()

        Route53a4kApplication()
            .subcommands(
                Init(configuration),
                Schedule(application, configuration),
                Run(application, configuration)
            ).main(args)
    }
}