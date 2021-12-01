package chess

fun main() {
    println(TITLE)

    println("First Player's name:")
    val playerW = readLine()!!

    println("Second Player's name:")
    val playerB = readLine()!!

    val board = Board(playerW, playerB)
    board.print()
    board.turn()
}
