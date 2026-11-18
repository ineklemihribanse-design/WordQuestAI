# WordQuest AI v1.0.13 Premium Arena UX and timer upgrade

- First move in crossword duels is drawn randomly after both players enter the lobby.
- A turn no longer starts while a room is being created; server time offset is followed.
- On expiration, a transaction automatically advances exactly one turn; this is safe against two devices racing.
- Live room settings and share actions are collapsed into the three-dot menu during gameplay.
- Added responsive crossword cell sizing for phones/tablets and white letters on solved green cells.
- Input hint remains visible above the keyboard when typing.
- Quiz question timers are retained while navigating between questions in a running session.
- End-of-match prompt offers to create another room (not an automatic rematch).

IMPORTANT: replace Firebase Realtime Database rules with database.rules.json before starting new rooms: the turn write rule now permits the host to choose an initial player in the lobby.
CI Android/JVM validation is required. Two-device Firebase live tests and device UI tests cannot be simulated by the checks.
