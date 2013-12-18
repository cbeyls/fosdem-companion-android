package be.digitalia.fosdem.providers;

import be.digitalia.fosdem.db.DatabaseManager;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class SearchSuggestionProvider extends ContentProvider {

	private static final int MIN_QUERY_LENGTH = 3;

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(Uri uri) {
		return SearchManager.SUGGEST_MIME_TYPE;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String query = uri.getLastPathSegment();
		// Ignore empty or too small queries
		if (query == null) {
			return null;
		}
		query = query.trim();
		if (query.length() < MIN_QUERY_LENGTH) {
			return null;
		}

		return DatabaseManager.getInstance().getSearchSuggestionResults(query);
	}
}
