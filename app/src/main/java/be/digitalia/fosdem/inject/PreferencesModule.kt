package be.digitalia.fosdem.inject

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import be.digitalia.fosdem.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    private const val UI_STATE_SHARED_PREFERENCES_NAME = "ui_state"

    @Provides
    @Named("UserSettings")
    fun provideUserSettingsSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        PreferenceManager.setDefaultValues(context, R.xml.settings, false)
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Named("UIState")
    fun provideUIStateSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(UI_STATE_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
}