import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class QuoteTest {

    @Test
    fun `should create quote with isin and price`() {
        val quote = Quote("DE0007164600", BigDecimal("125.4567"))

        assertEquals("DE0007164600", quote.isin)
        assertEquals(BigDecimal("125.4567"), quote.price)
    }

    @Test
    fun `should convert to JSON correctly`() {
        val quote = Quote("DE0007164600", BigDecimal("125.4567"))
        val json = quote.asJson()

        assertEquals("DE0007164600", json.getString("isin"))
        assertEquals(125.4567, json.getDouble("price"), 0.0001)
    }

    @Test
    fun `should handle zero price`() {
        val quote = Quote("DE0007164600", BigDecimal.ZERO)
        val json = quote.asJson()

        assertEquals(0.0, json.getDouble("price"), 0.0001)
    }

    @Test
    fun `should handle large price values`() {
        val quote = Quote("DE0007164600", BigDecimal("999999.9999"))
        val json = quote.asJson()

        assertEquals(999999.9999, json.getDouble("price"), 0.0001)
    }

    @Test
    fun `should be equal when isin and price match`() {
        val quote1 = Quote("DE0007164600", BigDecimal("125.45"))
        val quote2 = Quote("DE0007164600", BigDecimal("125.45"))

        assertEquals(quote1, quote2)
    }
}
