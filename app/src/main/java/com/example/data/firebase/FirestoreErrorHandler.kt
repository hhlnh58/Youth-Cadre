package com.example.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE,
    BATCH,
    TRANSACTION
}

fun handleFirestoreError(e: Exception, operation: OperationType, path: String) {
    val errorReport = JSONObject().apply {
        put("timestamp", System.currentTimeMillis())
        put("operation", operation.name)
        put("path", path)
        put("exceptionClass", e.javaClass.simpleName)
        put("message", e.localizedMessage ?: "Unknown error")
        if (e is FirebaseFirestoreException) {
            put("errorCode", e.code.name)
        }
    }
    Log.e("FirestoreError", errorReport.toString(2), e)
}
