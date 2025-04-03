package com.bibanpegratar.pycharmplugin

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import java.awt.event.MouseEvent

class VariableTypeWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    private var text: String = "Loading..."
    private var lastUpdateTime: Long = 0

    override fun ID(): String = "VariableTypeWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = text

    override fun getTooltipText(): String = "Displays variable type at caret position"

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
            // limit updates
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime > 100) { // update at most every 100ms
                lastUpdateTime = now
                updateText()
            }
        }
    }

    private fun updateText() {
        val editor: Editor? = getEditor()
        if (editor == null) {
            text = "No editor open"
            myStatusBar?.updateWidget(ID())
            return
        }

        val psiFile = com.intellij.psi.util.PsiUtilBase.getPsiFileInEditor(editor, project) ?: run {
            text = "Not a Python file"
            myStatusBar?.updateWidget(ID())
            return
        }

        if (!psiFile.language.`is`(com.jetbrains.python.PythonLanguage.getInstance())) {
            text = "Not a Python file"
            myStatusBar?.updateWidget(ID())
            return
        }

        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: run {
            text = "No element at caret"
            myStatusBar?.updateWidget(ID())
            return
        }

        val refExpr = com.intellij.psi.util.PsiTreeUtil.getParentOfType(
            element,
            PyReferenceExpression::class.java,
            false
        ) ?: run {
            text = "No variable at caret"
            myStatusBar?.updateWidget(ID())
            return
        }

        // create the evaluation context
        val type = TypeEvalContext.codeAnalysis(project, psiFile).getType(refExpr)

        text = if (type != null) {
            "${refExpr.name}: ${type.name}"
        } else {
            "${refExpr.name}: Unknown type"
        }

        myStatusBar?.updateWidget(ID())
    }

    override fun dispose() {
        getEditor()?.caretModel?.removeCaretListener(caretListener)
    }
}