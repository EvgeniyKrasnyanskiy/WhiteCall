package com.whitecall.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactHelper {

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getContactNameByNumber(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank() || !hasContactsPermission(context)) {
            return null
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) cursor.getString(nameIndex) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    data class ContactPickResult(val name: String, val phoneNumber: String)

    fun extractContactFromUri(context: Context, contactUri: Uri): ContactPickResult? {
        var name = ""
        var phoneNumber = ""

        try {
            context.contentResolver.query(
                contactUri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }

                    val id = if (idIndex != -1) cursor.getString(idIndex) else null
                    val hasPhone = if (hasPhoneIndex != -1) cursor.getInt(hasPhoneIndex) > 0 else false

                    if (hasPhone && id != null) {
                        val pCur = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        pCur?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val phoneNumIndex = phoneCursor.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                                )
                                if (phoneNumIndex != -1) {
                                    phoneNumber = phoneCursor.getString(phoneNumIndex) ?: ""
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (phoneNumber.isNotBlank()) {
            ContactPickResult(name = name.ifBlank { phoneNumber }, phoneNumber = phoneNumber)
        } else {
            null
        }
    }
}
