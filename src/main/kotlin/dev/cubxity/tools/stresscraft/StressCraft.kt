package dev.cubxity.tools.stresscraft

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class StressCraft(
    val host: String,
    val port: Int,
    val options: StressCraftOptions
) {
    private val executor = Executors.newScheduledThreadPool(2)
    private val terminal = Terminal()
    private var id = 0

    val sessions = AtomicInteger()
    val activeSessions = AtomicInteger()
    val chunksLoaded = AtomicInteger()

    fun start() {
        Runtime.getRuntime().addShutdownHook(Thread {
            executeShutdownHook()
        })

        executor.scheduleAtFixedRate({
            val sessions = sessions.get()
            val activeSessions = activeSessions.get()
            if (sessions < options.count && sessions - activeSessions < options.buffer) {
                createSession()
            }
        }, 0, options.delay.toLong(), TimeUnit.MILLISECONDS)

        // Render at 10 FPS
        // Some terminals may be too slow to handle high frequency updates
        executor.scheduleAtFixedRate({
            renderProgress()
        }, 0, 100, TimeUnit.MILLISECONDS)
    }

    private fun createSession() {
        val name = options.prefix + "${id++}".padStart(4, '0')
        Session(this).connect(name)
    }

    private fun renderProgress() {
        terminal.renderHeader(host, port)
        terminal.renderInfo("\uD83D\uDCE6 Chunks", chunksLoaded.get())
        terminal.newLine()
        terminal.renderBar(sessions.get(), options.count, "Connections")
        terminal.renderBar(activeSessions.get(), options.count, "Players")
        terminal.renderBar(100, 100, "Michael Appreciation")
        terminal.reset()
    }

    private fun executeShutdownHook() {
        executor.shutdownNow()
        terminal.close()
    }
}

fun main(args: Array<String>) {
    val parser = ArgParser("stresscraft")
    val host by parser.argument(ArgType.String, description = "the IP address or the hostname of the server")
    val port by parser.argument(ArgType.Int, description = "the port of the server")
        .optional().default(25565)
    val count by parser.option(ArgType.Int, "count", "c", description = "the amount of bots")
        .default(500)
    val delay by parser.option(ArgType.Int, "delay", "d", description = "delay between connections, in ms")
        .default(20)
    val buffer by parser.option(ArgType.Int, "buffer", "b", description = "buffer between connections and players")
        .default(20)
    val prefix by parser.option(ArgType.String, "prefix", "p", description = "player name prefix")
        .default("Player")
    val simulate by parser.option(ArgType.Boolean, "simulate", "s", description = "use player simulation (not implemented)")
        .default(true)

    parser.parse(args)

    val options = StressCraftOptions(count, delay, buffer, prefix, simulate)
    StressCraft(host, port, options).start()
}
