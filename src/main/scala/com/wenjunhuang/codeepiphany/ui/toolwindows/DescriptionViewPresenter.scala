package com.wenjunhuang.codeepiphany.ui.toolwindows

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class DescriptionViewPresenter(project:Project) extends Disposable {
  project.getMessageBus.connect(this).subscribe

  override def dispose(): Unit = ???
}
