package com.example.smsriskanalysis.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                val nameIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
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

    suspend fun searchContacts(query: String): List<ContactSearchData> =
        withContext(Dispatchers.IO) {
            val keyword = query.trim()

            if (keyword.isBlank()) {
                return@withContext emptyList()
            }

            val contacts = mutableListOf<ContactSearchData>()

            val projection = arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME,
                Phone.NUMBER,
                Phone.PHOTO_URI
            )

            val selection = "${Phone.DISPLAY_NAME} LIKE ? OR ${Phone.NUMBER} LIKE ?"

            val searchPattern = "%$keyword%"

            contentResolver.query(
                Phone.CONTENT_URI,
                projection,
                selection,
                arrayOf(searchPattern, searchPattern),
                "${Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(Phone.NUMBER)
                val photoIndex = cursor.getColumnIndexOrThrow(Phone.PHOTO_URI)
                val seen = mutableListOf<String>()

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: continue
                    val number = cursor.getString(numberIndex) ?: continue
                    val photoUri = cursor.getString(photoIndex)

                    val uniqueKey = "$id:${number.filter { it.isDigit() }}"

                    if (!seen.add(uniqueKey)) continue

                    contacts.add(
                        ContactSearchData(
                            id,
                            name,
                            phoneNumber = number,
                            photoUri
                        )
                    )

                    if (contacts.size >= 50) break
                }
            }
            contacts
        }

    data class ContactData(
        val name: String,
        val photoUri: String?
    )

    data class ContactSearchData(
        val id: Long,
        val name: String,
        val phoneNumber: String,
        val photoUri: String? = null
    )
}