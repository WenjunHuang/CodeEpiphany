package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import com.intellij.ide.plugins.newui.ListPluginComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.{ SearchTextField, SimpleTextAttributes }
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.{ JBUI, StatusText }
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeDetail

import scala.jdk.CollectionConverters.*
import java.awt.BorderLayout
import javax.swing.{ ListSelectionModel, ScrollPaneConstants }

class KeywordSearchView(private val myProject: Project, private val myPresenter: KeywordSearchViewPresenter) extends SimpleToolWindowPanel(true, true) {
  private val mySearchTextField = SearchTextField(true)

  mySearchTextField.getTextEditor.getEmptyText
    .appendText(PluginBundle.message("hackerrank.ui.query.searchhint"), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ListPluginComponent.GRAY_COLOR))
  mySearchTextField.addDocumentListener(myPresenter)

  private val myChallengesTableModel = ChallengesTableModel()

  private val myChallengesTable = TableView(myChallengesTableModel)
  myChallengesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  myChallengesTable.setShowGrid(false)
  myChallengesTable.setShowColumns(true)

  add(mySearchTextField, BorderLayout.NORTH)
  add(
    JBScrollPane(
      myChallengesTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ),
    BorderLayout.CENTER
  )

  def updateChallenges(challenges: List[ChallengeDetail]): Unit =
    myChallengesTableModel.setItems(challenges.asJava)
}
