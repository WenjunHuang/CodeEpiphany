package com.wenjunhuang.codeepiphany.actions.webview

import com.intellij.openapi.actionSystem.DataKey

trait WebviewActionProvider {
  def zoomIn(): Unit

  def zoomOut(): Unit

  def canZoomIn: Boolean

  def canZoomOut: Boolean

  def actualZoom(): Unit

  def zoom: Double
}

object WebviewActionProvider {
  val DATA_KEY: DataKey[WebviewActionProvider] = DataKey.create[WebviewActionProvider]("WEBVIEW_ACTION_PROVIDER")
}
