package com.teasupplier.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class BusinessProfile(
    val hotelNameMarathi: String = "हॉटेल रुद्रा अमृततुल्य",
    val hotelNameEnglish: String = "Hotel Rudra Amruttulya",
    val address: String = "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११",
    val phone: String = "मोबा. नं.: 8788138197 / 8830404860",
    val upiId: String = "8788138197@ybl"
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ALLOW_EDIT_KEY = booleanPreferencesKey("allow_edit_enabled")
        private val EXPIRY_DATE_KEY = longPreferencesKey("app_expiry_date")
        private val HOTEL_NAME_MARATHI_KEY = stringPreferencesKey("hotel_name_marathi")
        private val HOTEL_NAME_ENGLISH_KEY = stringPreferencesKey("hotel_name_english")
        private val HOTEL_ADDRESS_KEY = stringPreferencesKey("hotel_address")
        private val HOTEL_PHONE_KEY = stringPreferencesKey("hotel_phone")
        private val HOTEL_UPI_KEY = stringPreferencesKey("hotel_upi_id")
        private val TABLE_COUNT_KEY = intPreferencesKey("table_count")
    }

    val allowEditing: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ALLOW_EDIT_KEY] ?: false
    }

    suspend fun setAllowEditing(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ALLOW_EDIT_KEY] = enabled
        }
    }

    val expiryDate: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[EXPIRY_DATE_KEY]
    }

    suspend fun setExpiryDate(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[EXPIRY_DATE_KEY] = timestamp
        }
    }

    val businessProfile: Flow<BusinessProfile> = context.dataStore.data.map { prefs ->
        BusinessProfile(
            hotelNameMarathi = prefs[HOTEL_NAME_MARATHI_KEY] ?: "हॉटेल रुद्रा अमृततुल्य",
            hotelNameEnglish = prefs[HOTEL_NAME_ENGLISH_KEY] ?: "Hotel Rudra Amruttulya",
            address = prefs[HOTEL_ADDRESS_KEY] ?: "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११",
            phone = prefs[HOTEL_PHONE_KEY] ?: "मोबा. नं.: 8788138197 / 8830404860",
            upiId = prefs[HOTEL_UPI_KEY] ?: ""
        )
    }

    suspend fun updateBusinessProfile(profile: BusinessProfile) {
        context.dataStore.edit { prefs ->
            prefs[HOTEL_NAME_MARATHI_KEY] = profile.hotelNameMarathi
            prefs[HOTEL_NAME_ENGLISH_KEY] = profile.hotelNameEnglish
            prefs[HOTEL_ADDRESS_KEY] = profile.address
            prefs[HOTEL_PHONE_KEY] = profile.phone
            prefs[HOTEL_UPI_KEY] = profile.upiId
        }
    }

    val tableCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TABLE_COUNT_KEY] ?: 10
    }

    suspend fun setTableCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[TABLE_COUNT_KEY] = count.coerceIn(1, 50)
        }
    }
}
