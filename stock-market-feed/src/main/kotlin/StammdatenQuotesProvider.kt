import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

// Configuration
var minNumberOfInstruments: String = "5"
var maxNumberOfInstruments: String = "10"
const val avgMillisecondsBetweenQuotes: Int = 100

// Real Stock Data
val STOCK_LIST = listOf(
    // US Stocks
    "NVDA" to "NVIDIA Corporation",
    "AAPL" to "Apple Inc.",
    "GOOGL" to "Alphabet Inc.",
    "MSFT" to "Microsoft Corporation",
    "AMZN" to "Amazon.com, Inc.",
    "META" to "Meta Platforms, Inc.",
    "AVGO" to "Broadcom Inc.",
    "TSLA" to "Tesla, Inc.",
    "NFLX" to "Netflix, Inc.",
    "ASML" to "ASML Holding N.V.",
    // Indian Stocks
    "RELIANCE" to "Reliance Industries Ltd",
    "HDFCBANK" to "HDFC Bank Ltd",
    "BHARTIARTL" to "Bharti Airtel Ltd",
    "TCS" to "Tata Consultancy Services Ltd",
    "ICICIBANK" to "ICICI Bank Ltd",
    "SBIN" to "State Bank of India",
    "INFY" to "Infosys Ltd",
    "BAJFINANCE" to "Bajaj Finance Ltd",
    "LT" to "Larsen and Toubro Ltd",
    "HINDUNILVR" to "Hindustan Unilever Ltd",
    "MARUTI" to "Maruti Suzuki India Ltd",
    "ITC" to "ITC Ltd",
    "HCLTECH" to "HCL Technologies Ltd",
    "M&M" to "Mahindra and Mahindra Ltd",
    "KOTAKBANK" to "Kotak Mahindra Bank Ltd",
    "SUNPHARMA" to "Sun Pharmaceutical Industries Ltd",
    "AXISBANK" to "Axis Bank Ltd",
    "ULTRACEMCO" to "UltraTech Cement Ltd",
    "TITAN" to "Titan Company Ltd",
    "BAJAJFINSV" to "Bajaj Finserv Ltd",
    "ADANIPORTS" to "Adani Ports and Special Economic Zone Ltd",
    "NTPC" to "NTPC Ltd",
    "ONGC" to "Oil and Natural Gas Corporation Ltd",
    "BEL" to "Bharat Electronics Ltd",
    "WIPRO" to "Wipro Ltd",
    "JSWSTEEL" to "JSW Steel Ltd",
    "ETERNAL" to "Eternal Ltd",
    "ASIANPAINT" to "Asian Paints Ltd",
    "ADANIENT" to "Adani Enterprises Ltd",
    "BAJAJ-AUTO" to "Bajaj Auto Ltd",
    "POWERGRID" to "Power Grid Corporation of India Ltd",
    "NESTLEIND" to "Nestle India Ltd",
    "COALINDIA" to "Coal India Ltd",
    "TATASTEEL" to "Tata Steel Ltd",
    "SBILIFE" to "SBI Life Insurance Company Ltd",
    "EICHERMOT" to "Eicher Motors Ltd",
    "INDIGO" to "Interglobe Aviation Ltd",
    "GRASIM" to "Grasim Industries Ltd",
    "JIOFIN" to "Jio Financial Services Ltd",
    "HINDALCO" to "Hindalco Industries Ltd"
)

// Utility functions
fun randomInt(bound: Int): Int = Random.nextInt(bound)

fun randomDigit(): Int = Random.nextInt(10)

fun randomAZChar(): Char = ('A'.code + Random.nextInt(26)).toChar()

fun randomDigitOrChar(): Char = if (Random.nextBoolean()) ('0' + randomDigit()) else randomAZChar()

fun generateIsin(): String {
    val sb = StringBuilder()
    // First 2 chars are letters (country code)
    sb.append(randomAZChar())
    sb.append(randomAZChar())
    // Next 10 chars are alphanumeric
    repeat(10) {
        sb.append(randomDigitOrChar())
    }
    return sb.toString()
}

