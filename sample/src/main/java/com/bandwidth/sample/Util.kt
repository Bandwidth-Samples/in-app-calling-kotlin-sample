package com.bandwidth.sample

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Properties

object Util {

    // Name of the configuration file in the assets directory.
    private const val CONFIG_FILE = "config.properties"

    /**
     * Gets the property value associated with the given key from the configuration file.
     *
     * @param key The key to look up in the configuration file.
     * @param context The application context.
     * @return The value associated with the key, or null if the key is not found.
     * @throws IOException If there is an error reading the configuration file.
     */
    @Throws(IOException::class)
    fun getProperty(key: String?, context: Context): String? {
        return try {
            context.assets.open(CONFIG_FILE).use { inputStream ->
                // Create and configure a Properties object and load properties from the input stream.
                Properties().apply { load(inputStream) }.getProperty(key)
            }
        } catch (e: IOException) {
            throw IOException("File $CONFIG_FILE not found in project", e)
        }
    }


    fun getString(key: String?, context: Context): String {
        return getProperty(key, context).toString()
    }

    fun getInt(key: String?, context: Context): Int {
        return getString(key, context).toInt()
    }

    fun capitalize(s: String?): String {
        if (s.isNullOrEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            first.uppercaseChar().toString() + s.substring(1)
        }
    }

    fun mapToJson(data: Map<String?, String?>): String {
        val obj = JSONObject()
        data.forEach { (s: String?, s2: String?) ->
            try {
                obj.put(s, s2)
            } catch (e: JSONException) {
                Log.e("Notification", "Error while parsing", e.cause)
            }
        }
        return obj.toString()
    }
}
