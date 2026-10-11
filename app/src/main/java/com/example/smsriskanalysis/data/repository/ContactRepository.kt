package com.example.smsriskanalysis.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract

class ContactRepository(
    private val contentResolver: ContentResolver
) {
    fun getContactByPhoneNumber(phoneNumber: String): ContactData? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI
        )

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

                val name = cursor.getString(nameIndex)
                val photoUri = cursor.getString(photoIndex)

                return ContactData(
                    name = name,
                    photoUri = photoUri
                )
            }
        }
        return null
    }

    data class ContactData(
        val name: String,
        val photoUri: String?
    )
}