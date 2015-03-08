package org.fossasia.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.fossasia.R;
import org.fossasia.db.DatabaseManager;
import org.fossasia.loaders.SimpleCursorLoader;
import org.fossasia.widgets.BookmarksMultiChoiceModeListener;

/**
 * Bookmarks list, optionally filterable.
 *
 * @author Christophe Beyls
 */
public class BookmarksListFragment extends SmoothListFragment {

    private static final int BOOKMARKS_LOADER_ID = 1;
    private static final String PREF_UPCOMING_ONLY = "bookmarks_upcoming_only";

    private boolean upcomingOnly;

    private MenuItem filterMenuItem;
    private MenuItem upcomingOnlyMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        upcomingOnly = getActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            BookmarksMultiChoiceModeListener.register(getListView());
        }

        setEmptyText(getString(R.string.no_bookmark));
        setListShown(false);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bookmarks, menu);
        filterMenuItem = menu.findItem(R.id.filter);
        upcomingOnlyMenuItem = menu.findItem(R.id.upcoming_only);
        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        if (filterMenuItem != null) {
            filterMenuItem.setIcon(upcomingOnly ?
                    R.drawable.ic_filter_list_selected_white_24dp
                    : R.drawable.ic_filter_list_white_24dp);
            upcomingOnlyMenuItem.setChecked(upcomingOnly);
        }
    }

    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();
        filterMenuItem = null;
        upcomingOnlyMenuItem = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upcoming_only:
                upcomingOnly = !upcomingOnly;
                updateOptionsMenu();
                getActivity().getPreferences(Context.MODE_PRIVATE).edit().putBoolean(PREF_UPCOMING_ONLY, upcomingOnly).commit();
                return true;
        }
        return false;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

    }

    private static class BookmarksLoader extends SimpleCursorLoader {

        // Events that just started are still shown for 5 minutes
        private static final long TIME_OFFSET = 5L * 60L * 1000L;

        private final boolean upcomingOnly;
        private final Handler handler;
        private final Runnable timeoutRunnable = new Runnable() {

            @Override
            public void run() {
                onContentChanged();
            }
        };

        public BookmarksLoader(Context context, boolean upcomingOnly) {
            super(context);
            this.upcomingOnly = upcomingOnly;
            this.handler = new Handler();
        }

        @Override
        public void deliverResult(Cursor cursor) {
            if (upcomingOnly && !isReset()) {
                handler.removeCallbacks(timeoutRunnable);
                // The loader will be refreshed when the start time of the first bookmark in the list is reached
                if ((cursor != null) && cursor.moveToFirst()) {
                    long startTime = DatabaseManager.toEventStartTimeMillis(cursor);
                    if (startTime != -1L) {
                        long delay = startTime - (System.currentTimeMillis() - TIME_OFFSET);
                        if (delay > 0L) {
                            handler.postDelayed(timeoutRunnable, delay);
                        } else {
                            onContentChanged();
                        }
                    }
                }
            }
            super.deliverResult(cursor);
        }

        @Override
        protected void onReset() {
            super.onReset();
            if (upcomingOnly) {
                handler.removeCallbacks(timeoutRunnable);
            }
        }

        @Override
        protected Cursor getCursor() {
            return DatabaseManager.getInstance().getBookmarks(upcomingOnly ? System.currentTimeMillis() - TIME_OFFSET : -1L);
        }
    }
}
