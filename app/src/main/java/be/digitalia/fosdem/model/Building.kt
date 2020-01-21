package be.digitalia.fosdem.model

enum class Building {
    J, K, H, U, AW, Unknown;

    companion object {
        fun fromRoomName(roomName: String): Building {
            if (roomName.isNotEmpty()) {
                when {
                    roomName.startsWith('J', ignoreCase = true) -> return J
                    roomName.startsWith('K', ignoreCase = true) -> return K
                    roomName.startsWith('H', ignoreCase = true) -> return H
                    roomName.startsWith('U', ignoreCase = true) -> return U
                    roomName.startsWith("AW", ignoreCase = true) -> return AW
                    roomName.equals("Janson", ignoreCase = true) -> return J
                    roomName.equals("Ferrer", ignoreCase = true) -> return H
                    roomName.equals("Chavanne", ignoreCase = true)
                            || roomName.equals("Lameere", ignoreCase = true)
                            || roomName.equals("Guillissen", ignoreCase = true) -> return U
                }
            }

            return Unknown
        }
    }
}