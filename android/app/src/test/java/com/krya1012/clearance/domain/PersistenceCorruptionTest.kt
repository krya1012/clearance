package com.krya1012.clearance.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceCorruptionTest {

    @Test
    fun `absent data is not corrupted`() {
        assertFalse(isPersistedDataCorrupted<List<String>>(null))
    }

    @Test
    fun `valid json is not corrupted`() {
        assertFalse(isPersistedDataCorrupted<List<String>>("""["a","b"]"""))
    }

    @Test
    fun `an empty but validly-encoded collection is not corrupted`() {
        // Distinct from "no data yet": the user genuinely cleared everything,
        // which must not be mistaken for corruption.
        assertFalse(isPersistedDataCorrupted<List<String>>("[]"))
    }

    @Test
    fun `malformed json is corrupted`() {
        assertTrue(isPersistedDataCorrupted<List<String>>("not valid json"))
    }

    @Test
    fun `json of the wrong shape is corrupted`() {
        assertTrue(isPersistedDataCorrupted<List<String>>("""{"not": "a list"}"""))
    }
}
