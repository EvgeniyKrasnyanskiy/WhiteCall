package com.whitecall.app.util

import android.content.Context
import android.net.Uri
import com.whitecall.app.domain.model.GroupItem
import com.whitecall.app.domain.model.WhiteListEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupGroupData(
    val id: Long,
    val name: String,
    val isActive: Boolean
)

data class BackupEntryData(
    val displayName: String,
    val phoneNumber: String,
    val groupName: String? = null,
    val groupId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class BackupData(
    val groups: List<BackupGroupData>,
    val entries: List<BackupEntryData>
)

object JsonBackupHelper {

    fun exportToJson(groups: List<GroupItem>, entries: List<WhiteListEntry>): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val groupMap = groups.associateBy { it.id }

        val groupsArray = JSONArray()
        for (g in groups) {
            val gObj = JSONObject()
            gObj.put("id", g.id)
            gObj.put("name", g.name)
            gObj.put("isActive", g.isActive)
            groupsArray.put(gObj)
        }
        root.put("groups", groupsArray)

        val entriesArray = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("displayName", entry.displayName)
            obj.put("phoneNumber", entry.phoneNumber)
            if (entry.groupId != null) {
                obj.put("groupId", entry.groupId)
                val gName = groupMap[entry.groupId]?.name
                if (gName != null) {
                    obj.put("groupName", gName)
                }
            }
            obj.put("createdAt", entry.createdAt)
            entriesArray.put(obj)
        }
        root.put("whitelist", entriesArray)
        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): BackupData {
        val groupsList = mutableListOf<BackupGroupData>()
        val entriesList = mutableListOf<BackupEntryData>()

        val root = JSONObject(jsonString)

        if (root.has("groups")) {
            val groupsArray = root.getJSONArray("groups")
            for (i in 0 until groupsArray.length()) {
                val gObj = groupsArray.getJSONObject(i)
                val id = gObj.optLong("id", (i + 1).toLong())
                val name = gObj.optString("name", "Основная")
                val isActive = gObj.optBoolean("isActive", true)
                groupsList.add(BackupGroupData(id = id, name = name, isActive = isActive))
            }
        }

        if (root.has("whitelist")) {
            val array = root.getJSONArray("whitelist")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val displayName = obj.optString("displayName", "")
                val phoneNumber = obj.optString("phoneNumber", "")
                val groupName = if (obj.has("groupName")) obj.optString("groupName") else null
                val groupId = if (obj.has("groupId")) obj.optLong("groupId") else null
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                if (phoneNumber.isNotBlank()) {
                    entriesList.add(
                        BackupEntryData(
                            displayName = displayName.ifBlank { phoneNumber },
                            phoneNumber = phoneNumber,
                            groupName = groupName,
                            groupId = groupId,
                            createdAt = createdAt
                        )
                    )
                }
            }
        }

        return BackupData(groups = groupsList, entries = entriesList)
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
