package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.{Async, IO}
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.wenjunhuang.codeepiphany.controllers.dojo.HackerRankPanel.makeQuestionSheetQueryParamProvider
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.groups.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.LISTS_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.{ListQueryItem, ListsQueryParamProvider}

class HackerRankPanel(private val myProject: Project) extends SimpleToolWindowPanel(true, true) with AbstractCodeDojoViewPanel {
  private val actionManager = ActionManager.getInstance()
  private val actionGroup   = actionManager.getAction(TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val actionToolbar = actionManager.createActionToolbar(TOOLBAR_PLACE, actionGroup, true)

  setToolbar(actionToolbar.getComponent)
  actionToolbar.setTargetComponent(this)
  setContent(JBLabel("HackerRank"))

  override def dispose(): Unit = {}

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LISTS_PROVIDER_KEY, makeQuestionSheetQueryParamProvider[IO](myProject))
}

object HackerRankPanel {
  def makeQuestionSheetQueryParamProvider[F[_]:Async](project: Project): ListsQueryParamProvider[F] = new ListsQueryParamProvider[F] {
    override def getAllItems(): F[List[ListQueryItem]]                    = Async[F].pure(List(ListQueryItem("name", "id"), ListQueryItem("\uD83D\uDD25  LeetCode 热题 HOT 100", "id2")))
    override def getSelectedItems(): F[List[ListQueryItem]]               = Async[F].pure(List(ListQueryItem("name", "id")))
    override def addSelectedItems(items: List[ListQueryItem]): F[Unit]    = Async[F].unit
    override def removeSelectedItems(items: List[ListQueryItem]): F[Unit] = Async[F].unit
  }
}
