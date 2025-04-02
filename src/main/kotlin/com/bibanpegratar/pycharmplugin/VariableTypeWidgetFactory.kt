package com.bibanpegratar.pycharmplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

open class VariableTypeWidgetFactory : StatusBarEditorBasedWidgetFactory() {
    override fun getDisplayName(): String {
        return "PyCharm Variable Type"
    }

    override fun getId(): String {
        return "VariableTypeWidget"
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return VariableTypeWidget(project)
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

}