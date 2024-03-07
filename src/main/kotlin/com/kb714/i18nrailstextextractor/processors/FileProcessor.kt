package com.kb714.i18nrailstextextractor.processors

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

interface FileProcessor {
    fun processFile(editor: Editor, file: VirtualFile, project: Project)

    fun decomposeKey(key: String): List<String> = key.split(".")

    fun replaceSelectedText(editor: Editor, project: Project, replacementText: String) {
        val document = editor.document
        val selectionModel = editor.selectionModel
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        // Ejecuta el reemplazo como una acción atómica para deshacer en un solo paso si es necesario
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, replacementText)
        }

        // no estoy seguro de que esto sea necesario
        selectionModel.removeSelection()
    }

    fun buildYamlPath(filePath: String, projectRoot: String): String {
        // nota: esto hay que dejarlo configurable <.<
        val isPack = filePath.contains("/packs/")
        val basePath: String
        val relativePath: String

        if (isPack) {
            // Extrae la ruta del pack desde 'filePath' y construye la base hasta 'config/locales'
            val packPath = filePath.substringAfter("$projectRoot/packs/")
                    .substringBefore("/app/")
            basePath = "$projectRoot/packs/$packPath/config/locales"
            relativePath = filePath.substringAfter("/app/")
        } else {
            basePath = "$projectRoot/config/locales"
            relativePath = filePath.removePrefix("$projectRoot/app/")
        }

        // configurar también ... no siempre la base será es
        val locale = "es"

        val directoryPath = relativePath.substringBeforeLast("/")
        val fileName = relativePath.substringAfterLast("/")
                .substringBeforeLast(".") + "/${locale}.yml"

        return "$basePath/$directoryPath/$fileName"
    }

    fun updateOrCreateYaml(yamlPath: String, key: String, value: String) {
        // Configura las opciones de volcado para mejorar el formato
        // esto también lo podríamos configurar, esta acoplado a rails
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        // por alguna razón la opción explicitStart es privada ...
        options.isExplicitStart = true

        val yaml = Yaml(options)
        val file = File(yamlPath)
        // si el directorio o archivos no existen, los creamos
        file.parentFile.mkdirs()
        file.createNewFile()
        // configurar :z
        val fullPath = "es.$key"
        val decomposedKey = decomposeKey(fullPath)

        // Carga el archivo YAML existente o inicializa un nuevo mapa si no existe
        val yamlContent: MutableMap<String, Any> = if (file.exists()) {
            FileInputStream(file).use { inputStream ->
                yaml.load<Map<String, Any>>(inputStream) ?: mutableMapOf()
            }.toMutableMap()
        } else {
            mutableMapOf()
        }

        updateYamlContent(yamlContent, decomposedKey, value)

        // Guardamos el mapa actualizado en el archivo YAML
        FileWriter(file).use { writer ->
            yaml.dump(mapOf("es" to yamlContent["es"]), writer)
        }
    }

    fun updateYamlContent(currentContent: MutableMap<String, Any>, keyParts: List<String>, value: String) {
        var currentLevel = currentContent

        for (i in 0 until keyParts.size - 1) {
            val part = keyParts[i]
            if (!currentLevel.containsKey(part)) {
                currentLevel[part] = mutableMapOf<String, Any>()
            }
            currentLevel = currentLevel[part] as MutableMap<String, Any>
        }

        currentLevel[keyParts.last()] = value
    }

    fun buildI18nPath(filePath: String, projectRoot: String): String {
        // Elimina el prefijo del proyecto y cualquier parte de la ruta antes de "/app/"
        // ya que packwerk usa su propia carpeta, no deberíamos tener problemas
        val relevantPath = filePath
                .removePrefix(projectRoot)
                .substringAfter("/app/")

        // esto es para generar la ruta de I18n, estilo ruta.de.la.traduccion
        // la key la ingresamos manual
        val translationPath = relevantPath
                .substringBeforeLast(".")
                .replace('/', '.')

        return translationPath
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

    fun removeSurroundingQuotes(value: String): String {
        // Verifica si el valor está rodeado por comillas simples o dobles y las quita
        // en teoria el procesador de yml las agrega (snakeyaml)
        return if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            value.substring(1, value.length - 1)
        } else {
            value
        }
    }

    fun transformTextForI18nOnRuby(originalText: String): Pair<String, Map<String, String>> {
        val variablesMap = mutableMapOf<String, String>()

        val interpolationPattern = """#\{([^\}]+)\}""".toRegex()
        val transformedText = interpolationPattern.replace(originalText) { matchResult ->
            val fullExpression = matchResult.groupValues[1].trim()

            // Determina si la expresión es una variable simple o algo más complejo
            val isSimpleVariable = fullExpression.all { it.isLetterOrDigit() || it == '_' }

            val slug = if (isSimpleVariable) {
                fullExpression
            } else {
                // Para expresiones complejas limpiamos cualqueir caracter raro
                val cleanedExpression = fullExpression
                        .replace(Regex("""\([^\)]*\)"""), "")
                        .replace(Regex("""\[[^\]]*\]"""), "")

                cleanedExpression
                        .replace(Regex("[^a-zA-Z0-9_]"), "_")
                        .lowercase()
            }
            // nos aseguramos de que no parta ni terine con _, ni que tenga mas de un _ seguido
            val processedSlug = slug.replace(Regex("_+"), "_").trim('_')

            variablesMap[processedSlug] = fullExpression
            "%{$processedSlug}"
        }

        return Pair(transformedText, variablesMap)
    }
}