import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class RandomWalkInstrumentTest {

    @Test
    fun `should create with instrument`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val randomWalk = RandomWalkInstrument(instrument)

        assertEquals(instrument, randomWalk.instrument)
    }

    @Test
    fun `should generate positive prices`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val randomWalk = RandomWalkInstrument(instrument)

        repeat(100) {
            val price = randomWalk.nextPrice()
            assertTrue(price > BigDecimal.ZERO, "Price should be positive: $price")
        }
    }

    @Test
    fun `should generate prices with 4 decimal places`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val randomWalk = RandomWalkInstrument(instrument)

        repeat(10) {
            val price = randomWalk.nextPrice()
            assertEquals(4, price.scale(), "Price should have 4 decimal places")
        }
    }

    @Test
    fun `should generate varying prices over time`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val randomWalk = RandomWalkInstrument(instrument)

        val prices = mutableSetOf<BigDecimal>()
        repeat(50) {
            prices.add(randomWalk.nextPrice())
        }

        assertTrue(prices.size > 1, "Should generate different prices over time")
    }

    @Test
    fun `should never generate negative prices`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val randomWalk = RandomWalkInstrument(instrument)

        repeat(1000) {
            val price = randomWalk.nextPrice()
            assertTrue(price >= BigDecimal.ONE, "Price should be at least 1: $price")
        }
    }

    @Test
    fun `prices should follow random walk pattern`() {
        val instrument = Instrument("DE0007164600", "SAP SE")
        val randomWalk = RandomWalkInstrument(instrument)

        var previousPrice = randomWalk.nextPrice()
        var totalChange = BigDecimal.ZERO

        repeat(100) {
            val currentPrice = randomWalk.nextPrice()
            val change = currentPrice.subtract(previousPrice).abs()
            totalChange = totalChange.add(change)
            previousPrice = currentPrice
        }

        val avgChange = totalChange.divide(BigDecimal(100), 4, java.math.RoundingMode.HALF_UP)
        assertTrue(avgChange < BigDecimal(10), "Average change should be relatively small for random walk")
    }
}
