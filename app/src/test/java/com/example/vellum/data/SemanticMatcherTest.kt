package com.example.vellum.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticMatcherTest {

    @Test
    fun testDirectContainmentMatches() {
        // Direct containment
        assertTrue(SemanticMatcher.isSemanticMatch("coffee", "morning coffee"))
        assertTrue(SemanticMatcher.isSemanticMatch("morning coffee", "coffee"))
    }

    @Test
    fun testSemanticSimilarityMatches() {
        // Semantically similar financial words (e.g. food concept)
        assertTrue(SemanticMatcher.isSemanticMatch("something to eat", "grocery shopping"))
        assertTrue(SemanticMatcher.isSemanticMatch("burger", "restaurant bill"))
        assertTrue(SemanticMatcher.isSemanticMatch("pizza", "cafe lunch"))
        
        // Transport concept
        assertTrue(SemanticMatcher.isSemanticMatch("fuel", "petrol station"))
        assertTrue(SemanticMatcher.isSemanticMatch("cab ride", "taxi travel"))
        
        // Shopping concept
        assertTrue(SemanticMatcher.isSemanticMatch("buy shirt", "clothes shopping"))
        assertTrue(SemanticMatcher.isSemanticMatch("shoes", "store clothing"))
        
        // Utilities/Bills concept
        assertTrue(SemanticMatcher.isSemanticMatch("wifi bill", "internet subscription"))
        assertTrue(SemanticMatcher.isSemanticMatch("electricity", "utility payment"))
    }

    @Test
    fun testNonMatchingWords() {
        // Very different concepts should not match
        assertFalse(SemanticMatcher.isSemanticMatch("salary", "pizza restaurant"))
        assertFalse(SemanticMatcher.isSemanticMatch("taxi cab", "wifi internet bill"))
        assertFalse(SemanticMatcher.isSemanticMatch("dividend paycheck", "clothes shop"))
    }
}
