package com.wenjunhuang.codeepiphany.utils.walkaround;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * 这个中间java类是为了绕过scala 3.6以后不能正确继承带protected构造函数的内部类的问题
 */
public abstract class DialogWrapperBridge extends DialogWrapper {
    protected AbstractAction myOkAction = new OkAction() {
        @Override
        protected void doAction(ActionEvent e) {
            onOkAction(e);
        }
    };

    protected DialogWrapperBridge(@Nullable Project project, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
    }

    protected abstract void onOkAction(ActionEvent e);

    @Override
    protected @NotNull Action getOKAction() {
        return myOkAction;
    }
}
