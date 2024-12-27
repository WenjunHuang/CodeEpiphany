package com.wenjunhuang.codeepiphany.utils.template

import com.wenjunhuang.codeepiphany.model.Constants
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity

import java.io.StringWriter

object VelocityUtils {
  def generateContent(template: String, attributes: Map[String, Any]): Either[Exception, String] = {
    val context = new VelocityContext()
    attributes.foreach { case (k, v) => context.put(k, v) }
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