fun randomBetween(min: Double, max: Double): Double = min + Random.nextDouble() * (max - min)

// Interface for JSON serializable objects
interface JsonPrintable {
    fun asJson(): JSONObject
}

// Data Models
data class Instrument(
    val isin: String,
    val description: String
) : JsonPrintable {
    override fun asJson(): JSONObject = JSONObject()
        .put("isin", isin)
        .put("description", description)
}

data class Quote(
    val isin: String,
    val price: BigDecimal
) : JsonPrintable {
    override fun asJson(): JSONObject = JSONObject()
        .put("isin", isin)
        .put("price", price.toDouble())
}

enum class MessageType {
    ADD,
    DELETE,
    QUOTE
}

data class WebsocketMessage(
    val type: MessageType,
    val data: JsonPrintable
) : JsonPrintable {
    override fun asJson(): JSONObject = JSONObject()
        .put("type", type.name)
        .put("data", data.asJson())
}

// Random Walk Price Generator
class RandomWalkInstrument(val instrument: Instrument) {
    private var step: Long = 0
    private var startPrice: Int = Random.nextInt(50, 500)
    private var targetPrice: Int = generateTargetPrice()
    private var targetSteps: Int = Random.nextInt(10, 50)
    private var lastPrice: BigDecimal = BigDecimal(startPrice)
    private var stepWidth: Double = calculateStepWidth()

    private fun generateTargetPrice(): Int {
        val change = Random.nextInt(-50, 51)
        return (startPrice + change).coerceAtLeast(1)
    }

    private fun calculateStepWidth(): Double {
        return if (targetSteps > 0) {
            (targetPrice - startPrice).toDouble() / targetSteps
        } else {
            0.0
        }
    }

    private fun reset() {
        startPrice = lastPrice.toInt()
        targetPrice = generateTargetPrice()
        targetSteps = Random.nextInt(10, 50)
        step = 0
        stepWidth = calculateStepWidth()
    }

    fun nextPrice(): BigDecimal {
        step++
        if (step >= targetSteps) {
            reset()
        }

        val noise = randomBetween(-0.5, 0.5)
        lastPrice = lastPrice.add(BigDecimal(stepWidth + noise))

        if (lastPrice < BigDecimal.ONE) {
            lastPrice = BigDecimal.ONE
        }

        return lastPrice.setScale(4, RoundingMode.HALF_UP)
    }
}

// WebSocket Servers
class QuotesWebSocketServer {
    private val sockets = CopyOnWriteArrayList<WebSocketServerSession>()

    suspend fun sendPrice(quote: Quote) {
        val message = WebsocketMessage(MessageType.QUOTE, quote)
        val json = message.asJson().toString()
        sockets.forEach { socket ->
            try {
                socket.send(Frame.Text(json))
            } catch (e: Exception) {
                // Socket might be closed
            }
        }
    }

    suspend fun addSocket(socket: WebSocketServerSession) {
        sockets.add(socket)
    }

    suspend fun removeSocket(socket: WebSocketServerSession) {
        sockets.remove(socket)
    }
}

class InstrumentWebSocketServer {
    private val instruments = CopyOnWriteArrayList<Instrument>()
    private val sockets = CopyOnWriteArrayList<WebSocketServerSession>()

    suspend fun addSocket(socket: WebSocketServerSession) {
        sockets.add(socket)
        // Send all current instruments to the new socket
        instruments.forEach { instrument ->
            val message = WebsocketMessage(MessageType.ADD, instrument)
            try {
                socket.send(Frame.Text(message.asJson().toString()))
            } catch (e: Exception) {
                // Socket might be closed
            }
        }
    }

    suspend fun removeSocket(socket: WebSocketServerSession) {
        sockets.remove(socket)
    }

