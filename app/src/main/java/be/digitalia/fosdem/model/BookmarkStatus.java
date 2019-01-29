package be.digitalia.fosdem.model;

public class BookmarkStatus {

	private final boolean isBookmarked;
	private final boolean isUpdate;

	public BookmarkStatus(boolean isBookmarked, boolean isUpdate) {
		this.isBookmarked = isBookmarked;
		this.isUpdate = isUpdate;
	}

	public boolean isBookmarked() {
		return isBookmarked;
	}

	public boolean isUpdate() {
		return isUpdate;
	}
}
