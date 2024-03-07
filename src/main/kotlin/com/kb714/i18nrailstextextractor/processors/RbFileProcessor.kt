package com.kb714.i18nrailstextextractor.processors

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
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
        val (transformedText, variablesMap) = transformTextForI18nOnRuby(selectedText)
        val textToYaml = removeSurroundingQuotes(transformedText)

        val replacementText = buildI18nCall(i18nKey, variablesMap)

        updateOrCreateYaml(yamlPath, i18nKey, textToYaml)
        replaceSelectedText(editor, project, replacementText)
    }
}