package com.wenjunhuang.codeepiphany.model

import com.intellij.credentialStore.{CredentialAttributes, CredentialAttributesKt}
import com.intellij.ide.passwordSafe.PasswordSafe

object SensitiveDataStore {
  def saveData(key: String, value: String): Unit = {
    val attrs        = createCredentialAttributes(key)
    val passwordSafe = PasswordSafe.getInstance()
    passwordSafe.setPassword(attrs, value)
  }

  def loadData(key: String): Option[String] = {
    val attrs        = createCredentialAttributes(key)
    val passwordSafe = PasswordSafe.getInstance()
    Option(passwordSafe.getPassword(attrs))
  }
  
  def removeData(key: String): Unit = {
    val attrs        = createCredentialAttributes(key)
    val passwordSafe = PasswordSafe.getInstance()
    passwordSafe.setPassword(attrs,null)
  }

  private def createCredentialAttributes(key: String): CredentialAttributes =
    new CredentialAttributes(CredentialAttributesKt.generateServiceName(Constants.PROJECT_ID, key))

}
