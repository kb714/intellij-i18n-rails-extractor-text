package com.kb714.i18nrailsextractortext.processors

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class ErbFileProcessor : FileProcessor {
    override fun processFile(editor: Editor, file: VirtualFile, project: Project) {
        Messages.showInfoMessage("Archivo ERB sin implementar", "Information")
    }
}