package org.fossasia.api;

/**
 * Main API entry point.
 *
 * @author Christophe Beyls
 */
public class FosdemApi {

    // Local broadcasts parameters
    public static final String ACTION_DOWNLOAD_SCHEDULE_PROGRESS = "be.digitalia.fosdem.action.DOWNLOAD_SCHEDULE_PROGRESS";
    public static final String EXTRA_PROGRESS = "PROGRESS";
    public static final String ACTION_DOWNLOAD_SCHEDULE_RESULT = "be.digitalia.fosdem.action.DOWNLOAD_SCHEDULE_RESULT";
    public static final String EXTRA_RESULT = "RESULT";

    public static final int RESULT_ERROR = -1;
    public static final int RESULT_UP_TO_DATE = -2;

}