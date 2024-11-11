package com.devfive.vim_switch_ko

import com.intellij.ide.IdeEventQueue
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Font
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.awt.im.InputContext
import java.lang.reflect.Method
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
            for (ctx in context)
                ctx!!.setCharacterSubsets(null)
            InputContext.getInstance().setCharacterSubsets(null)
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner.inputContext.setCharacterSubsets(null)
        }

        fun isFocusedEditor(): Boolean {
            return FocusManager.getCurrentManager().focusOwner is EditorComponentImpl
        }

        fun isProjectViewPanel(): Boolean {
            return FocusManager.getCurrentManager().focusOwner is ProjectViewTree
        }

        var getEditor: Method? = null
        var getVirtualFile: Method? = null
        fun loadEditors() {
            val vimPluginId = PluginId.getId("IdeaVIM")
            val pluginDescriptor = PluginManager.getLoadedPlugins().find { it.pluginId == vimPluginId }
            if (pluginDescriptor == null)
                return
            val pluginClassLoader = pluginDescriptor.pluginClassLoader
            if (pluginClassLoader == null)
                return

            ApplicationManager.getApplication().invokeLater {
                val injectClass2 = pluginClassLoader.loadClass("com.maddyhome.idea.vim.helper.EditorHelper")
                injectClass2.methods.forEach { println(it) }
                getEditor = injectClass2.getMethod("getEditor", VirtualFile::class.java)
                getVirtualFile = injectClass2.getMethod("getVirtualFile", Editor::class.java)
            }
        }


        fun isCurrentModeNormal(): Boolean {
            if (getEditor == null) {
                loadEditors()
                if (getEditor == null)
                    return false
            }
            if (FocusManager.getCurrentManager().focusOwner !is EditorComponentImpl)
                return false

            val virtualFile = getVirtualFile!!.invoke(null, (FocusManager.getCurrentManager().focusOwner as EditorComponentImpl).editor)
            println("vitual file $virtualFile ${(FocusManager.getCurrentManager().focusOwner as EditorComponentImpl).editor}")
            val editor = getEditor!!.invoke(null, virtualFile)
            val mode = editor.javaClass.getMethod("getMode").invoke(editor)
            return mode != null && mode.toString().startsWith("NORMAL")
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner") {
            if (isFocusedEditor() && enableIdeaVIM() && isCurrentModeNormal())
                toEnglishIME((it.newValue as EditorComponentImpl).inputContext)
            if (it.newValue != null && it.newValue.javaClass.name == "com.maddyhome.idea.vim.ui.ex.ExTextField") {
////                // #TODO: Fix font in vim search mode
//                val editor = it.newValue.javaClass.getMethod("getEditor").invoke(it.newValue)
//
//                val fontSize = when {
//                    editor is EditorImpl -> editor.fontSize2D
//                    UISettings.getInstance().presentationMode -> UISettingsUtils.getInstance().presentationModeFontSize
////                    editor?.editorKind == EditorKind.CONSOLE -> UISettingsUtils.getInstance().scaledConsoleFontSize
//                    else -> UISettingsUtils.getInstance().scaledEditorFontSize
//                }
//                val scheme = EditorColorsManager.getInstance().globalScheme
//                scheme.fontPreferences.realFontFamilies.forEach { fontName ->
//                    val font = Font(fontName, Font.PLAIN, scheme.editorFontSize)
//                    if (font.canDisplayUpTo("") == -1) {
////                        return font.deriveFont(fontSize)
//                    }
//                }
//                println("${EditorColorsManager.getInstance().globalScheme.editorFontName} ${UISettingsUtils.getInstance().scaledConsoleFontSize} $fontSize")
//                editor.javaClass.getMethod("setFont").invoke(editor, Font("JetBrains Mono", Font.PLAIN,scheme.editorFontSize ).deriveFont(13.0F))
            }
        }
        IdeEventQueue.getInstance().addDispatcher(IdeEventQueue.EventDispatcher { e ->
            if (e !is KeyEvent) return@EventDispatcher false

            val projectView = isProjectViewPanel()
            val editorView = isFocusedEditor()

            if (!editorView && !projectView)
                return@EventDispatcher false
            if (!enableIdeaVIM())
                return@EventDispatcher false
            if (projectView) {
                if (e.keyCode == KeyEvent.VK_ESCAPE)
                    toEnglishIME(e.component.inputContext)
                return@EventDispatcher false
            }
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
