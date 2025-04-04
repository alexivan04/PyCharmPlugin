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
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.TypeEvalContext
import java.awt.event.MouseEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.util.concurrency.AppExecutorUtil

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
            // Limit updates
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime > 100) { // update at most every 100ms
                lastUpdateTime = now
                updateText()
            }
        }
    }

    private fun updateText() {
        ReadAction.nonBlocking<String> {
            val editor: Editor = getEditor() ?: return@nonBlocking "No editor open"

            val psiFile = com.intellij.psi.util.PsiUtilBase.getPsiFileInEditor(editor, project)
                ?: return@nonBlocking "Not a Python file"

            if (!psiFile.language.`is`(com.jetbrains.python.PythonLanguage.getInstance())) {
                return@nonBlocking "Not a Python file"
            }

            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset) ?: return@nonBlocking "No element at caret"

            val refExpr = com.intellij.psi.util.PsiTreeUtil.getParentOfType(
                element, PyReferenceExpression::class.java, false
            )

            if (refExpr != null) {
                val type = TypeEvalContext.codeAnalysis(project, psiFile).getType(refExpr)
                return@nonBlocking if (type != null) "${refExpr.name}: ${type.name}" else "${refExpr.name}: Unknown type"
            }

            val targetExpr = com.intellij.psi.util.PsiTreeUtil.getParentOfType(
                element, PyTargetExpression::class.java, false
            )

            if (targetExpr != null) {
                val parent = targetExpr.parent
                if (parent is PyAssignmentStatement) {
                    val assignedValue = parent.assignedValue
                    if (assignedValue != null) {
                        return@nonBlocking "${targetExpr.name}: ${getTypeFromExpression(assignedValue)}"
                    }
                }
                return@nonBlocking "${targetExpr.name}: Unknown type"
            }

            return@nonBlocking "No variable at caret"
        }.finishOnUiThread(ApplicationManager.getApplication().defaultModalityState) { resultText ->
            text = resultText
            myStatusBar?.updateWidget(ID())
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun getTypeFromExpression(expression: PyExpression): String {
        return when (expression) {
            is PyStringLiteralExpression -> "str"
            is PyNumericLiteralExpression -> if (expression.text.contains(".")) "float" else "int"
            is PyBoolLiteralExpression -> "bool"
            is PyListLiteralExpression -> "list"
            is PyDictLiteralExpression -> "dict"
            is PyTupleExpression -> "tuple"

            is PyReferenceExpression -> {
                // Try to resolve the reference
                val resolved = expression.reference.resolve()
                if (resolved is PyTargetExpression) {
                    // Recursively determine the type
                    val assignedValue = resolved.findAssignedValue()
                    if (assignedValue != null) {
                        return getTypeFromExpression(assignedValue)
                    }
                }
                "Unknown type"
            }

            is PyBinaryExpression -> {
                val leftType = getTypeFromExpression(expression.leftExpression)
                val rightType = expression.rightExpression?.let { getTypeFromExpression(it) }
                val operator = expression.psiOperator?.text ?: return "Unknown type"

                return when (operator) {
                    "+", "-", "*", "//", "/" -> {
                        if (leftType == "str" || rightType == "str") "str"
                        else if (leftType == "float" || rightType == "float") "float"
                        else "int"
                    }
                    else -> "Unknown type"
                }
            }

            else -> "Unknown type"
        }
    }

    override fun dispose() {
        getEditor()?.caretModel?.removeCaretListener(caretListener)
    }
}