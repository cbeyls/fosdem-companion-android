package be.digitalia.fosdem.model

enum class Building {
    J, K, H, U, AW;

    companion object {
        fun fromRoomName(roomName: String): Building? = with(roomName) {
            when {
                isEmpty() -> null
                startsWith('J', ignoreCase = true) -> J
                startsWith('K', ignoreCase = true) -> K
                startsWith('H', ignoreCase = true) -> H
                startsWith('U', ignoreCase = true) -> U
                startsWith("AW", ignoreCase = true) -> AW
                equals("Janson", ignoreCase = true) -> J
                equals("Ferrer", ignoreCase = true) -> H
                equals("Chavanne", ignoreCase = true)
                        || equals("Lameere", ignoreCase = true)
                        || equals("Guillissen", ignoreCase = true) -> U
                else -> null
            }
        }
    }
}