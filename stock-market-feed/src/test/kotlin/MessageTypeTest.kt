import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MessageTypeTest {

    @Test
    fun `should have ADD type`() {
        val type = MessageType.ADD
        assertEquals("ADD", type.name)
    }

    @Test
    fun `should have DELETE type`() {
        val type = MessageType.DELETE
        assertEquals("DELETE", type.name)
    }

    @Test
    fun `should have QUOTE type`() {
        val type = MessageType.QUOTE
        assertEquals("QUOTE", type.name)
    }

    @Test
    fun `should have exactly 3 values`() {
        val values = MessageType.values()
        assertEquals(3, values.size)
    }

    @Test
    fun `should parse from string`() {
        assertEquals(MessageType.ADD, MessageType.valueOf("ADD"))
        assertEquals(MessageType.DELETE, MessageType.valueOf("DELETE"))
        assertEquals(MessageType.QUOTE, MessageType.valueOf("QUOTE"))
    }

    @Test
    fun `should throw exception for invalid value`() {
        assertThrows(IllegalArgumentException::class.java) {
            MessageType.valueOf("INVALID")
        }
    }
}
