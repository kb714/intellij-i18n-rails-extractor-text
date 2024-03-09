package com.kb714.i18nrailstextextractor.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent

object I18nFinder {
    fun loadMap(): Map<String, String> {
        val i18nMapJson = PropertiesComponent.getInstance().getValue("PluginConfiguration.I18nMap", "")
        return if (i18nMapJson.isNotBlank()) {
            val gson = Gson()
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(i18nMapJson, type)
        } else {
            emptyMap()
        }
    }

    fun keyForText(text: String): String? {
        val i18nMap = loadMap()
        val fullI18nPath = i18nMap.entries.firstOrNull { it.value == text }
        return fullI18nPath?.key?.split(".")?.drop(1)?.joinToString(separator = ".")
    }
}