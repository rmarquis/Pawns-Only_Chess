package chess

import kotlin.math.abs

const val TITLE = "Pawns-Only Chess"
val RANKS: List<Char> = ('1'..'1' + 7).toList()
val FILES: List<Char> = ('a'..'a' + 7).toList()

enum class Result {
    ONGOING, WHITE_WINS, BLACK_WINS, STALEMATE
}

class Board(private var playerW: String, private var playerB: String) {

    // Let's use a square centric representation, but a piece centric representation might be easier
    // Note: a real chess game would make good use of a hybrid approach
    // See https://www.chessprogramming.org/Board_Representation
    private val squares = mutableListOf<Square>()
    private var playerOnTurn: Player
    private var enPassant: Pair<Int, Int>

    init {
        for (i in 0..63) {
            val player: Player? = when (i / 8) {
                1 -> Player.WHITE
                6 -> Player.BLACK
                else -> null
            }
            //println("${i / 8} ${i % 8}")
            squares.add(Square(i / 8, i % 8, player))
        }

        playerOnTurn = Player.WHITE
        playerOnTurn.playername = playerW
        enPassant = Pair(-1, -1)
    }

    fun print() {
        val lineSeparator = "  +---+---+---+---+---+---+---+---+"
        println(lineSeparator)
        for (i in 7 downTo 0) {
            print(RANKS[i] + " |")
            for (j in 0..7) {
                print(" ${squareCharAt(j, i)} |")
            }
            println()
            println(lineSeparator)
        }
        print("    ")
        for (i in 0..7) {
            print(FILES[i] + "   ")
        }
        println()
    }

    private fun squareCharAt(column: Int, row: Int): Char {
        return squares[row * 8 + column].player?.char?.uppercaseChar() ?: ' '
    }

    fun turn() {
        playerOnTurn.playername = if (playerOnTurn == Player.WHITE) playerW else playerB

        while (true) {

            // check stalemate
            if (checkStalemate()) {
                break
            }

            println("${playerOnTurn.playername}'s turn:")

            val input = readLine()!!
            when {
                input.matches("[a-h][1-8][a-h][1-8]".toRegex()) -> {
                    // start coordinates
                    val sFile = FILES.indexOf(input[0])
                    val sRank = RANKS.indexOf(input[1])

                    // no pawn at initial square or pawn of opposite color
                    if (squareCharAt(sFile, sRank) != playerOnTurn.char.uppercaseChar()) {
                        println("No ${playerOnTurn.color} pawn at ${FILES[sFile]}${RANKS[sRank]}")
                        continue
                    }

                    // move is invalid
                    if (!makePawnMove(playerOnTurn, input)) {
                        println("Invalid Input")
                        continue
                    }

                    // check win conditions
                    if (checkWinConditions()) {
                        break
                    }

                    // switch turn
                    switchTurn(playerOnTurn)
                    continue
                }
                input == "exit" -> break
                else -> {
                    println("Invalid Input")
                    continue
                }
            }
        }
        println("Bye!")
    }

    private fun switchTurn(currentPlayerOnTurn: Player) {
        playerOnTurn = if (currentPlayerOnTurn == Player.WHITE) Player.BLACK else Player.WHITE
        playerOnTurn.playername = if (currentPlayerOnTurn == Player.WHITE) playerB else playerW
    }

