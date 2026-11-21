# 1.0.75 YDS timed exam disclosure regression

For 15-minute and 80-question/180-minute timed exams, tapping an option must
**not** reveal the correct option in green or show the Turkish solution. A
neutral "answer recorded" message is shown. The original answer, score and
wrong-question progress still record to the user's profile.

For unscheduled practice mode only, the correct option and explanation appear
immediately. Both branches use the single Wq175YdsVisibility.reveal(exam)
predicate, covered by JVM tests. Run a manual tap-through on a phone to
inspect text and option colors before publishing.
