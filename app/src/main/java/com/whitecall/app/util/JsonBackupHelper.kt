package com.whitecall.app.util

import android.content.Context
import android.net.Uri
import com.whitecall.app.domain.model.WhiteListEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object JsonBackupHelper {

    fun exportToJson(entries: List<WhiteListEntry>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("displayName", entry.displayName)
            obj.put("phoneNumber", entry.phoneNumber)
            obj.put("createdAt", entry.createdAt)
            array.put(obj)
        }
        root.put("whitelist", array)
        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): List<WhiteListEntry> {
        val list = mutableListOf<WhiteListEntry>()
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("whitelist")

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val displayName = obj.optString("displayName", "")
            val phoneNumber = obj.optString("phoneNumber", "")
            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

            if (phoneNumber.isNotBlank()) {
                list.add(
                    WhiteListEntry(
                        displayName = displayName.ifBlank { phoneNumber },
                        phoneNumber = phoneNumber,
                        normalizedNumber = "",
                        createdAt = createdAt
                    )
                )
            }
        }
        return list
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
