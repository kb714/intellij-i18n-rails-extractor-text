package com.kb714.i18nrailstextextractor

import com.google.gson.Gson
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.kb714.i18nrailstextextractor.configuration.YamlTableModel
import com.kb714.i18nrailstextextractor.utils.I18nFinder
import org.yaml.snakeyaml.Yaml
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.FileInputStream
import javax.swing.*

class PluginConfiguration : Configurable {
    private var selectedFiles = mutableListOf<VirtualFile>()
    private val listModel = DefaultListModel<String>()
    private val filesList = JBList(listModel)
    private val tableModel = YamlTableModel()
    private val keysTable = JBTable(tableModel)
    private val i18nUtil = I18nFinder

    override fun getDisplayName(): String = "Extractor Settings"
    override fun isModified(): Boolean = false

    override fun createComponent(): JComponent {
        loadSelectedFiles()
        loadKeysTable()
        val mainPanel = JPanel(BorderLayout())
        setupFilesListPanel(mainPanel)
        setupKeysTablePanel(mainPanel)
        setupButtonsPanel(mainPanel)
        return mainPanel
    }

    private fun setupFilesListPanel(mainPanel: JPanel) {
        val scrollPane = JBScrollPane(filesList)
        val scrollPaneContainer = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(-1, 200)
            add(scrollPane, BorderLayout.CENTER)
        }

        mainPanel.add(scrollPaneContainer, BorderLayout.NORTH)
    }


    private fun setupKeysTablePanel(mainPanel: JPanel) {
        val tableScrollPane = JBScrollPane(keysTable)
        mainPanel.add(tableScrollPane, BorderLayout.CENTER)
    }

    private fun setupButtonsPanel(mainPanel: JPanel) {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val selectButton = JButton("Select YAML Files").apply {
            addActionListener {
                val descriptor = FileChooserDescriptor(true, false, true, false, true, true)
                    .withFileFilter { it.extension == "yml" || it.extension == "yaml" }
                val files = FileChooser.chooseFiles(descriptor, null, null)
                val newFiles = files.filterNot { selectedFile -> selectedFiles.any { it.path == selectedFile.path } }
                if (newFiles.isNotEmpty()) {
                    selectedFiles.addAll(newFiles)
                    refreshListModel()
                    refreshKeysTable()
                    saveSelectedFiles()
                }
            }
        }

        val removeButton = JButton("Remove Selected").apply {
            addActionListener {
                filesList.selectedIndices.reversedArray().forEach {
                    selectedFiles.removeAt(it)
                    listModel.remove(it)
                }
                refreshKeysTable()
                saveSelectedFiles()
            }
        }

        val clearButton = JButton("Clear All").apply {
            addActionListener {
                selectedFiles.clear()
                listModel.clear()
                refreshKeysTable()
                saveSelectedFiles()
            }
        }

        buttonPanel.add(selectButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(clearButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
    }
    override fun apply() {
        saveSelectedFiles()
    }

    private fun loadSelectedFiles() {
        val paths = PropertiesComponent.getInstance().getValue("PluginConfiguration.SelectedFiles", "")
        selectedFiles = paths.split(";").filter { it.isNotEmpty() }.mapNotNull {
            LocalFileSystem.getInstance().findFileByPath(it)
        }.toMutableList()
        refreshListModel()
        refreshKeysTable()
    }

    private fun loadKeysTable() {
        val i18nMap = i18nUtil.loadMap()
        tableModel.rowCount = 0

        i18nMap.forEach { (key, value) ->
            tableModel.addYamlData(key, value)
        }
    }

    private fun refreshListModel() {
        listModel.removeAllElements()
        selectedFiles.forEach { file -> listModel.addElement(file.path) }
    }

    private fun refreshKeysTable() {
        val i18nMap = mutableMapOf<String, String>()
        val yaml = Yaml()
        selectedFiles.forEach { file ->
            try {
                FileInputStream(file.path).use { inputStream ->
                    val data = yaml.load<Map<String, Any>>(inputStream)
                    i18nMap.putAll(flattenMap("", data))
                }
            } catch (e: Exception) {
                println("Error al analizar el archivo YAML: ${file.path}")
            }
        }

        tableModel.setRowCount(0)
        i18nMap.forEach { (key, value) ->
            tableModel.addRow(arrayOf(key, value))
        }

        saveI18nMap(i18nMap)
    }

    private fun flattenMap(prefix: String, map: Map<String, Any>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        map.forEach { (key, value) ->
            val fullKey = if (prefix.isBlank()) key else "$prefix.$key"
            when (value) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    result.putAll(flattenMap(fullKey, value as Map<String, Any>))
                }
                else -> {
                    result[fullKey] = value.toString()
                }
            }
        }
        return result
    }

    private fun saveI18nMap(i18nMap: Map<String, String>) {
        val gson = Gson()
        val i18nMapJson = gson.toJson(i18nMap)
        PropertiesComponent.getInstance().setValue("PluginConfiguration.I18nMap", i18nMapJson)
    }

    private fun saveSelectedFiles() {
        val paths = selectedFiles.joinToString(separator = ";") { it.path }
        PropertiesComponent.getInstance().setValue("PluginConfiguration.SelectedFiles", paths)
    }
}
