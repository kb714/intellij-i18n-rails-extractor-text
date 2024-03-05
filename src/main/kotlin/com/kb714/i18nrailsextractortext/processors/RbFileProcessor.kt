package com.kb714.i18nrailsextractortext.processors

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class RbFileProcessor : FileProcessor {
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
        val userInput = Messages.showInputDialog("Ingrese la key del texto", "Información para YAML", Messages.getQuestionIcon(), "", null)

        val translationPath = buildI18nPath(filePath, projectRoot)
        val yamlPath = buildYamlPath(filePath, projectRoot)
        val i18nKey = "$translationPath.$userInput"
        val (transformedText, variablesMap) = transformTextForI18n(selectedText)
        val textToYaml = removeSurroundingQuotes(transformedText)

        val replacementText = buildI18nCall(i18nKey, variablesMap)

        updateOrCreateYaml(yamlPath, i18nKey, textToYaml)
        replaceSelectedText(editor, project, replacementText)
    }

    fun buildI18nCall(i18nKey: String, variablesMap: Map<String, String>): String {
        val i18nArguments = variablesMap.entries.joinToString(separator = ", ") { (slug, originalExpression) ->
            if (originalExpression.contains('.')) {
                // Para expresiones de clase, mantenemos la expresión original como valor
                "$slug: $originalExpression"
            } else {
                // Para variables lo dejamos igual
                "$slug: $slug"
            }
        }

        // configurable?
        val replacementText = if (variablesMap.isNotEmpty()) {
            "I18n.t('$i18nKey', $i18nArguments)"
        } else {
            "I18n.t('$i18nKey')"
        }

        return replacementText
    }

    private fun removeSurroundingQuotes(value: String): String {
        // Verifica si el valor está rodeado por comillas simples o dobles y las quita
        // en teoria el procesador de yml las agrega (snakeyaml)
        return if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            value.substring(1, value.length - 1)
        } else {
            value
        }
    }

    fun transformTextForI18n(originalText: String): Pair<String, Map<String, String>> {
        var transformedText = originalText
        val variablesMap = mutableMapOf<String, String>()

        // las variables ya vienen listas para procesar
        transformedText = transformedText.replace("""#\{([a-z_][^\}]+)\}""".toRegex()) {
            val variableName = it.groupValues[1]
            variablesMap[variableName] = variableName
            "%{$variableName}"
        }

        // Buscamos posibles clases dentro de los strings Class o Namespace::Class
        val classMethodPattern = """#\{([A-Z][\w:]*\w+)\.([a-zA-Z_]+)\(([^\}]*)\)\}""".toRegex()
        transformedText = classMethodPattern.replace(transformedText) { matchResult ->
            // Extrae y transforma el identificador completo de la clase y el método.
            val classPath = matchResult.groupValues[1] // Clase con o sin namespaces
            val methodName = matchResult.groupValues[2]
            val methodArgs = matchResult.groupValues[3]

            val classPathSlug = classPath.replace("::", "_").lowercase()
            val slug = "${classPathSlug}_${methodName}"

            // Prepara la expresión Ruby original para usar como argumento, manteniendo los argumentos del método.
            variablesMap[slug] = if (methodArgs.isNotBlank()) {
                "${classPath}.${methodName}(${methodArgs})"
            } else {
                "${classPath}.${methodName}()"
            }

            "%{$slug}"
        }

        return Pair(transformedText, variablesMap)
    }
}