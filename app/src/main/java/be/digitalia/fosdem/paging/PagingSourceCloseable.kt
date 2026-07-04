package be.digitalia.fosdem.paging

import androidx.paging.PagingSource

/**
 * Allows registering a PagingSource in a ViewModel so its resources are properly cleared
 * when the ViewModel is destroyed.
 * This is a necessary hack because Room PagingSources automatically start a coroutine that is otherwise never canceled.
 */
fun PagingSource<*, *>.toAutoCloseable() = AutoCloseable { invalidate() }
