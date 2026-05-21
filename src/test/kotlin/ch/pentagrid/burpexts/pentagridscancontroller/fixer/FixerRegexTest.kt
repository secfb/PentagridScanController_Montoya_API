package ch.pentagrid.burpexts.pentagridscancontroller.fixer

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FixerRegexTest {

    // Test UUID regex defined in UuidValue: "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    // Test Boolean regex defined in BooleanValue: "(true|True|TRUE|false|False|FALSE|0|1)"
    private val booleanRegex = Regex("^(true|True|TRUE|false|False|FALSE|0|1)$")

    @Test
    fun testUuidRegexMatching() {
        val validUuids = listOf(
            "123e4567-e89b-12d3-a456-426614174000",
            "00000000-0000-0000-0000-000000000000",
            "ABCDEF12-ABCD-EF12-ABCD-EF1234567890"
        )
        for (uuid in validUuids) {
            assertTrue(uuidRegex.matches(uuid), "Should match valid UUID: $uuid")
        }
    }

    @Test
    fun testUuidRegexNonMatching() {
        val invalidUuids = listOf(
            "123e4567-e89b-12d3-a456-42661417400", // too short
            "123e4567-e89b-12d3-a456-4266141740000", // too long
            "123e4567-e89b-12d3-a456-42661417400G", // invalid hex character
            "not-a-uuid"
        )
        for (uuid in invalidUuids) {
            assertFalse(uuidRegex.matches(uuid), "Should not match invalid UUID: $uuid")
        }
    }

    @Test
    fun testBooleanRegexMatching() {
        val validBooleans = listOf("true", "True", "TRUE", "false", "False", "FALSE", "0", "1")
        for (bool in validBooleans) {
            assertTrue(booleanRegex.matches(bool), "Should match valid Boolean: $bool")
        }
    }

    @Test
    fun testBooleanRegexNonMatching() {
        val invalidBooleans = listOf("yes", "no", "t", "f", "true1", "false0", "2", "-1")
        for (bool in invalidBooleans) {
            assertFalse(booleanRegex.matches(bool), "Should not match invalid Boolean: $bool")
        }
    }
}
