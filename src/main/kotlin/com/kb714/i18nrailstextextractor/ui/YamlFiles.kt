package com.kb714.i18nrailstextextractor.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import org.yaml.snakeyaml.Yaml
import java.util.concurrent.ConcurrentHashMap

class YamlFiles(private val project: Project) {
    private val i18nValues = ConcurrentHashMap<String, String>()

    init {
        loadI18nFiles()
    }

    fun findKey(key: String): String? {
        return i18nValues[key]
    }

    fun refresh() {
        // este refresh no funciona tan bien
        // cuando el texto recién se está extrayendo
        // no estoy seguro por que, quizás hay que agregar un "loader"
        // en segundo plano que pregunte por cambios en los yml
        i18nValues.clear()
        loadI18nFiles()
    }

    private fun loadI18nFiles() {
        val projectBaseDir = project.baseDir
        VfsUtil.visitChildrenRecursively(projectBaseDir, object : VirtualFileVisitor<Void?>() {
            override fun visitFile(file: VirtualFile): Boolean {
                // configurar >.<
                if (!file.isDirectory && file.name.endsWith("es.yml") && file.path.contains("/config/locales/")) {
                    parseYamlFile(file)
                }
                return true
            }
        })
    }

    private fun parseYamlFile(file: VirtualFile) {
        val yaml = Yaml()
        try {
            val content = yaml.loadAs(file.inputStream, Map::class.java)
            parseYamlContent("", content)
        } catch (e: Exception) {
            // Ignoramos los archivos que dan error, por ejemplo, los que presentan recursividad
            // no podemos procesar cosas como
            // foo: &defaults
            //  bar: Text
            // así que por ahora las saltamos, acepto ideas '(ᗒWᗕ)՞
            println("Error parsing YAML file: ${file.path}, ${e.message}")
        }
    }

    private fun parseYamlContent(prefix: String, content: Any?) {
        when (content) {
            is Map<*, *> -> {
                content.forEach { (key, value) ->
                    val newPrefix = if (prefix.isEmpty()) key.toString() else "$prefix.$key"
                    parseYamlContent(newPrefix, value)
                }
            }
            is String -> i18nValues[prefix] = content
            else -> { }
        }
    }

}
