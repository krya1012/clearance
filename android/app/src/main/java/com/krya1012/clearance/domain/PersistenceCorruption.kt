package com.krya1012.clearance.domain

import kotlinx.serialization.json.Json

/**
 * True when `raw` — a blob read from some persisted key — exists but fails
 * to decode as [T]. Distinct from "no data yet" (`raw == null`), which
 * callers should treat differently (e.g. reseed vs. silently accept as
 * empty). Mirrors iOS's `isPersistedDataCorrupted`.
 */
inline fun <reified T> isPersistedDataCorrupted(raw: String?): Boolean {
    if (raw == null) return false
    return runCatching { Json.decodeFromString<T>(raw) }.getOrNull() == null
}
