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
            is PyTupleExpression -> "tuple"  // Direct tuple detection (e.g., a = (1, 2, 3))
            is PySetLiteralExpression -> "set"

            // Handle function definitions
            is PyFunction -> "function"
            is PyLambdaExpression -> "lambda function"

            // Detect class attributes
            is PyReferenceExpression -> {
                val resolved = expression.reference.resolve()

                if (resolved is PyFunction) {
                    return "function assignment - ${resolved.name}"
                }

                // If the reference is inside a class, it's likely a class attribute
                if (resolved is PyTargetExpression) {
                    // Check if the variable is part of a class definition
                    val containingClass = resolved.containingClass
                    if (containingClass != null) {
                        return "class attribute"
                    }

                    // Otherwise, treat it as a regular variable
                    return getTypeFromExpression(resolved.findAssignedValue() ?: expression)
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

            // Handle complex expressions (e.g., call expressions, etc.)
            is PyCallExpression -> {
                val callee = expression.callee
                if (callee is PyReferenceExpression) {
                    // Check if it's a function call and handle accordingly
                    val resolved = callee.reference.resolve()
                    if (resolved is PyFunction) {
                        return "function call"
                    }
                }
                "Unknown type"
            }

            // Handle assignment statements
            is PyAssignmentStatement -> {
                val assignedValue = expression.assignedValue
                if (assignedValue != null) {
                    // Check if the assigned value is a tuple (e.g., a = (1, 2, 3))
                    if (assignedValue is PyTupleExpression) {
                        return "tuple"
                    }
                    // Check for more complex cases (e.g., tuple returned by function)
                    return getTypeFromExpression(assignedValue)  // Handle other types recursively
                }
                "Unknown type"
            }

            // Handle parenthesized expressions (e.g., (a, b) in a function call or assignment)
            is PyParenthesizedExpression -> {
                // Access the contained expression correctly
                val innerExpression = expression.containedExpression
                return getTypeFromExpression(innerExpression ?: expression)  // Fall back to the full expression if null
            }

            else -> "Unknown type"
        }
    }




    override fun dispose() {
        getEditor()?.caretModel?.removeCaretListener(caretListener)
    }
}