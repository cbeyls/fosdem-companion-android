package be.digitalia.fosdem.adapters;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simplified CursorAdapter designed for RecyclerView.
 *
 * @author Christophe Beyls
 */
public abstract class RecyclerViewCursorAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

	private Cursor cursor;
	private int rowIDColumn = -1;

	public RecyclerViewCursorAdapter() {
		setHasStableIds(true);
	}

	/**
	 * Swap in a new Cursor, returning the old Cursor.
	 * The old cursor is not closed.
	 *
	 * @return The previously set Cursor, if any.
	 * If the given new Cursor is the same instance as the previously set
	 * Cursor, null is also returned.
	 */
	public Cursor swapCursor(Cursor newCursor) {
		if (newCursor == cursor) {
			return null;
		}
		Cursor oldCursor = cursor;
		cursor = newCursor;
		rowIDColumn = (newCursor == null) ? -1 : newCursor.getColumnIndexOrThrow("_id");
		notifyDataSetChanged();
		return oldCursor;
	}

	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public int getItemCount() {
		return (cursor == null) ? 0 : cursor.getCount();
	}

	/**
	 * @return The cursor initialized to the specified position.
	 */
	public Object getItem(int position) {
		if (cursor != null) {
			cursor.moveToPosition(position);
		}
		return cursor;
	}

	@Override
	public long getItemId(int position) {
		if ((cursor != null) && cursor.moveToPosition(position)) {
			return cursor.getLong(rowIDColumn);
		}
		return RecyclerView.NO_ID;
	}

	@Override
	public void onBindViewHolder(@NonNull VH holder, int position) {
		if (cursor == null) {
			throw new IllegalStateException("this should only be called when the cursor is not null");
		}
		if (!cursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position " + position);
		}
		onBindViewHolder(holder, cursor);
	}

	public abstract void onBindViewHolder(VH holder, Cursor cursor);
}