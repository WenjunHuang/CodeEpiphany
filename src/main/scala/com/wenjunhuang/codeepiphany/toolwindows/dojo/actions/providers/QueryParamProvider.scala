package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers

trait QueryParamProvider[T] {

  def getAllItems: List[T]

  def isMultipleSelection: Boolean

  def isSelected(item: T): Boolean

  def getSelectedItems: List[T]

  def addSelectedItems(items: List[T]): Unit

  def toggleSelection(item: T): Unit

  def removeSelectedItems(items: List[T]): Unit
}
