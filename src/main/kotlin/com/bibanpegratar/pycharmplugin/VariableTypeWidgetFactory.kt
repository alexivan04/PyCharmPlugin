package com.bibanpegratar.pycharmplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

open class VariableTypeWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "VariableTypeWidget"  // Unique ID for your widget
    }

    override fun getDisplayName(): String {
        return "VariableTypeWidget"
    }

    override fun createWidget(project: Project): VariableTypeWidget {
        return VariableTypeWidget()  // Create an instance of your custom widget
    }
}