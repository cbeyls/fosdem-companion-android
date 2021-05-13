package be.digitalia.fosdem.providers

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import be.digitalia.fosdem.db.ScheduleDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Simple content provider responsible for search suggestions.
 *
 * @author Christophe Beyls
 */
class SearchSuggestionProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun getType(uri: Uri): String? {
        return SearchManager.SUGGEST_MIME_TYPE
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        var query = uri.lastPathSegment ?: return null
        // Ignore empty or too small queries
        query = query.trim()
        if (query.length < MIN_QUERY_LENGTH || query == "search_suggest_query") {
            return null
        }

        val limitParam = uri.getQueryParameter("limit")
        val limit = if (limitParam.isNullOrEmpty()) DEFAULT_MAX_RESULTS else limitParam.toInt()

        val entryPoint = EntryPointAccessors.fromApplication(
            context!!.applicationContext, SearchSuggestionProviderEntryPoint::class.java
        )
        return entryPoint.scheduleDao.getSearchSuggestionResults(query, limit)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SearchSuggestionProviderEntryPoint {
        val scheduleDao: ScheduleDao
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 3
        private const val DEFAULT_MAX_RESULTS = 5
    }
}