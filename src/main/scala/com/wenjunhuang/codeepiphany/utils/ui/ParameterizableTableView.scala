package com.wenjunhuang.codeepiphany.utils.ui

import javax.swing.JProgressBar

import com.intellij.ui.components.{JBLayeredPane, JBPanel, JBScrollPane}
import com.intellij.ui.table.TableView
import com.intellij.util.ui.components.BorderLayoutPanel

class ParameterizableTableView[T] extends JBPanel[ParameterizableTableView[T]] {
  private val myTableView = new TableView[T]()
  private val myTableScrollPane = JBScrollPane(myTableView)

  private var myIsLoading = false
  private val myProgressBar = JProgressBar()
  
  private val myTopPanel = BorderLayoutPanel()
  private val myCenterPanel = BorderLayoutPanel()
  private val myLayeredPane = JBLayeredPane()
  
  private var myTableHadFocus = false
  
  private var myRefreshEnabled = true
  

}
