package com.kelimebulmacasi.a1

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.TextAttribute
import android.widget.EditText
import androidx.annotation.RequiresApi

// The grid is the text display. This editor keeps a single Android IME connection
// open as the active cell changes, so the keyboard never has to refocus per letter.
@SuppressLint("AppCompatCustomView") // Embedded in Compose with platform input APIs and no AppCompat theme.
class KeyboardInput(context: Context) : EditText(context) {
    var onLetters: (String) -> Unit = {}
    var onDelete: () -> Unit = {}
    var onNextWord: () -> Unit = {}

    init {
        id = NO_ID
        isSaveEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(Color.TRANSPARENT)
        setPadding(0, 0, 0, 0)
        isCursorVisible = false
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
    }

    fun openKeyboard() {
        requestFocus()
        post {
            if (hasFocus() && isAttachedToWindow) {
                context.getSystemService(InputMethodManager::class.java)
                    .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun closeKeyboard() {
        context.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val target = super.onCreateInputConnection(outAttrs) ?: return null
        return if (Build.VERSION.SDK_INT >= 33) {
            ModernConnection(target)
        } else {
            GridConnection(target)
        }
    }

    private open inner class GridConnection(target: InputConnection) : InputConnectionWrapper(target, false) {
        private var composing = ""

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            composing = ""
            onLetters(text?.toString().orEmpty())
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            // Some keyboards still compose with suggestions disabled. Keep only the
            // latest candidate until they commit it or finish composing.
            composing = text?.toString().orEmpty()
            return true
        }

        override fun finishComposingText(): Boolean {
            if (composing.isNotEmpty()) commitText(composing, 1)
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            val count = beforeLength.coerceIn(0, 16)
            val buffered = composing.length
            composing = composing.dropLast(count)
            repeat((count - buffered).coerceAtLeast(0)) { onDelete() }
            return true
        }

        override fun deleteSurroundingTextInCodePoints(
            beforeLength: Int,
            afterLength: Int,
        ): Boolean = deleteSurroundingText(beforeLength, afterLength)

        override fun performEditorAction(actionCode: Int): Boolean {
            finishComposingText()
            onNextWord()
            return true
        }
    }

    @RequiresApi(33)
    private inner class ModernConnection(target: InputConnection) : GridConnection(target) {
        override fun commitText(text: CharSequence, newCursorPosition: Int, textAttribute: TextAttribute?): Boolean =
            commitText(text, newCursorPosition)

        override fun setComposingText(text: CharSequence, newCursorPosition: Int, textAttribute: TextAttribute?): Boolean =
            setComposingText(text, newCursorPosition)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> onDelete()
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_TAB -> onNextWord()
            else -> {
                val codePoint = event.unicodeChar
                if (codePoint == 0 || !Character.isLetter(codePoint) ||
                    event.isCtrlPressed || event.isMetaPressed
                ) return super.onKeyDown(keyCode, event)
                onLetters(String(Character.toChars(codePoint)))
            }
        }
        return true
    }
}
