package com.wenjunhuang.codeepiphany.utils;

import com.intellij.openapi.wm.ToolWindowFactory;

/**
 * This class is used to prevent scala 3 generate synthetic methods of kotlin ToolWindowFactory interface
 * which contains some @Internal methods that are not supposed to be implemented by the user.
 * Which will cause the jetbrains marketplace verifier to fail the plugin.
 */
public abstract class ToolWindowFactoryBridge implements ToolWindowFactory {
}
