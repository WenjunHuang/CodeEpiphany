package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces

object CodoDojoHeaders {
  def getHeader(codeDojo: CodeDojo): String =
    codeDojo match
      case CodeForces => getCodeForcesHeader()
      case _          => ""

  private def getCodeForcesHeader(): String =
    """
      |   <!-- MathJax -->
      |    <script type="text/x-mathjax-config">
      |    MathJax.Hub.Config({
      |      tex2jax: {inlineMath: [['$$$','$$$']], displayMath: [['$$$$$$','$$$$$$']]}
      |    });
      |    MathJax.Hub.Register.StartupHook("End", function () {
      |        Codeforces.runMathJaxListeners();
      |    });
      |    </script>
      |    <script type="text/javascript" async
      |            src="https://codeforces.com/mathjax.codeforces.org/MathJax.js?config=TeX-AMS_HTML-full"
      |    >
      |    </script>
      |    <!-- /MathJax -->
      |""".stripMargin
}
