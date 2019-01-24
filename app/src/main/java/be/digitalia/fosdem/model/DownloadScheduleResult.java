package be.digitalia.fosdem.model;

public class DownloadScheduleResult {

	private static final DownloadScheduleResult RESULT_ERROR = new DownloadScheduleResult(0);
	private static final DownloadScheduleResult RESULT_UP_TO_DATE = new DownloadScheduleResult(0);

	private final int eventsCount;

	private DownloadScheduleResult(int eventsCount) {
		this.eventsCount = eventsCount;
	}

	public static DownloadScheduleResult success(int eventsCount) {
		return new DownloadScheduleResult(eventsCount);
	}

	public static DownloadScheduleResult error() {
		return RESULT_ERROR;
	}

	public static DownloadScheduleResult upToDate() {
		return RESULT_UP_TO_DATE;
	}

	public boolean isSuccess() {
		return this != RESULT_ERROR && this != RESULT_UP_TO_DATE;
	}

	public boolean isError() {
		return this == RESULT_ERROR;
	}

	public boolean isUpToDate() {
		return this == RESULT_UP_TO_DATE;
	}

	public int getEventsCount() {
		return eventsCount;
	}
}
