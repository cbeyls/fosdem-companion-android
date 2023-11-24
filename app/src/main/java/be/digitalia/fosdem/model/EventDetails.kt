package be.digitalia.fosdem.model

data class EventDetails(
    val persons: List<Person>,
    val attachments: List<Attachment>,
    val links: List<Link>
)