    suspend fun addInstrument(instrument: Instrument) {
        instruments.add(instrument)
        val message = WebsocketMessage(MessageType.ADD, instrument)
        val json = message.asJson().toString()
        sockets.forEach { socket ->
            try {
                socket.send(Frame.Text(json))
            } catch (e: Exception) {
                // Socket might be closed
            }
        }
    }

    suspend fun deleteInstrument(instrument: Instrument) {
        instruments.remove(instrument)
        val message = WebsocketMessage(MessageType.DELETE, instrument)
        val json = message.asJson().toString()
        sockets.forEach { socket ->
            try {
                socket.send(Frame.Text(json))
            } catch (e: Exception) {
                // Socket might be closed
            }
        }
    }
}

// Generator - The Orchestrator
class Generator(
    private val instrumentServer: InstrumentWebSocketServer,
    private val quotesServer: QuotesWebSocketServer
) {
    private var instruments = mutableListOf<RandomWalkInstrument>()
    private val availableStocks = STOCK_LIST.toMutableList()
    private val activeSymbols = mutableSetOf<String>()

    init {
        // Start the generator in a background coroutine
        GlobalScope.launch {
            // Add initial instruments
            val initialCount = minNumberOfInstruments.toIntOrNull() ?: 5
            repeat(initialCount) {
                addInstrument()
            }

            // Main loop
            while (true) {
                val minInstruments = minNumberOfInstruments.toIntOrNull() ?: 5
                val maxInstruments = maxNumberOfInstruments.toIntOrNull() ?: 10

                // Maybe add a new instrument (if we have available stocks)
                if (Random.nextDouble() < 0.1 && instruments.size < maxInstruments && availableStocks.isNotEmpty()) {
                    addInstrument()
                }

                // Maybe remove an instrument
                if (Random.nextDouble() < 0.05 && instruments.size > minInstruments) {
                    removeInstrument()
                }

                // Generate a price for a random instrument
                if (instruments.isNotEmpty()) {
                    val randomInstrument = instruments[randomInt(instruments.size)]
                    val newPrice = randomInstrument.nextPrice()
                    val quote = Quote(randomInstrument.instrument.isin, newPrice)
                    quotesServer.sendPrice(quote)
                }

                delay(avgMillisecondsBetweenQuotes.toLong())
            }
        }
    }

    private suspend fun addInstrument() {
        if (availableStocks.isEmpty()) return

        // Pick a random stock from available list
        val index = randomInt(availableStocks.size)
        val (symbol, name) = availableStocks.removeAt(index)
        activeSymbols.add(symbol)

        val instrument = Instrument(symbol, name)
        val randomWalkInstrument = RandomWalkInstrument(instrument)
        instruments.add(randomWalkInstrument)
        instrumentServer.addInstrument(instrument)
    }

    private suspend fun removeInstrument() {
        if (instruments.isNotEmpty()) {
            val index = randomInt(instruments.size)
            val removed = instruments.removeAt(index)

            // Return stock to available pool
            val symbol = removed.instrument.isin
            activeSymbols.remove(symbol)
            availableStocks.add(symbol to removed.instrument.description)

            instrumentServer.deleteInstrument(removed.instrument)
        }
    }
}

// Ktor Application Module
fun Application.mainModule() {
    install(WebSockets)

    val instrumentServer = InstrumentWebSocketServer()
    val quotesServer = QuotesWebSocketServer()

    // Start the generator
    Generator(instrumentServer, quotesServer)

    routing {
        // Serve static files from resources/static
        static("/") {
            resources("static")
            defaultResource("index.html", "static")
        }

        webSocket("/instruments") {
            instrumentServer.addSocket(this)
            try {
                for (frame in incoming) {
                    // Keep connection alive, we don't process incoming messages
                }
            } finally {
                instrumentServer.removeSocket(this)
            }
        }

        webSocket("/quotes") {
            quotesServer.addSocket(this)
            try {
                for (frame in incoming) {
                    // Keep connection alive, we don't process incoming messages
                }
            } finally {
                quotesServer.removeSocket(this)
            }
        }
    }
}

