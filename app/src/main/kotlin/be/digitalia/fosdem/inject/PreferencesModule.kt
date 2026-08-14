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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    private const val UI_STATE_DATASTORE_FILE_NAME = "ui_state"

    @Provides
    @Named("UserSettings")
    fun provideUserSettingsSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        PreferenceManager.setDefaultValues(context, R.xml.settings, false)
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Named("UIState")
    @Singleton
    fun provideUIStateDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
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
}
