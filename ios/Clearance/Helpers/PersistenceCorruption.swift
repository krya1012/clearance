//
//  PersistenceCorruption.swift
//  Clearance
//
//  Shared helper for telling "no data yet" (key absent) apart from "data
//  present but corrupted" (key present, undecodable) across every
//  UserDefaults-backed store. The two cases need different handling —
//  absent usually means first launch, corrupted usually means recover.
//

import Foundation
import os

/// App-wide logger for persistence-layer diagnostics (failed saves, corrupted
/// UserDefaults blobs) — visible in Console.app even in Release builds,
/// unlike `assertionFailure`.
let persistenceLogger = Logger(subsystem: "com.krya1012.clearance", category: "persistence")

/// True when `key`'s stored blob exists but fails to decode as `T`.
func isPersistedDataCorrupted<T: Decodable>(_ type: T.Type, forKey key: String, in defaults: UserDefaults) -> Bool {
    guard let data = defaults.data(forKey: key) else { return false }
    return (try? JSONDecoder().decode(type, from: data)) == nil
}
