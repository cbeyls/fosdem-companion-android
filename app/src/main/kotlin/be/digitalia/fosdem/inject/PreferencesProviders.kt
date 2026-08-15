package be.digitalia.fosdem.inject

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.datastore.DeferredWriteDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@ContributesTo(AppScope::class)
interface PreferencesProviders {
    @Provides
    fun provideSharedPreferences(context: Context): SharedPreferences {
        PreferenceManager.setDefaultValues(context, R.xml.settings, false)
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @UIStateDataStore
    @SingleIn(AppScope::class)
    fun provideUIStateDataStore(context: Context): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val preferencesDataStore = PreferenceDataStoreFactory.create(
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = UI_STATE_DATASTORE_FILE_NAME,
                )
            ),
            scope = scope,
        ) {
            context.preferencesDataStoreFile(UI_STATE_DATASTORE_FILE_NAME)
        }
        return DeferredWriteDataStore(preferencesDataStore, scope)
    }

    companion object {
        private const val UI_STATE_DATASTORE_FILE_NAME = "ui_state"
    }
}
