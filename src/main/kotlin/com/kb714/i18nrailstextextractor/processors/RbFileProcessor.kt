package com.kb714.i18nrailstextextractor.processors

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.kb714.i18nrailstextextractor.utils.I18nFinder
import com.kb714.i18nrailstextextractor.utils.UserInteraction

class RbFileProcessor : FileProcessor {
    override fun processFile(editor: Editor, file: VirtualFile, project: Project) {
        val selectedText = editor.selectionModel.selectedText
        val filePath = file.path
        val projectRoot = project.basePath ?: return
        val i18nUtil = I18nFinder
        val interactionUtil = UserInteraction

        if (selectedText == null) {
            // nota personal: usa i18n
            Messages.showInfoMessage("No hay texto seleccionado", "Información")
            return
        }

        val translationPath = buildI18nPath(filePath, projectRoot)
        val yamlPath = buildYamlPath(filePath, projectRoot)
        val (transformedText, variablesMap) = transformTextForI18nOnRuby(selectedText)
        val textToYaml = removeSurroundingQuotes(transformedText)

        // vamos a buscar si el texto ya existe en los yml de base
        val existingKey = i18nUtil.keyForText(textToYaml)

        if (existingKey.isNullOrEmpty()) {
            val userInput = interactionUtil.askForInput() ?: return
            val i18nKey = "$translationPath.$userInput"
            updateOrCreateYaml(yamlPath, i18nKey, textToYaml)
            replaceSelectedText(editor, project, buildI18nCall(i18nKey, variablesMap))
        } else {
            replaceSelectedText(editor, project, buildI18nCall(existingKey, variablesMap))
        }
    }
}