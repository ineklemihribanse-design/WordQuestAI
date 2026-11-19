package com.kelimebulmacasi.a1

/** Deterministic duel policy. No UI timers, Firebase calls or device-specific state. */
internal enum class ArenaDuelMove { CORRECT, WRONG, PASS, TIMEOUT }

internal data class ArenaDuelWord(
    val firstStarter: String? = null,
    val firstAttempts: Map<String, ArenaDuelMove> = emptyMap(),
    val secondAttempts: Map<String, ArenaDuelMove> = emptyMap(),
    val owner: String? = null,
    val closed: Boolean = false
)

internal data class ArenaDuelSnapshot(
    val players: List<String>,
    val turn: String,
    val words: Map<Int, ArenaDuelWord>,
    val round: Int = 1,
    val score: Map<String, Int> = emptyMap(),
    val hints: Map<String, Int> = emptyMap(),
    val hintUsed: Set<Pair<String, Int>> = emptySet(),
    val completed: Boolean = false
) {
    fun selectable(player: String): List<Int> {
        val active = if(round==1) ArenaDuelWord::firstAttempts else ArenaDuelWord::secondAttempts
        return words.filter { (_, w) ->
            !w.closed && w.owner == null &&
                (if(round==1)player !in active(w) else
                    w.firstAttempts.size == players.size && player !in active(w))
        }.keys.sorted()
    }
}

/**
 * Exactly one answer attempt per player per word per round; a hint is a separate
 * resource, not a second guess. Two failures never reveal a word during round 1.
 */
internal object ArenaDuelEngine {
    fun hintsFor(size: Int): Int = when(size) {
        20 -> 3; 30 -> 4; 40 -> 5; 50 -> 6; else -> (size/10+1).coerceAtLeast(1)
    }

    fun newGame(wordCount: Int, first: String, second: String): ArenaDuelSnapshot {
        require(wordCount > 0 && first.isNotBlank() && second.isNotBlank() && first != second)
        val h = hintsFor(wordCount)
        return ArenaDuelSnapshot(
            players=listOf(first,second), turn=first,
            words=(0 until wordCount).associateWith { ArenaDuelWord() },
            score=mapOf(first to 0,second to 0),hints=mapOf(first to h,second to h))
    }

    fun useHint(current: ArenaDuelSnapshot, player: String, wordId: Int): ArenaDuelSnapshot {
        if(current.completed || player != current.turn ||
            wordId !in current.selectable(player) ||
            (player to wordId) in current.hintUsed ||
            (current.hints[player] ?: 0) <= 0) return current
        return current.copy(
            hints=current.hints+(player to ((current.hints[player] ?: 0)-1)),
            hintUsed=current.hintUsed+(player to wordId))
    }

    fun submit(
        current: ArenaDuelSnapshot, player: String, wordId: Int, move: ArenaDuelMove,
        remainingSeconds: Int = 0, totalSeconds: Int = 30
    ): ArenaDuelSnapshot {
        if(current.completed || current.turn != player || wordId !in current.selectable(player))
            return current
        val word=current.words.getValue(wordId)
        val opponent=current.players.first {it!=player}
        val currentAttempts=if(current.round==1)word.firstAttempts else word.secondAttempts
        val updates=currentAttempts+(player to move)
        val fixed=if(current.round==1) word.copy(
            firstStarter=word.firstStarter ?: player,
            firstAttempts=updates,owner=if(move==ArenaDuelMove.CORRECT)player else null
        ) else word.copy(
            secondAttempts=updates,owner=if(move==ArenaDuelMove.CORRECT)player else null,
            closed=move!=ArenaDuelMove.CORRECT && updates.size==current.players.size
        )
        val allWords=current.words+(wordId to fixed)
        val previous=current.score[player] ?: 0
        val speed=if(totalSeconds>0) (remainingSeconds.coerceIn(0,totalSeconds)*20/totalSeconds)
            else 0
        val steal=if(currentAttempts.isEmpty() &&
            (if(current.round==1)word.firstAttempts else word.secondAttempts)
                .any {it.key != player}) 10 else if(
            move==ArenaDuelMove.CORRECT &&
            word.firstAttempts.keys.any {it!=player}) 10 else 0
        val increment=when(move) {
            ArenaDuelMove.CORRECT -> (100+speed+steal -
                if((player to wordId) in current.hintUsed)20 else 0).coerceAtLeast(0)
            ArenaDuelMove.WRONG -> -10
            ArenaDuelMove.PASS, ArenaDuelMove.TIMEOUT -> 0
        }
        val nextScore=(previous+increment).coerceAtLeast(0)
        var updated=current.copy(words=allWords,score=current.score+(player to nextScore))
        val allPending=allWords.values.filter {it.owner==null&&!it.closed}
        val doneRound=allPending.all { w ->
            if(current.round==1)w.firstAttempts.size==current.players.size
            else w.secondAttempts.size==current.players.size
        }
        if(doneRound) {
            if(current.round==2 || allPending.isEmpty())
                return updated.copy(completed=true)
            // Every unresolved word gets ONE final replay, with crossing letters intact.
            val starter=if(word.firstStarter==player) opponent else player
            return updated.copy(round=2,turn=starter)
        }
        val bothTried=updates.size==current.players.size
        val preferred=when {
            bothTried -> if(word.firstStarter==player) opponent else player
            move==ArenaDuelMove.CORRECT -> player
            else -> opponent
        }
        val other=current.players.first {it != preferred}
        val nextPlayer=when {
            updated.selectable(preferred).isNotEmpty() -> preferred
            updated.selectable(other).isNotEmpty() -> other
            else -> preferred
        }
        return updated.copy(turn=nextPlayer)
    }
}
