package com.bandwidth.sample

import android.content.Context
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
}
