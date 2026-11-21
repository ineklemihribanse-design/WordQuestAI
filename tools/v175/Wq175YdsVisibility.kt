package com.kelimebulmacasi.a1

/** Prevents immediate disclosure of correct YDS answers during timed exams. */
internal object Wq175YdsVisibility {
    fun reveal(isTimedExam:Boolean):Boolean = !isTimedExam
}
