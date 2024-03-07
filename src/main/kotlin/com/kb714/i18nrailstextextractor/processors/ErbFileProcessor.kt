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

    fun transformTextForI18nOnHTML(originalText: String): Pair<String, MutableMap<String, String>> {
        var transformedText = originalText
        val variablesMap = mutableMapOf<String, String>()

        // Para variables normales tipo <%= variable %>
        val rubyVariablePattern = Regex("<%=?\\s*([a-zA-Z_][\\w.]*)\\s*%>")
        transformedText = rubyVariablePattern.replace(transformedText) { matchResult ->
            val variableName = matchResult.groupValues[1]
            if (!variablesMap.containsKey(variableName)) {
                variablesMap[variableName] = variableName
            }
            "%{${variableName}}"
        }

        // Para clases tipo <%= Foo.human_attribute_name(:bar) %>
        val classMethodPattern = Regex("(?:<%=\\s*|\\#\\{)([A-Z][\\w:]*\\w+)\\.([a-zA-Z_]+)\\(([^\\}]*)\\)(?:\\s*%>|\\})")
        transformedText = classMethodPattern.replace(transformedText) { matchResult ->
            val classPath = matchResult.groupValues[1]
            val methodName = matchResult.groupValues[2]
            val methodArgs = matchResult.groupValues[3]
            val classPathSlug = classPath.replace("::", "_").lowercase()
            val slug = "${classPathSlug}_$methodName"
            val originalExpression = if (methodArgs.isNotBlank()) "$classPath.$methodName($methodArgs)" else "$classPath.$methodName()"
            variablesMap[slug] = originalExpression
            "%{$slug}"
        }

        return Pair(transformedText, variablesMap)
    }
}
