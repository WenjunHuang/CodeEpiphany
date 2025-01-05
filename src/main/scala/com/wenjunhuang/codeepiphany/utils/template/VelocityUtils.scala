package com.wenjunhuang.codeepiphany.utils.template

import com.wenjunhuang.codeepiphany.model.{ChallengeCodeTemplate, Constants}
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity

import java.io.StringWriter

object VelocityUtils {
  def generateContent(template: String, challenge: ChallengeCodeTemplate): Either[Exception, String] = {
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
