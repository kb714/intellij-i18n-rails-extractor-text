package com.kb714.i18nrailsextractortext

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class ExtractTextAction : AnAction() {
    // como es un plugin, no necesitamos una clase inicial
    // esto esta configurado en el plugin.yml (como acción)
    override fun actionPerformed(e: AnActionEvent) {
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project: Project? = e.project

        if (editor != null && file != null && project != null) {
            val fileExtension = file.extension

            val processor = FileProcessorFactory.getProcessor(fileExtension)

            if (processor != null) {
                processor.processFile(editor, file, project)
            } else {
                Messages.showMessageDialog("No se encontró un procesador para la extensión del archivo: $fileExtension", "Error", Messages.getErrorIcon())
            }

        } else {
            Messages.showMessageDialog("Editor o archivo no disponible.", "Error", Messages.getErrorIcon())
        }
    }
}