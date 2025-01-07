package com.wenjunhuang.codeepiphany.utils.jcef

import com.intellij.openapi.project.Project
import org.cef.browser.{CefBrowser, CefFrame}
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.BoolRef
import org.cef.network.CefRequest

import java.net.URL

type CefResourceProvider = () => Option[CefResourceHandler]

class CefLocalRequestHandler(private val myProtocol: String, private val myAuthority: String,private val myProject:Project)
    extends CefRequestHandlerAdapter {
  private var myResources: Map[String, CefResourceProvider] = Map.empty

  private val REJECTING_RESOURCE_HANDLER = new CefResourceHandlerAdapter {
    override def processRequest(request: CefRequest, callback: CefCallback): Boolean = {
      callback.cancel()
      false
    }
  }

  private val OUTSIDE_REQUEST_HANDLER = CefRemoteRequestHandler.createResourceRequestHandler(myProject)


  private val RESOURCE_REQUEST_HANDLER = new CefResourceRequestHandlerAdapter {
    override def getResourceHandler(browser: CefBrowser, frame: CefFrame, request: CefRequest): CefResourceHandler = {
      val url = URL(request.getURL)
      if url.getProtocol != myProtocol || url.getAuthority != myAuthority then
        OUTSIDE_REQUEST_HANDLER.getResourceHandler(browser, frame, request)
      else
        myResources.get(url.getPath) match {
          case Some(provider) => provider().getOrElse(REJECTING_RESOURCE_HANDLER)
          case None           => REJECTING_RESOURCE_HANDLER
        }
    }
  }

  override def getResourceRequestHandler(
    browser: CefBrowser,
    frame: CefFrame,
    request: CefRequest,
    isNavigation: Boolean,
    isDownload: Boolean,
    requestInitiator: String,
    disableDefaultHandling: BoolRef
  ): CefResourceRequestHandler =
    RESOURCE_REQUEST_HANDLER

  def addResource(resourcePath: String)(resourceProvider: CefResourceProvider): Unit =
    myResources = myResources + (resourcePath -> resourceProvider)
}
