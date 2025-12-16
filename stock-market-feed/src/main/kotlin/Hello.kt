import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class Hello : CliktCommand(
    help = "Stock Market Feed Server - Simulates real-time stock price updates via WebSocket"
) {
    private val port: Int by option("-p", "--port", help = "Port to run the server on")
        .int()
        .default(8080)

    private val min: Int by option("--min", help = "Minimum number of instruments")
        .int()
        .default(5)

    private val max: Int by option("--max", help = "Maximum number of instruments")
        .int()
        .default(10)

    override fun run() {
        minNumberOfInstruments = min.toString()
        maxNumberOfInstruments = max.toString()

        println("Starting Stock Market Feed Server...")
        println("Port: $port")
        println("Min instruments: $min")
        println("Max instruments: $max")
        println()
        println("WebSocket endpoints:")
        println("  ws://localhost:$port/instruments - Instrument add/delete events")
        println("  ws://localhost:$port/quotes - Real-time price quotes")
        println()

        embeddedServer(Netty, port = port) {
            mainModule()
        }.start(wait = true)
    }
}

fun main(args: Array<String>) = Hello().main(args)
