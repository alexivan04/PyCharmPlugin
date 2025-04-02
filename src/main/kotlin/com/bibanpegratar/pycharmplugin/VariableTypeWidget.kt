package com.bibanpegratar.pycharmplugin

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

class VariableTypeWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    private var text: String = "Loading..."

    override fun ID(): String = "VariableTypeWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = text

    override fun getTooltipText(): String = "Displays caret position"

    override fun getAlignment(): Float = 1f

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer { println("Widget clicked!") }
    }

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)
        myStatusBar = statusBar
        registerCaretListener()
        updateText()

        project.messageBus.connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                registerCaretListener()
                updateText()
            }
        })
    }

    private fun registerCaretListener() {
        val editor = getEditor() ?: return
        editor.caretModel.removeCaretListener(caretListener)
        editor.caretModel.addCaretListener(caretListener)
    }

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            updateText()
        }
    }

    private fun updateText() {
        val editor: Editor? = getEditor()
        text = if (editor != null) {
            val line = editor.caretModel.logicalPosition.line + 1
            val column = editor.caretModel.logicalPosition.column + 1
            "Ln $line, Col $column"
        } else {
            "No editor open"
        }
        myStatusBar?.updateWidget(ID())
    }

    override fun dispose() {
        getEditor()?.caretModel?.removeCaretListener(caretListener)
    }
}
