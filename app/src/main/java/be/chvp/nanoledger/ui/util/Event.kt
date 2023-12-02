package be.chvp.nanoledger.ui.util

open class Event<out T>(private val content: T) {
    var handled = false
        private set

    fun get(): T? {
        if (handled) {
            return null
        }
        handled = true
        return content
    }

    fun peek(): T = content
}
