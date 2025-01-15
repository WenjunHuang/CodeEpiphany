package com.wenjunhuang.codeepiphany.utils

import com.softwaremill.id.DefaultIdGenerator

object IdGenerator {
  private val idGenerator = new DefaultIdGenerator()
  
  def nextId(): Long      = idGenerator.nextId()
}
