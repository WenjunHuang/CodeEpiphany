package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import com.intellij.ide.plugins.newui.ListPluginComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.ui.{SearchTextField, SimpleTextAttributes}
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeDetail

import java.awt.BorderLayout
import javax.swing.ScrollPaneConstants

class KeywordSearchView(private val myProject: Project, private val myPresenter: KeywordSearchViewPresenter)
    extends SimpleToolWindowPanel(true,true) {
  private val mySearchTextField                            = SearchTextField(true)
  private val myChallengesTableModel: ChallengesTableModel = ChallengesTableModel()
  private val myChallengesTable: TableView[ChallengeDetail] =
    myChallengesTableModel.createTableView(myPresenter.uiDataSnapshot)

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

  def getTable: TableView[ChallengeDetail] = myChallengesTable
  def getTableModel: ChallengesTableModel  = myChallengesTableModel

}