    private fun makePawnMove(playerOnTurn: Player, input: String): Boolean {
        // start and end coordinates
        val sFile = FILES.indexOf(input[0])
        val sRank = RANKS.indexOf(input[1])
        val eFile = FILES.indexOf(input[2])
        val eRank = RANKS.indexOf(input[3])

        //println("input $input: $sFile$sRank$eFile$eRank")

        // is this a capture move?
        val isCapture: Boolean = when (playerOnTurn) {
            Player.WHITE -> abs(sFile - eFile) == 1 && eRank - sRank == 1
            Player.BLACK -> abs(sFile - eFile) == 1 && eRank - sRank == -1
        }

        // is this an en passant capture?
        var isEnPassant = false
        if (isCapture && enPassant.first != -1 && enPassant.second != -1) {
            val (epFile, epRank) = enPassant

            isEnPassant = when (playerOnTurn) {
                Player.WHITE -> eFile == epFile && eRank == epRank + 1
                Player.BLACK -> eFile == epFile && eRank == epRank - 1
            }
        }

        when (playerOnTurn) {
            Player.WHITE -> {
                // check file value
                if (sFile != eFile && isCapture.not()) return false
                // check direction
                if (sRank >= eRank) return false
                // check rank value
                if (sRank == 1) {
                    if (eRank - sRank > 2) return false
                } else {
                    if (eRank - sRank > 1) return false
                }
                // check if destination and path is empty
                if (isCapture) {
                    if (isEnPassant) {
                        // ep. destination is empty
                        if (squareCharAt(eFile, eRank) != ' ') return false
                    } else {
                        // destination is occupied by enemy pawn
                        if (squareCharAt(eFile, eRank) != 'B') return false
                    }
                } else {
                    val rangeRank = (sRank + 1)..eRank
                    for (i in rangeRank) {
                        //println("$i -> ${FILES[sFile]}${RANKS[i]}")
                        if (squareCharAt(sFile, i) != ' ') return false
                    }
                }
            }
            Player.BLACK -> {
                // check file value
                if (sFile != eFile && isCapture.not()) return false
                // check direction
                if (sRank <= eRank) return false
                // check rank value
                if (sRank == 6) {
                    if (sRank - eRank > 2) return false
                } else {
                    if (sRank - eRank > 1) return false
                }
                // check if destination and path is empty
                if (isCapture) {             // b5c4
                    if (isEnPassant) {
                        // ep. destination is empty
                        if (squareCharAt(eFile, eRank) != ' ') return false
                    } else {
                        // destination is occupied by enemy pawn
                        if (squareCharAt(eFile, eRank) != 'W') return false
                    }
                } else {
                    val rangeRank = (sRank - 1)..eRank
                    for (i in rangeRank) {
                        //println("$i -> ${FILES[sFile]}${RANKS[i]}")
                        if (squareCharAt(sFile, i) != ' ') return false
                    }
                }
            }
        }

        // make move
        makeMove(playerOnTurn, input, isEnPassant)

        // record ep of current move
        enPassant = when (playerOnTurn) {
            Player.WHITE -> if (sFile == eFile && sRank == 1 && eRank == 3) Pair(sFile, eRank) else Pair(-1, -1)
            Player.BLACK -> if (sFile == eFile && sRank == 6 && eRank == 4) Pair(sFile, eRank) else Pair(-1, -1)
        }
        return true
    }

    private fun makeMove(playerOnTurn: Player, input: String, isEnPassant: Boolean) {
        // start and end coordinates
        val sFile = FILES.indexOf(input[0])
        val sRank = RANKS.indexOf(input[1])
        val eFile = FILES.indexOf(input[2])
        val eRank = RANKS.indexOf(input[3])

        // remove pawn to start coordinate
        // add pawn to end coordinate
        val sSquareNumber = sRank * 8 + sFile
        val eSquareNumber = eRank * 8 + eFile
        squares[sSquareNumber] = Square(sSquareNumber / 8, sSquareNumber % 8, null)
        squares[eSquareNumber] = Square(eSquareNumber / 8, eSquareNumber % 8, playerOnTurn)

        if (isEnPassant) {
            // remove ep pawn
            val (epFile, epRank) = enPassant
            val epSquareNumber = epRank * 8 + epFile
            squares[epSquareNumber] = Square(epSquareNumber / 8, epSquareNumber % 8, null)
        }

        // print board
        print()
    }

