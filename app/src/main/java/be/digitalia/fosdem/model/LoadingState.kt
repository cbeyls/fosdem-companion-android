package be.digitalia.fosdem.model

sealed class LoadingState<out T : Any> {
    /**
     * The current download progress:
     * -1   : in progress, indeterminate
     * 0..99: progress value in percents
     */
    data class Loading(val progress: Int = -1) : LoadingState<Nothing>()
    data class Idle<T : Any>(val result: T? = null) : LoadingState<T>()
}