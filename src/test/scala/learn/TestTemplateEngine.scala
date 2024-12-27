package learn

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.wenjunhuang.codeepiphany.utils.template.VelocityTool
import org.apache.velocity.*
import org.apache.velocity.app.Velocity
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

import java.io.{FileInputStream, StringWriter}
import scala.io.Source

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
