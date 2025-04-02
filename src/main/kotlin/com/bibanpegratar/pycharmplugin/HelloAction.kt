package com.bibanpegratar.pycharmplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class HelloAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        Messages.showMessageDialog(e.getProject(), "Toolbar Button Clicked", "Info", Messages.getInformationIcon());
    }
}