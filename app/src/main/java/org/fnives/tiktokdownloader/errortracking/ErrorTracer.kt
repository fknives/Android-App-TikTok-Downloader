package org.fnives.tiktokdownloader.errortracking

import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName


object ErrorTracer {

    private var errorTransaction: ErrorTransaction? = null
    private val errorTransactions = mutableListOf<ErrorTransaction>()
    private val errorListener = mutableListOf<() -> Unit>()

    val hasErrors get() = errorTransactions.isNotEmpty()

    fun startErrorTransaction(url: String) {
        errorTransaction = ErrorTransaction(url)
    }

    fun addError(html: String, message: String, throwable: Throwable?) {
        errorTransaction?.addError(html = html, message = message, throwable = throwable)
    }

    fun cancelErrorTransaction() {
        errorTransaction = null
    }

    fun commitErrorTransaction() {
        val needsToNotify = !hasErrors
        val errorTransaction = errorTransaction
        if (errorTransaction != null) {
            errorTransactions.add(errorTransaction)
        }
        if (needsToNotify) {
            val errorListener = errorListener
            errorListener.forEach {
                doOnMainThread {
                    it.invoke()
                }
            }
        }
    }

    fun subscribeToHasErrorChanges(listener: () -> Unit): () -> Unit {
        doOnMainThread {
            errorListener.add(listener)
        }
        return fun() {
            doOnMainThread {
                errorListener.remove(listener)
            }
        }
    }

    private fun doOnMainThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post { action() }
    }

    @WorkerThread
    fun getErrorAsJSON(): String {
        val errorTransactions = errorTransactions
        val gson = Gson()
        val arrayInner = errorTransactions.joinToString(",", transform = gson::toJson)
        return "[$arrayInner]"
    }
}

private class ErrorTransaction(
    @SerializedName("url") val url: String
) {
    @SerializedName("errors")
    val errors = mutableListOf<ErrorsDuringTransaction>()

    @SerializedName("type")
    val errorType = "ErrorTransaction"

    fun addError(html: String, message: String, throwable: Throwable?) {
        val stacktrace = throwable?.stackTraceToString()

        errors.add(
            ErrorsDuringTransaction(
                html = html,
                message = message,
                stacktrace = stacktrace,
            )
        )
    }
}

private class ErrorsDuringTransaction(
    @SerializedName("html")
    val html: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("stacktrace")
    val stacktrace: String?
)