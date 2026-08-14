package be.digitalia.fosdem.model

sealed class DownloadScheduleResult {
    data class Success(val eventsCount: Int) : DownloadScheduleResult()
    data object Error : DownloadScheduleResult()
    data object UpToDate : DownloadScheduleResult()
}