# Learning rules smoke test

Requires JDK 17+ and `kotlinc`. From the project root:

```bash
kotlinc app/src/main/java/com/kelimebulmacasi/a1/Crossword.kt \
  app/src/main/java/com/kelimebulmacasi/a1/LearningRules.kt \
  app/src/main/java/com/kelimebulmacasi/a1/Fsrs.kt \
  tools/learning-tests/LearningRulesTest.kt -include-runtime -d /tmp/wq-learning-tests.jar
java -jar /tmp/wq-learning-tests.jar
```

This only tests pure-Kotlin rules and crossword smoke behavior; it does **not** replace Android build, database instrumentation, UI, Firebase or two-device testing.
