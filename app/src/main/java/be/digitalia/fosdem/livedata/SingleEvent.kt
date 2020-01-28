package be.digitalia.fosdem.livedata

/**
 * Encapsulates data that can only be consumed once.
 */
class SingleEvent<T>(content: T) {

    private var content: T? = content

    /**
     * @return The content, or null if it has already been consumed.
     */
    fun consume(): T? {
        val previousContent = content
        if (previousContent != null) {
            content = null
        }
        return previousContent
    }
}