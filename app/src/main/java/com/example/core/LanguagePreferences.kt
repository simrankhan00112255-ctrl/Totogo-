package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "toto_go_prefs")

class LanguagePreferences(private val context: Context) {
  private val languageKey = stringPreferencesKey("preferred_language")

  val languageFlow: Flow<AppLanguage> =
    context.dataStore.data.map { prefs ->
      when (prefs[languageKey]) {
        AppLanguage.ENGLISH.code -> AppLanguage.ENGLISH
        else -> AppLanguage.BENGALI // Bengali is default
      }
    }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { prefs ->
      prefs[languageKey] = language.code
    }
  }
}
