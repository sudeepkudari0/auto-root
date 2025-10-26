package com.assistant.root.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactManager {
    private static final String TAG = "ContactManager";
    private final Context context;
    private final ContentResolver contentResolver;
    private Map<String, String> contactMap; // name -> phone number

    public ContactManager(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
        this.contactMap = new HashMap<>();

        // Check permission before loading contacts
        if (hasContactsPermission()) {
            loadContacts();
        } else {
            Log.w(TAG, "Contacts permission not granted. Contacts will not be loaded.");
        }
    }

    /**
     * Check if contacts permission is granted
     */
    public boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Load all contacts from the device
     */
    private void loadContacts() {
        if (!hasContactsPermission()) {
            Log.w(TAG, "Cannot load contacts - permission not granted");
            return;
        }

        try {
            Cursor cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            if (cursor != null) {
                int contactCount = 0;
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));

                    if (name != null && phone != null) {
                        // Clean phone number (remove spaces, dashes, etc.)
                        String cleanPhone = cleanPhoneNumber(phone);
                        if (!cleanPhone.isEmpty()) {
                            // Store both original name and lowercase for matching
                            contactMap.put(name.toLowerCase(), cleanPhone);
                            contactMap.put(name, cleanPhone);
                            contactCount++;
                        }
                    }
                }
                cursor.close();

                Log.i(TAG, "✅ Contacts acquired! Loaded " + contactCount + " contacts");
            } else {
                Log.w(TAG, "Failed to query contacts - cursor is null");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied while loading contacts: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error loading contacts: " + e.getMessage());
        }
    }

    /**
     * Clean phone number by removing non-digit characters except +
     */
    private String cleanPhoneNumber(String phone) {
        if (phone == null)
            return "";

        // Remove spaces, dashes, parentheses, etc.
        String cleaned = phone.replaceAll("[^+0-9]", "");

        // If it starts with +, keep it, otherwise ensure it starts with country code
        if (cleaned.startsWith("+")) {
            return cleaned.substring(1); // Remove + for WhatsApp URL
        } else if (cleaned.length() >= 10) {
            return cleaned;
        }

        return "";
    }

    /**
     * Get phone number for a contact name (fuzzy matching)
     */
    public String getPhoneNumber(String contactName) {
        if (contactName == null || contactName.trim().isEmpty()) {
            return null;
        }

        String name = contactName.trim();
        String nameLower = name.toLowerCase();

        Log.d(TAG, "Looking for contact: '" + name + "' (lowercase: '" + nameLower + "')");

        // Direct match (case sensitive)
        if (contactMap.containsKey(name)) {
            Log.d(TAG, "Found direct match (case sensitive): " + name);
            return contactMap.get(name);
        }

        // Direct match (case insensitive)
        if (contactMap.containsKey(nameLower)) {
            Log.d(TAG, "Found direct match (case insensitive): " + nameLower);
            return contactMap.get(nameLower);
        }

        // Fuzzy matching - check if any contact name contains the given name
        for (Map.Entry<String, String> entry : contactMap.entrySet()) {
            String contactKey = entry.getKey();
            String contactKeyLower = contactKey.toLowerCase();

            // Check if contact name contains the search name (case insensitive)
            if (contactKeyLower.contains(nameLower) || nameLower.contains(contactKeyLower)) {
                Log.d(TAG, "Found fuzzy match: '" + contactKey + "' contains '" + name + "'");
                return entry.getValue();
            }
        }

        // Log available contacts for debugging
        Log.d(TAG, "No match found. Available contacts: " + contactMap.keySet());

        // If no match found, return null
        return null;
    }

    /**
     * Get all contact names
     */
    public List<String> getAllContactNames() {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, String> entry : contactMap.entrySet()) {
            String key = entry.getKey();
            // Only add original case names (not lowercase duplicates)
            if (!key.equals(key.toLowerCase())) {
                names.add(key);
            }
        }
        return names;
    }

    /**
     * Check if a contact exists
     */
    public boolean hasContact(String contactName) {
        return getPhoneNumber(contactName) != null;
    }

    /**
     * Get contact count
     */
    public int getContactCount() {
        return contactMap.size() / 2; // Divide by 2 because we store both cases
    }

    /**
     * Refresh contacts (call this if contacts are updated)
     */
    public void refreshContacts() {
        contactMap.clear();
        loadContacts();
    }

    /**
     * Load contacts after permission is granted
     */
    public void loadContactsAfterPermissionGranted() {
        if (hasContactsPermission()) {
            Log.i(TAG, "📞 Contacts permission granted! Loading contacts...");
            contactMap.clear();
            loadContacts();
        } else {
            Log.w(TAG, "Contacts permission still not granted");
        }
    }

    /**
     * Get all contact names for debugging
     */
    public void logAllContacts() {
        Log.d(TAG, "=== ALL CONTACTS ===");
        for (Map.Entry<String, String> entry : contactMap.entrySet()) {
            Log.d(TAG, "Contact: '" + entry.getKey() + "' -> " + entry.getValue());
        }
        Log.d(TAG, "=== END CONTACTS ===");
    }
}
