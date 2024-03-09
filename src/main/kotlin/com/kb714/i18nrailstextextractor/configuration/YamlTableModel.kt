package com.kb714.i18nrailstextextractor.configuration
import javax.swing.table.DefaultTableModel

class YamlTableModel : DefaultTableModel() {
    init {
        addColumn("Key")
        addColumn("Value")
    }

    fun addYamlData(key: String, value: String) {
        addRow(arrayOf(key, value))
    }
}