    private fun checkStalemate(): Boolean {

        var result = Result.ONGOING
        var noMove = true

        // stalemate
        when (playerOnTurn) {
            Player.WHITE -> {
                for (i in 0..7) {
                    for (j in 0..7) {
                        // can move forward?
                        if (squareCharAt(i, j) == 'W' && ((j + 1) in 0..7) && squareCharAt(i, j + 1) == ' ') {
                            noMove = false
                        }
                        // can capture?
                        if (squareCharAt(i, j) == 'W' && ((i + 1) in 0..7) && ((j + 1) in 0..7) && squareCharAt(i + 1, j + 1) == 'B') {
                            noMove = false
                        }
                        if (squareCharAt(i, j) == 'W' && ((i - 1) in 0..7) && ((j + 1) in 0..7) && squareCharAt(i - 1, j + 1) == 'B') {
                            noMove = false
                        }
                        // can capture en passant?
                        if (squareCharAt(i, j) == 'W' && ((i + 1) in 0..7) && squareCharAt(i + 1, j) == 'B' && enPassant.first == i + 1 && enPassant.second == j) {
                            noMove = false
                        }
                        if (squareCharAt(i, j) == 'W' && ((i - 1) in 0..7) && squareCharAt(i - 1, j) == 'B' && enPassant.first == i - 1 && enPassant.second == j) {
                            noMove = false
                        }
                    }
                }
                if (noMove) {
                    result = Result.STALEMATE
                }
            }
            Player.BLACK -> {
                for (i in 0..7) {
                    for (j in 0..7) {
                        // can move forward?
                        if (squareCharAt(i, j) == 'B' && ((j - 1) in 0..7) && squareCharAt(i, j - 1) == ' ') {
                            noMove = false
                        }
                        // can capture?
                        if (squareCharAt(i, j) == 'B' && ((i + 1) in 0..7) && ((j - 1) in 0..7) && squareCharAt(i + 1, j - 1) == 'W') {
                            noMove = false
                        }
                        if (squareCharAt(i, j) == 'B' && ((i - 1) in 0..7) && ((j - 1) in 0..7) && squareCharAt(i - 1, j - 1) == 'W') {
                            noMove = false
                        }
                        // can capture en passant?
                        if (squareCharAt(i, j) == 'B' && ((i + 1) in 0..7) && squareCharAt(i + 1, j) == 'W' && enPassant.first == i + 1 && enPassant.second == j) {
                            noMove = false
                        }
                        if (squareCharAt(i, j) == 'B' && ((i - 1) in 0..7) && squareCharAt(i - 1, j) == 'W' && enPassant.first == i - 1 && enPassant.second == j) {
                            noMove = false
                        }
                    }
                }
                if (noMove) {
                    result = Result.STALEMATE
                }
            }
        }

        when (result) {
            Result.STALEMATE -> println("Stalemate!")
            Result.ONGOING -> return false
            else -> {}
        }
        return true
    }

    private fun checkWinConditions(): Boolean {

        var result = Result.ONGOING
        var allCaptured = true

        when (playerOnTurn) {
            Player.WHITE -> {
                // white pawn on 8th rank
                for (i in 0..7) {
                    if (squareCharAt(i, RANKS.lastIndex) == 'W') {
                        result = Result.WHITE_WINS
                        break
                    }
                }
                // all opposite pawns are captured
                for (i in 7 downTo 0) {
                    for (j in 0..7) {
                        if (squareCharAt(i, j) == 'B') {
                            allCaptured = false
                        }
                    }
                }
                if (allCaptured) {
                    result = Result.WHITE_WINS
                }
            }
            Player.BLACK -> {
                // black pawn on 1st rank
                for (i in 0..7) {
                    if (squareCharAt(i, 0) == 'B') {
                        result = Result.BLACK_WINS
                        break
                    }
                }
                // all opposite pawns are captured
                for (i in 7 downTo 0) {
                    for (j in 0..7) {
                        if (squareCharAt(i, j) == 'W') {
                            allCaptured = false
                        }
                    }
                }
                if (allCaptured) {
                    result = Result.BLACK_WINS
                }
            }
        }

        when (result) {
            Result.WHITE_WINS -> println("White Wins!")
            Result.BLACK_WINS -> println("Black Wins!")
            Result.ONGOING -> return false
            else -> {}
        }
        return true
    }
}
