package com.wenjunhuang.codeepiphany.utils.template

import java.io.StringWriter
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity

import com.wenjunhuang.codeepiphany.model.{Constants, Language}

object VelocityUtils {
  def generateContent(
    template: String,
    language: Language,
    challenge: Any
  ): Either[Exception, String] = {
    val context = new VelocityContext()
    context.put("challenge", challenge)
    context.put("beginRegion", language.createComment(Constants.SUBMIT_CODE_REGION_BEGIN))
    context.put("endRegion", language.createComment(Constants.SUBMIT_CODE_REGION_END))
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
