package chess

enum class Player(val char: Char, val color: String, var playername: String) {
    WHITE('w', "white", "White"), BLACK('b', "black","Black")
}

//fun Player.char(): Char = if (this == Player.WHITE) 'w' else 'b'

//fun Player.color(): String = if (this == Player.WHITE) "white" else "black"
