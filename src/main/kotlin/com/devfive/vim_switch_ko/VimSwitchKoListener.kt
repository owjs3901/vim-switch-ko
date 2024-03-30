package com.devfive.vim_switch_ko

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import java.awt.event.KeyEvent
import java.awt.im.InputContext

class VimSwitchKoListener : AppLifecycleListener {
    override fun appStarted() {
        super.appStarted()
        var enabled = false
        fun enableIdeaVIM(): Boolean {
            if (enabled) return true

//            VIM 플러그인을 중간에 추가할 수 있습니다.
            val pluginDescriptor = PluginManager.getLoadedPlugins().find { it.pluginId == PluginId.getId("IdeaVIM") }
            enabled = pluginDescriptor != null && pluginDescriptor.isEnabled
            return enabled
        }

        fun toEnglishIME(event: KeyEvent) {
            val context = (event.source.javaClass.getMethod("getInputContext").invoke(event.source) as InputContext)
            context.setCharacterSubsets(null)
            InputContext.getInstance().setCharacterSubsets(null)
        }

        var editors:List<Any>?  = null
        fun loadEditors(){
            val pluginDescriptor = PluginManager.getLoadedPlugins().find { it.pluginId == PluginId.getId("IdeaVIM") }
            val pluginClassLoader = pluginDescriptor!!.classLoader
            val injectClass = pluginClassLoader.loadClass("com.maddyhome.idea.vim.api.VimInjectorKt")
            val instance = injectClass?.getMethod("getInjector")!!.invoke(null)
            val editorGroup = instance.javaClass.getMethod("getEditorGroup").invoke(instance)
            editors = editorGroup.javaClass.getMethod("getEditors").invoke(editorGroup) as ArrayList<Any>

        }


        fun isCurrentModeNormal(): Boolean {
            ProjectManager.getInstance().openProjects.forEach { project ->
                if(editors == null){
                    loadEditors()
                    if(editors == null)
                        return false
                }
                val currentFile = FileEditorManager.getInstance(project).selectedEditor?.file?.path ?: return false
                for (editor in editors!!) {
                    val virtualFilePath = editor.javaClass.getMethod("getPath").invoke(editor)
                    if (virtualFilePath == currentFile) {
                        val mode = editor.javaClass.getMethod("getMode").invoke(editor)
                        return mode != null && mode.toString().startsWith("NORMAL")
                    }
                }
            }
            return false
        }
        IdeEventQueue.getInstance().addDispatcher(IdeEventQueue.EventDispatcher { e ->
            if (e !is KeyEvent) return@EventDispatcher false
            if (!enableIdeaVIM()) {
                return@EventDispatcher false
            }
            if (e.id != KeyEvent.KEY_PRESSED) {
                if (e.keyCode == 0 && e.keyChar.code == 65535 && isCurrentModeNormal()) {
                    // Switching to English IME when switching korean mode in normal mode
                    toEnglishIME(e)
                }
                return@EventDispatcher false
            }

            if (e.keyCode == KeyEvent.VK_ESCAPE) {
                // Always switch to English IME when pressing ESC
                toEnglishIME(e)
            } else if (e.isControlDown && e.keyCode == KeyEvent.VK_C) {
                // Switch to English IME when pressing Ctrl+C in normal mode
                if (!isCurrentModeNormal()) {
                    toEnglishIME(e)
                }
            }
            return@EventDispatcher false
        }, null)
    }
}
