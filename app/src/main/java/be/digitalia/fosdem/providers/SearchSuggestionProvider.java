package be.digitalia.fosdem.providers;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import be.digitalia.fosdem.db.AppDatabase;

/**
 * Simple content provider responsible for search suggestions.
 *
 * @author Christophe Beyls
 */
public class SearchSuggestionProvider extends ContentProvider {

	private static final int MIN_QUERY_LENGTH = 3;
	private static final int DEFAULT_MAX_RESULTS = 5;

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(@NonNull Uri uri) {
		return SearchManager.SUGGEST_MIME_TYPE;
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String query = uri.getLastPathSegment();
		// Ignore empty or too small queries
		if (query == null) {
			return null;
		}
		query = query.trim();
		if ((query.length() < MIN_QUERY_LENGTH) || "search_suggest_query".equals(query)) {
			return null;
		}

		String limitParam = uri.getQueryParameter("limit");
		int limit = TextUtils.isEmpty(limitParam) ? DEFAULT_MAX_RESULTS : Integer.parseInt(limitParam);

		return AppDatabase.getInstance(getContext()).getScheduleDao().getSearchSuggestionResults(query, limit);
	}
}
