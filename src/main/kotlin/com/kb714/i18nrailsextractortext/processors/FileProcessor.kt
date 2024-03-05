package com.kb714.i18nrailsextractortext.processors

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

        // clear
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
}