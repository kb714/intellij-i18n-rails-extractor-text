import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.table.JBTable
import com.intellij.util.messages.MessageBusConnection
import com.kb714.i18nrailstextextractor.ui.YamlFiles
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JScrollPane

object FileEditorListener {
    private var currentFile: VirtualFile? = null
    private lateinit var yamlFiles: YamlFiles
    fun setupListener(project: Project, panel: JPanel) {
        val connection: MessageBusConnection = project.messageBus.connect(project)
        val tableModel = TranslationKeyTableModel()
        val table = JBTable(tableModel)
        yamlFiles = YamlFiles(project)
        table.fillsViewportHeight = true

        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Refresh", "Refresh list", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    refresh(tableModel)
                }
            })
        }

        val toolbar = ActionToolbarImpl("FileEditorListenerToolbar", actionGroup, true).apply {
            setTargetComponent(panel)
        }

        val toolbarPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
        }

        panel.add(toolbarPanel, BorderLayout.EAST)
        panel.add(JScrollPane(table), BorderLayout.CENTER)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    if (row >= 0 && currentFile != null) {
                        val offset = tableModel.getOffsetAt(row) + 6 // así dejamos el mouse en la t
                        navigateToFileLocation(project, currentFile!!, offset)
                    }
                }
            }
        })

        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                currentFile = event.newFile
                updatePanelForFile(currentFile, tableModel)
            }
        })
    }

    private fun updatePanelForFile(file: VirtualFile?, tableModel: TranslationKeyTableModel) {
        when {
            file == null || (!file.name.endsWith(".rb") && !file.name.endsWith(".erb")) -> tableModel.setData(emptyList())
            else -> {
                val text = String(file.contentsToByteArray(), Charsets.UTF_8)
                val keys = Regex("""I18n\.t\((["'])(.*?)\1[,)]""")
                    .findAll(text)
                    .mapNotNull { matchResult ->
                        val keyText = matchResult.groupValues[2].trim()
                        if (keyText.isNotEmpty()) {
                            val i18nValue = yamlFiles.findKey("es.$keyText")
                            TranslationKey(keyText, matchResult.range.first, i18nValue ?: "undefined")
                        } else null
                    }
                    .toList()
                tableModel.setData(keys)
            }
        }
    }

    fun navigateToFileLocation(project: Project, file: VirtualFile, offset: Int) {
        val descriptor = OpenFileDescriptor(project, file, offset)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    private fun refresh(tableModel: TranslationKeyTableModel) {
        yamlFiles.refresh()
        currentFile?.let { updatePanelForFile(it, tableModel) }
    }
}
