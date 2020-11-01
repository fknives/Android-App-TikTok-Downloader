package org.fnives.tiktokdownloader.ui.shared

data class Event<D>(private val data: D) {

    var consume: Boolean = false
        private set

    val item: D? get() = data?.takeUnless { consume }.also { consume = true }

    fun peek(): D = data
}