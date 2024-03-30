package com.devfive.vim_switch_ko

import com.intellij.ide.DataManager
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.awt.event.KeyEvent
import java.awt.im.InputContext

class ProjectOpenStartUpActivity : StartupActivity.DumbAware {

    override fun runActivity(project: Project) {

        val pluginDescriptor = PluginManager.getLoadedPlugins().find { it.pluginId == PluginId.getId("IdeaVIM") }

        thisLogger().warn("pluginDescriptor $pluginDescriptor")
        thisLogger().warn("pluginDescriptor ${pluginDescriptor?.isEnabled}")
        thisLogger().warn("pluginDescriptor ${PluginManager.getLoadedPlugins().map {
            it.pluginId
        }}")
        if (pluginDescriptor == null || !pluginDescriptor.isEnabled){
            thisLogger().warn("IdeaVIM plugin is not enabled")
            return
        }
        fun isCursorInEditor(event: AnActionEvent): Boolean {
            return event.dataContext.getData("editor") != null
        }


        fun toEnglishIME(event: KeyEvent) {
            val context = (event.source.javaClass.getMethod("getInputContext").invoke(event.source) as InputContext)
            context.setCharacterSubsets(null)
        }


        val pluginClassLoader = pluginDescriptor.classLoader
        val injectClass = pluginClassLoader.loadClass("com.maddyhome.idea.vim.api.VimInjectorKt")
        val instance = injectClass?.getMethod("getInjector")!!.invoke(null)
        val editorGroup = instance.javaClass.getMethod("getEditorGroup").invoke(instance)
        thisLogger().warn("editorGroup $editorGroup ${editorGroup.javaClass} ${editorGroup.javaClass.methods.toList()}")
        val editors = editorGroup.javaClass.getMethod("getEditors").invoke(editorGroup) as ArrayList<Any>

        fun isCurrentModeNormal(): Boolean {
            val currentFile = FileEditorManager.getInstance(project).selectedEditor?.file?.path ?: return false
            for (editor in editors) {
                val virtualFile = editor.javaClass.getMethod("getVirtualFile").invoke(editor).javaClass.getMethod("getPath").invoke(editor) as String
                if (virtualFile == currentFile) {
                    val mode = editor.javaClass.getMethod("getMode").invoke(editor)
                    return mode.toString().startsWith("NORMAL")
                }
            }
            return false
        }
        IdeEventQueue.getInstance().addDispatcher(IdeEventQueue.EventDispatcher { e ->
            if (e !is KeyEvent)
                return@EventDispatcher false
            if(e.id != KeyEvent.KEY_PRESSED){
                if(e.keyCode==0&&e.keyChar.code==65535 &&isCurrentModeNormal()){
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
                DataManager.getInstance().dataContextFromFocusAsync.then {
                    val event = AnActionEvent.createFromDataContext("context", null, it)
                    if (isCursorInEditor(event) && !isCurrentModeNormal()) {
                        toEnglishIME(e)
                    }
                }
            }
            return@EventDispatcher false
        }, null)
    }
}
