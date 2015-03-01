package org.fossasia.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;

import org.fossasia.R;
import org.fossasia.db.DatabaseManager;
import org.fossasia.loaders.BaseLiveLoader;

public class NowLiveListFragment extends BaseLiveListFragment {

    @Override
    protected String getEmptyText() {
        return getString(R.string.now_empty);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new NowLiveLoader(getActivity());
    }

    private static class NowLiveLoader extends BaseLiveLoader {

        public NowLiveLoader(Context context) {
            super(context);
        }

        @Override
        protected Cursor getCursor() {
            long now = System.currentTimeMillis();
            return DatabaseManager.getInstance().getEvents(-1L, now, now, false);
        }
    }
}
