import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class InstrumentTest {

    @Test
    fun `should create instrument with isin and description`() {
        val instrument = Instrument("DE0007164600", "SAP SE")

        assertEquals("DE0007164600", instrument.isin)
        assertEquals("SAP SE", instrument.description)
    }

    @Test
    fun `should convert to JSON correctly`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val json = instrument.asJson()

        assertEquals("DE0007164600", json.getString("isin"))
        assertEquals("SAP SE", json.getString("description"))
    }

    @Test
    fun `should support data class copy`() {
        val original = Instrument("DE0007164600", "SAP SE")
        val copy = original.copy(description = "SAP AG")

        assertEquals("DE0007164600", copy.isin)
        assertEquals("SAP AG", copy.description)
    }

    @Test
    fun `should be equal when isin and description match`() {
        val instrument1 = Instrument("DE0007164600", "SAP SE")
        val instrument2 = Instrument("DE0007164600", "SAP SE")

        assertEquals(instrument1, instrument2)
        assertEquals(instrument1.hashCode(), instrument2.hashCode())
    }

    @Test
    fun `should not be equal when isin differs`() {
        val instrument1 = Instrument("DE0007164600", "SAP SE")
        val instrument2 = Instrument("US0378331005", "SAP SE")

        assertNotEquals(instrument1, instrument2)
    }
}
