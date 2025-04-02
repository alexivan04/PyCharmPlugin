package com.bibanpegratar.pycharmplugin

import com.intellij.openapi.wm.StatusBarWidget
import com.sun.java.swing.ui.StatusBar
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class VariableTypeWidget : StatusBarWidget {

    override fun ID(): String { return "VariableTypeWidget" }

    fun getComponent(): JComponent {
        return JPanel(BorderLayout())
    }

    fun install(statusBar: StatusBar) {
        statusBar.add(getComponent())
    }

    override fun dispose() {
        // Clean up resources if needed

    }
}