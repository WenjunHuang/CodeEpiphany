package learn

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase.assertEquals
import org.apache.velocity.*
import org.apache.velocity.app.Velocity

import java.io.FileInputStream
import scala.io.Source
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import java.io.StringWriter

class TestTemplateEngine extends BasePlatformTestCase {
  override def getTestDataPath = s"testResources/velocity/"
  def testHelloworld(): Unit = {
    val content = Source.fromInputStream(new FileInputStream(getTestDataPath + s"/${getTestName(false)}.txt")).getLines().mkString("\n")
    val context = new VelocityContext()
    context.put("name", "world")
    val writer = StringWriter()
    Velocity.evaluate(context, writer, "test", content)
    assertThat(writer.toString, equalTo("Hello, world!"))
  }

  def testCamelCaseName(): Unit = {
    val content = Source.fromInputStream(new FileInputStream(getTestDataPath + s"/${getTestName(false)}.txt")).getLines().mkString("\n")
    val context = new VelocityContext()
    context.put("name", "hello there my_good-friend")
    context.put("velocity",VelocityTool)
    val writer = StringWriter()
    Velocity.evaluate(context,writer,"test",content)
    assertThat(writer.toString,equalTo("HelloThereMyGoodFriend"))
  }
}

object VelocityTool {
  def camelCaseName(str: String): String =
    // split str with any space, hyphen , capitalize each word, and join them with no space
    str.split("[\\s-_]").map(_.capitalize).mkString("")

  def smallCamelCaseName(str: String): String =
    // split str with any space, hyphen , capitalize each word, and join them with no space
    str.split("[\\s-_]").map(_.capitalize).mkString("") match
      case "" => ""
      case s  => s.head.toLower + s.tail

  def snakeCaseName(str: String): String =
    // change from camel case to snake case
    str.split("(?=[A-Z])").map(_.toLowerCase).mkString("_")

  //  $ ! velocityTool.camelCaseName(str) 转换字符为大驼峰样式
//  （开头字母大写
//  ）
//  $ ! velocityTool.smallCamelCaseName(str) 转换字符为小驼峰样式
//  （开头字母小写
//  ）
//  $ ! velocityTool.snakeCaseName(str) 转换字符为蛇形样式
//    $ ! velocityTool.leftPadZeros(str, n) 在字符串的左边填充0
//  ，使字符串的长度至少为n
//  $ ! velocityTool.date()

}
