package com.devfive.vim_switch_ko

import com.intellij.ide.IdeEventQueue
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.cl.PluginAwareClassLoader
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.awt.im.InputContext
import javax.swing.FocusManager

internal class VimSwitchKoListener : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        var enabled = false
        fun enableIdeaVIM(): Boolean {
            if (enabled) return true

//            VIM 플러그인을 중간에 추가할 수 있습니다.
            val pluginDescriptor = PluginManager.getLoadedPlugins().find { it.pluginId == PluginId.getId("IdeaVIM") }
            enabled = pluginDescriptor != null && pluginDescriptor.isEnabled
            return enabled
        }

        fun toEnglishIME(vararg context: InputContext?) {
            for (context in context)
                context!!.setCharacterSubsets(null)
            InputContext.getInstance().setCharacterSubsets(null)
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner.inputContext.setCharacterSubsets(null)
        }

        fun isFocusedEditor(): Boolean {
            return FocusManager.getCurrentManager().focusOwner is EditorComponentImpl
        }

        var editors: List<Any>? = null
        fun loadEditors() {
            val pluginDescriptor = PluginManager.getLoadedPlugins().find { it.pluginId == PluginId.getId("IdeaVIM") }
            if (pluginDescriptor == null)
                return
            val pluginClassLoader = pluginDescriptor.pluginClassLoader as PluginAwareClassLoader
            val injectClass = pluginClassLoader.tryLoadingClass("com.maddyhome.idea.vim.api.VimInjectorKt", true)
            val instance = injectClass?.getMethod("getInjector")!!.invoke(null)
            val editorGroup = instance.javaClass.getMethod("getEditorGroup").invoke(instance)
            editors = editorGroup.javaClass.getMethod("getEditors").invoke(editorGroup) as ArrayList<Any>
        }


        fun isCurrentModeNormal(forcedLoad: Boolean = false): Boolean {
            if (editors == null || forcedLoad) {
                loadEditors()
                if (editors == null)
                    return false
            }
            if (FocusManager.getCurrentManager().focusOwner !is EditorComponentImpl)
                return false

            val currentEditor = (FocusManager.getCurrentManager().focusOwner as EditorComponentImpl).editor

            for (editor in editors!!) {
                val compareEditor = editor.javaClass.getMethod("getEditor").invoke(editor)
                if (compareEditor == currentEditor) {
                    val mode = editor.javaClass.getMethod("getMode").invoke(editor)
                    return mode != null && mode.toString().startsWith("NORMAL")
                }
            }
            return false
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner") {
            if (isFocusedEditor() && enableIdeaVIM() && isCurrentModeNormal(true))
                toEnglishIME((it.newValue as EditorComponentImpl).inputContext)
            if (it.newValue.javaClass.name == "com.maddyhome.idea.vim.ui.ex.ExTextField") {
                // #TODO: Fix font in vim search mode
                // vim search mode
//                val editor = it.newValue.javaClass.getMethod("getEditor").invoke(it.newValue)
//                val component = editor.javaClass.getMethod("getComponent").invoke(editor)
//                val font = component.javaClass.getMethod("getFont").invoke(component)
//                val parent = (it.newValue as JTextField).parent
//                val label = parent.javaClass.getMethod("getLabel").invoke(parent)
//                label.javaClass.getMethod("setFont").invoke(label, font)
            }
        }
        IdeEventQueue.getInstance().addDispatcher(IdeEventQueue.EventDispatcher { e ->
            if (e !is KeyEvent) return@EventDispatcher false
            if (!isFocusedEditor()) {
                return@EventDispatcher false
            }

            if (!enableIdeaVIM())
                return@EventDispatcher false
            if (e.id != KeyEvent.KEY_PRESSED) {
                if (e.keyCode == 0 && e.keyChar.code == 65535 && isCurrentModeNormal()) {
                    // Switching to English IME when switching korean mode in normal mode
                    toEnglishIME(e.component.inputContext)
                }
                return@EventDispatcher false
            }

            if (e.keyCode == KeyEvent.VK_ESCAPE) {
                // Always switch to English IME when pressing ESC
                toEnglishIME(e.component.inputContext)
            } else if (e.isControlDown && (e.keyCode == KeyEvent.VK_C || e.keyCode == KeyEvent.VK_OPEN_BRACKET)) {
                // Switch to English IME when pressing Ctrl+C or Ctrl+[ in normal mode
                if (!isCurrentModeNormal())
                    toEnglishIME(e.component.inputContext)
            }
            return@EventDispatcher false
        }, null)
    }
}
