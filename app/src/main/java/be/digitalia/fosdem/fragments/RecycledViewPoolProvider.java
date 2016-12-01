package be.digitalia.fosdem.fragments;

import android.support.v7.widget.RecyclerView;

/**
 * Components implementing this interface allow to share a RecycledViewPool between similar fragments.
 */
public interface RecycledViewPoolProvider {
	RecyclerView.RecycledViewPool getRecycledViewPool();
}
