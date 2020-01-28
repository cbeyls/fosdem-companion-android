package be.digitalia.fosdem.model

sealed class DownloadScheduleResult {
    class Success(val eventsCount: Int) : DownloadScheduleResult()
    object Error : DownloadScheduleResult()
    object UpToDate : DownloadScheduleResult()
}