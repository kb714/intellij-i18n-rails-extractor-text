package com.kb714.i18nrailstextextractor.processors

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.kb714.i18nrailstextextractor.utils.I18nFinder
import com.kb714.i18nrailstextextractor.utils.UserInteraction

class ErbFileProcessor : FileProcessor {

    override fun processFile(editor: Editor, file: VirtualFile, project: Project) {
        val selectedText = editor.selectionModel.selectedText
        val filePath = file.path
        val projectRoot = project.basePath ?: return

        if (selectedText == null) {
            // nota personal: usa i18n
            Messages.showInfoMessage("No hay texto seleccionado", "Información")
            return
        }

        val yamlPath = buildYamlPath(filePath, projectRoot)
        val isWithinERBTags = isWithinERBTags(editor)
        val (textToYaml, variablesMap) = transformTextForI18n(selectedText, isWithinERBTags)
        val existingKey = I18nFinder.keyForText(textToYaml)

        val i18nKey = if (existingKey.isNullOrEmpty()) {
            val translationPath = buildI18nPath(filePath, projectRoot)
            val userInput = UserInteraction.askForInput() ?: return
            "$translationPath.$userInput"
        } else {
            existingKey
        }

        val textToFile = if (isWithinERBTags) {
            buildI18nCall(i18nKey, variablesMap)
        } else {
            val replacementText = buildI18nCall(i18nKey, variablesMap)
            "<%= $replacementText %>"
        }

        if (existingKey.isNullOrEmpty()) {
            updateOrCreateYaml(yamlPath, i18nKey, textToYaml)
            replaceSelectedText(editor, project, textToFile)
        } else {
            replaceSelectedText(editor, project, textToFile)
        }
    }

    private fun transformTextForI18n(selectedText: String, isWithinERBTags: Boolean): Pair<String, Map<String, String>> {
        if (isWithinERBTags) {
            val (transformedText, variablesMap) = transformTextForI18nOnRuby(selectedText)
            val cleanedText = removeSurroundingQuotes(transformedText)
            return Pair(cleanedText, variablesMap)
        } else {
            return transformTextForI18nOnHTML(selectedText)
        }
    }

    private fun isWithinERBTags(editor: Editor): Boolean {
        val document = editor.document
        val selectionModel = editor.selectionModel
        val selectionStart = selectionModel.selectionStart
        val textBeforeSelection = document.getText(TextRange(0, selectionStart))

        // Busca la última etiqueta de apertura de ERB antes del texto seleccionado
        val lastOpeningTagIndex = maxOf(textBeforeSelection.lastIndexOf("<%="), textBeforeSelection.lastIndexOf("<%"))

        // Si no hay etiquetas de apertura, entonces no está dentro de un bloque ERB
        if (lastOpeningTagIndex == -1) {
            return false
        }

        // Busca la última etiqueta de cierre de ERB antes del texto seleccionado
        val lastClosingTagIndex = textBeforeSelection.lastIndexOf("%>")

        // Si la última etiqueta de apertura está después de la última etiqueta de cierre,
        // entonces la selección está dentro de un bloque ERB
        return lastOpeningTagIndex > lastClosingTagIndex
    }

    // este método es casi igual al de rb, pero lo dejo por separado por si tenemos que
    // procesar cosas propias de uno u otro
    fun transformTextForI18nOnHTML(originalText: String): Pair<String, MutableMap<String, String>> {
        val variablesMap = mutableMapOf<String, String>()

        // capturamos variables que vivan entre <%= %> y <% %>
        val interpolationPattern = """<%=?\s*([^%]+?)\s*%>""".toRegex()
        val transformedText = interpolationPattern.replace(originalText) { matchResult ->
            val fullExpression = matchResult.groupValues[1].trim()

            // Determina si la expresión es una variable simple o algo más complejo
            val isSimpleVariable = fullExpression.all { it.isLetterOrDigit() || it == '_' }
                    && !fullExpression.contains(" ")

            val slug = if (isSimpleVariable) {
                fullExpression
            } else {
                fullExpression
                    .replace(Regex("""\([^)]*\)"""), "")
                    .replace(Regex("""\[[^]]*]"""), "")
                    .replace(Regex("[^a-zA-Z0-9_]"), "_")
                    .replace(Regex("_+"), "_")
                    .trim('_')
                    .lowercase()
            }

            variablesMap[slug] = fullExpression
            "%{$slug}"
        }

        return Pair(transformedText, variablesMap)
    }
}
