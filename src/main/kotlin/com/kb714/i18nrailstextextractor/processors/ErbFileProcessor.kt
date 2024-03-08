package com.kb714.i18nrailstextextractor.processors

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile

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

        // de alguna forma hay que generar esto automaticamente en caso de nulo
        // o validar que siempre se ingrese algo
        val userInput = Messages.showInputDialog(
                "Ingresa la clave del texto a Extraer",
                "Información",
                Messages.getQuestionIcon(),
                "",
                object : InputValidator {
                    override fun checkInput(inputString: String?): Boolean {
                        return !inputString.isNullOrEmpty()
                    }
                    override fun canClose(inputString: String?): Boolean {
                        return checkInput(inputString)
                    }
                })

        val translationPath = buildI18nPath(filePath, projectRoot)
        val yamlPath = buildYamlPath(filePath, projectRoot)
        val i18nKey = "$translationPath.$userInput"
        val textToYaml: String

        val isWithinERBTags = isWithinERBTags(editor)

        val replacementText = if (isWithinERBTags) {
            // ruby
            val (transformedText, variablesMap) = transformTextForI18nOnRuby(selectedText)
            textToYaml = removeSurroundingQuotes(transformedText)
            val buildText = buildI18nCall(i18nKey, variablesMap)
            buildText
        } else {
            // html? erb
            val (transformedText, variablesMap) = transformTextForI18nOnHTML(selectedText)
            textToYaml = transformedText
            val buildText = buildI18nCall(i18nKey, variablesMap)
            "<%= $buildText %>"
        }


        updateOrCreateYaml(yamlPath, i18nKey, textToYaml)
        replaceSelectedText(editor, project, replacementText)
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
