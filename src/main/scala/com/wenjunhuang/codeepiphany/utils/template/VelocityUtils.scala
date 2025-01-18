package com.wenjunhuang.codeepiphany.utils.template

import java.io.StringWriter
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity

import com.wenjunhuang.codeepiphany.model.Constants

object VelocityUtils {
  def generateContent(template: String, challenge: Any): Either[Exception, String] = {
    val context = new VelocityContext()
    context.put("challenge", challenge)
    context.put("velocity", VelocityTool)
    val writer = StringWriter()
    try {
      Velocity.evaluate(context, writer, Constants.PROJECT_NAME, template)
      Right(writer.toString)
    } catch {
      case e: Exception => Left(e)
    } finally writer.close()
  }
}
