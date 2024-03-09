package com.kb714.i18nrailstextextractor.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class FileInformationFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = JPanel(BorderLayout()).apply {
            add(JLabel("No file selected"))
        }
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        FileEditorListener.setupListener(project, panel)
    }
}
