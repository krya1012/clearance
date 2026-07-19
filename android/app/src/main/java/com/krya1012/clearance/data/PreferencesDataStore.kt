package com.krya1012.clearance.data

import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

val Context.clearanceDataStore by preferencesDataStore(name = "clearance_prefs")
