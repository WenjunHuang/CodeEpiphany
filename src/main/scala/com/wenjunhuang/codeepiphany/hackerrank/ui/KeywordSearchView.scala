package com.wenjunhuang.codeepiphany.hackerrank.ui

import java.awt.BorderLayout
import javax.swing.ScrollPaneConstants

import com.intellij.ide.plugins.newui.ListPluginComponent
import com.intellij.openapi.actionSystem.{ DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.{ SearchTextField, SimpleTextAttributes }
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankChallengeDetail

class KeywordSearchView(private val myProject: Project, private val myPresenter: KeywordSearchViewPresenter)
    extends SimpleToolWindowPanel(true, true)
    with UiDataProvider {
  private val mySearchTextField                            = SearchTextField(true)
  private val myChallengesTableModel: ChallengesTableModel = ChallengesTableModel()
  private val myChallengesTable: TableView[HackerRankChallengeDetail] =
    myChallengesTableModel.createTableView()

  mySearchTextField.getTextEditor.getEmptyText
    .appendText(
      PluginBundle.message("hackerrank.ui.query.searchHint"),
      new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ListPluginComponent.GRAY_COLOR)
    )
  mySearchTextField.addDocumentListener(myPresenter)

  add(mySearchTextField, BorderLayout.NORTH)
  add(
    JBScrollPane(
      myChallengesTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ),
    BorderLayout.CENTER
  )

  override def uiDataSnapshot(sink: DataSink): Unit = {
    myPresenter.uiDataSnapshot(sink)
  }

  def getTable: TableView[HackerRankChallengeDetail] = myChallengesTable
  def getTableModel: ChallengesTableModel            = myChallengesTableModel

}
