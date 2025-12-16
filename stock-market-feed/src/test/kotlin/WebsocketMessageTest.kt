import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class WebsocketMessageTest {

    @Test
    fun `should create ADD message for instrument`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val message = WebsocketMessage(MessageType.ADD, instrument)

        assertEquals(MessageType.ADD, message.type)
        assertEquals(instrument, message.data)
    }

    @Test
    fun `should create DELETE message for instrument`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val message = WebsocketMessage(MessageType.DELETE, instrument)

        assertEquals(MessageType.DELETE, message.type)
    }

    @Test
    fun `should create QUOTE message`() {
        val quote = Quote("DE0007164600", BigDecimal("125.45"))
        val message = WebsocketMessage(MessageType.QUOTE, quote)

        assertEquals(MessageType.QUOTE, message.type)
    }

    @Test
    fun `should convert ADD message to JSON correctly`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val message = WebsocketMessage(MessageType.ADD, instrument)
        val json = message.asJson()

        assertEquals("ADD", json.getString("type"))
        assertTrue(json.has("data"))
        assertEquals("DE0007164600", json.getJSONObject("data").getString("isin"))
    }

    @Test
    fun `should convert QUOTE message to JSON correctly`() {
        val quote = Quote("DE0007164600", BigDecimal("125.45"))
        val message = WebsocketMessage(MessageType.QUOTE, quote)
        val json = message.asJson()

        assertEquals("QUOTE", json.getString("type"))
        assertTrue(json.has("data"))
        assertEquals(125.45, json.getJSONObject("data").getDouble("price"), 0.01)
    }
}
