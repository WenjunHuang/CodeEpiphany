package com.wenjunhuang.codeepiphany.model

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*

class MatchRegionTest extends BasePlatformTestCase {
  def testLanguageRegionMatch(): Unit = {
    val java = Language.Java
    assertThat(
      java.matchRegion(s"// ${Constants.SUBMIT_CODE_REGION_BEGIN}", Constants.SUBMIT_CODE_REGION_BEGIN),
      is(true)
    )
  }
}
