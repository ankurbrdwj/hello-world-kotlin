import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest

class UtilityFunctionsTest {

    @Test
    fun `randomInt should return value within bounds`() {
        repeat(100) {
            val result = randomInt(10)
            assertTrue(result in 0..9, "randomInt(10) should return 0-9, got $result")
        }
    }

    @Test
    fun `randomDigit should return 0-9`() {
        repeat(100) {
            val digit = randomDigit()
            assertTrue(digit in 0..9, "randomDigit should return 0-9, got $digit")
        }
    }

    @Test
    fun `randomAZChar should return uppercase letter`() {
        repeat(100) {
            val char = randomAZChar()
            assertTrue(char in 'A'..'Z', "randomAZChar should return A-Z, got $char")
        }
    }

    @Test
    fun `randomDigitOrChar should return digit or uppercase letter`() {
        repeat(100) {
            val char = randomDigitOrChar()
            val isValid = char in '0'..'9' || char in 'A'..'Z'
            assertTrue(isValid, "randomDigitOrChar should return 0-9 or A-Z, got $char")
        }
    }

    @Test
    fun `generateIsin should return 12 character string`() {
        repeat(10) {
            val isin = generateIsin()
            assertEquals(12, isin.length, "ISIN should be 12 characters")
        }
    }

    @Test
    fun `generateIsin should start with two letters`() {
        repeat(10) {
            val isin = generateIsin()
            assertTrue(isin[0] in 'A'..'Z', "First char should be a letter")
            assertTrue(isin[1] in 'A'..'Z', "Second char should be a letter")
        }
    }

    @Test
    fun `generateIsin should have alphanumeric characters`() {
        repeat(10) {
            val isin = generateIsin()
            isin.forEach { char ->
                val isValid = char in '0'..'9' || char in 'A'..'Z'
                assertTrue(isValid, "ISIN should only contain alphanumeric chars, got $char")
            }
        }
    }

    @Test
    fun `generateIsin should generate unique values`() {
        val isins = mutableSetOf<String>()
        repeat(100) {
            isins.add(generateIsin())
        }
        assertTrue(isins.size > 90, "Should generate mostly unique ISINs")
    }

    @Test
    fun `randomBetween should return value within range`() {
        repeat(100) {
            val result = randomBetween(10.0, 20.0)
            assertTrue(result >= 10.0, "Should be >= 10.0, got $result")
            assertTrue(result <= 20.0, "Should be <= 20.0, got $result")
        }
    }

    @Test
    fun `randomBetween should handle negative ranges`() {
        repeat(100) {
            val result = randomBetween(-10.0, -5.0)
            assertTrue(result >= -10.0, "Should be >= -10.0, got $result")
            assertTrue(result <= -5.0, "Should be <= -5.0, got $result")
        }
    }

    @Test
    fun `randomBetween should handle zero crossing ranges`() {
        repeat(100) {
            val result = randomBetween(-5.0, 5.0)
            assertTrue(result >= -5.0, "Should be >= -5.0, got $result")
            assertTrue(result <= 5.0, "Should be <= 5.0, got $result")
        }
    }
}
