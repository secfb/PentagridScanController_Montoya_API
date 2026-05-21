package ch.pentagrid.burpexts.pentagridscancontroller.helpers

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SimilarityTest {

    @Test
    fun testSimilarityIdentical() {
        val a = "Hello World".toByteArray()
        val b = "Hello World".toByteArray()
        val (similar, _) = Similarity.isSimilar(a, b, 0.9, null)
        assertTrue(similar, "Identical byte arrays must be similar")
    }

    @Test
    fun testSimilarityCompletelyDifferent() {
        val a = "AAAAA".toByteArray()
        val b = "BBBBB".toByteArray()
        val (similar, _) = Similarity.isSimilar(a, b, 0.5, null)
        assertFalse(similar, "Completely different strings should not be similar")
    }

    @Test
    fun testSimilaritySlightlyDifferent() {
        val a = "Test String A".toByteArray()
        val b = "Test String B".toByteArray()
        // With 12 out of 13 matches (ignoring order), the quick ratio is extremely high
        val (similar, _) = Similarity.isSimilar(a, b, 0.8, null)
        assertTrue(similar, "Slightly different strings with high similarity threshold should match")
    }
}
