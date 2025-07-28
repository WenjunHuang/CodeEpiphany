package com.wenjunhuang.codeepiphany.notifications

import icons.CodeEpiphanyIcons

import com.intellij.ide.BrowserUtil
import com.intellij.notification.{Notification, NotificationAction, NotificationGroupManager, NotificationType}
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.utils.IdeUtils

object CodeEpiphanyNotification {
  val NOTIFICATION_GROUP_ID = "Code Epiphany"
  val GITHUB_URL            = "https://github.com/WenjunHuang/CodeEpiphany"
  val DONATION_URL          = "https://leetcodeepiphany.pages.dev/"

  def notifyFirstlyDownloaded(project: Project): Unit = {
    val title       = PluginBundle.message("notification.firstlyDownloaded.title")
    val description = PluginBundle.message("notification.firstlyDownloaded.description")

    val notification = NotificationGroupManager
      .getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP_ID)
      .createNotification(title, description, NotificationType.INFORMATION)
    addNotificationActions(notification)
    notification.setIcon(CodeEpiphanyIcons.PLUGIN)
    notification.notify(project)
  }

  def notifyReleaseNote(project:Project):Unit = {
    val title       = PluginBundle.message("notification.releaseNote.title", IdeUtils.pluginVersion)
    val description = PluginBundle.message("whatsNew")

    val notification = NotificationGroupManager
      .getInstance()
      .getNotificationGroup(NOTIFICATION_GROUP_ID)
      .createNotification(title, description, NotificationType.INFORMATION)
    addNotificationActions(notification)
    notification.setIcon(CodeEpiphanyIcons.PLUGIN)
    notification.notify(project)
  }

  private def addNotificationActions(notification: Notification): Unit = {
    val github = NotificationAction.createSimple(
      "GitHub",
      () => {
        BrowserUtil.browse(GITHUB_URL)
      }
    )
    val donation = NotificationAction.createSimple(
      PluginBundle.message("donate"),
      () => {
        BrowserUtil.browse(DONATION_URL)
      }
    )

    notification.addAction(github)
    notification.addAction(donation)

  }

}
