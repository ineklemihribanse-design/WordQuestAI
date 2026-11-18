package com.kelimebulmacasi.a1

fun main() {
    val word = Entry("RIGHT", "Sağ", "Turn right.")
    val key = learningWordKey(word)
    check(key == learningWordKey(Entry("right", " sağ ")))
    check(key != learningWordKey(Entry("RIGHT", "Hak")))
    val wrong = LearningSignal(LearningKind.WRONG_CHECK, word, attempt = "RIHGT")
    check(learningEventId("g1", wrong) == learningEventId("g1", wrong))
    check(learningEventId("g1", wrong) != learningEventId("g2", wrong))
    check(learningEventId("g1", wrong) != learningEventId("g1", wrong.copy(resetAt = 200)))
    check(learningEventId("g1", wrong) != learningEventId("g1", wrong.copy(attempt = "RGIHT")))
    check(learningEventId("g1", wrong.copy(boardUpdatedAt = 200)) ==
        learningEventId("g1", wrong.copy(boardUpdatedAt = 200)))
    check(learningEventId("g1", wrong.copy(boardUpdatedAt = 200)) !=
        learningEventId("g1", wrong.copy(boardUpdatedAt = 201)))
    val hint = LearningSignal(LearningKind.HINT, word, boardUpdatedAt = 200)
    check(learningEventId("g1", hint) != learningEventId("g1", hint.copy(boardUpdatedAt = 201)))
    check(learningSearchTerm(" SIĞINAK / ŞİŞE / SÜT ") == "siginak / sise / sut")
    var card = LearningCard(key, word.answer, word.clue, word.context)
    val t = 1_700_000_000_000L
    card = LearningSchedule.next(card, LearningKind.WRONG_CHECK, t)
    check(card.wrong == 1 && card.dueAt == t + 10 * LearningSchedule.MINUTE && card.memory == null)
    card = LearningSchedule.next(card, LearningKind.SOLVED, t + 1000)
    check(card.dueAt == t + 10 * LearningSchedule.MINUTE) // solved doesn't erase mistake
    card = LearningSchedule.next(card, LearningKind.REVIEW_GOOD, t + 10 * LearningSchedule.MINUTE)
    check(card.remembered == 1 && card.memory!!.stability == 2.3065)
    check(card.dueAt == t + 10 * LearningSchedule.MINUTE + 2 * LearningSchedule.DAY)
    val nextAt = card.dueAt
    val hard = Fsrs.update(card.memory, ReviewRating.Hard, nextAt)
    val good = Fsrs.update(card.memory, ReviewRating.Good, nextAt)
    val easy = Fsrs.update(card.memory, ReviewRating.Easy, nextAt)
    check(hard.stability < good.stability && good.stability < easy.stability)
    card = LearningSchedule.next(card, LearningKind.REVIEW_GOOD, nextAt)
    check(card.remembered == 2 && card.memory == good && card.dueAt > nextAt + 2 * LearningSchedule.DAY)
    val forgottenAt = card.dueAt
    card = LearningSchedule.next(card, LearningKind.REVIEW_AGAIN, forgottenAt)
    check(card.forgotten == 1 && card.memory!!.stability < good.stability)
    check(card.dueAt == forgottenAt + 10 * LearningSchedule.MINUTE)
    val assisted = LearningSchedule.next(LearningCard(key,word.answer,word.clue,""), LearningKind.SOLVED, t, assisted = true)
    check(assisted.dueAt == t + 10*LearningSchedule.MINUTE)
    // Existing crossword still locks a correct completed word.
    val game = Crossword(foodPuzzle)
    val index = foodPuzzle.words.indexOfFirst { it.entry.letters == "WATER" }
    game.selectWord(index)
    check(game.reveal() == foodPuzzle.words[index].cells[0])
    check(game.reveal() == foodPuzzle.words[index].cells[1])
    game.enter("WATER")
    check(index in game.progress().lockedWords)
    check(game.reveal() == null)
    val ready = Crossword(foodPuzzle)
    val water = foodPuzzle.words[index]
    ready.selectWord(index)
    ready.enter("W")
    ready.selectLetter(water.cells.first())
    check(ready.reveal() == null)
    check(index !in ready.progress().hinted)
    println("PASS: learning schedule, identity, Turkish search and crossword hints")
}
