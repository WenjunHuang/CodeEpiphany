package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.{AtCoder, CodeForces, LuoGu}

object CodoDojoHeaders {
  def getHeader(codeDojo: CodeDojo): String =
    codeDojo match
      case CodeForces => getCodeForcesHeader
      case AtCoder    => getAtCoderHeader
      case LuoGu      => getLuoGuHeader
      case _          => ""

  private def getCodeForcesHeader: String =
    // language=HTML
    """
      |<script type="text/javascript">
      |    window.MathJax = {
      |      tex2jax: {
      |        inlineMath: [['$$$','$$$']], 
      |        displayMath: [['$$$$$$','$$$$$$']]
      |      }
      |    };
      |</script>
      |<script type="text/javascript" src="https://codeforces.com/mathjax.codeforces.org/MathJax.js?config=TeX-AMS_HTML-full"></script>
      |""".stripMargin

  private def getAtCoderHeader: String =
    // language=HTML
    """
      |<script src="https://img.atcoder.jp/public/c84259b/js/lib/jquery-1.9.1.min.js"></script>
      |<link rel="stylesheet" href="https://img.atcoder.jp/public/c84259b/css/cdn/katex.min.css">
      |<script defer src="https://img.atcoder.jp/public/c84259b/js/cdn/katex.min.js"></script>
      |<script defer src="https://img.atcoder.jp/public/c84259b/js/cdn/auto-render.min.js"></script>
      |<script>$(function(){$('var').each(function(){var html=$(this).html().replace(/<sub>/g,'_{').replace(/<\/sub>/g,'}');$(this).html('\\('+html+'\\)');});});</script>
      |<script>
      |			var katexOptions = {
      |				delimiters: [
      |					{left: "$$", right: "$$", display: true},
      |					{left: "\\(", right: "\\)", display: false},
      |					{left: "\\[", right: "\\]", display: true}
      |				],
      |      	ignoredTags: ["script", "noscript", "style", "textarea", "code", "option"],
      |				ignoredClasses: ["prettyprint", "source-code-for-copy"],
      |				throwOnError: false
      |			};
      |			document.addEventListener("DOMContentLoaded", function() { renderMathInElement(document.body, katexOptions);});
      |</script>
      |<script>
      |var LANG = "en";
      |// local storage
      |function setLS(key, val) {
      |  try {
      |    localStorage.setItem(key, JSON.stringify(val));
      |  } catch(error) {
      |    console.log(error);
      |  }
      |}
      |function getLS(key) {
      |  var val = localStorage.getItem(key);
      |  return val?JSON.parse(val):val;
      |}
      |function delLS(key) {
      |  localStorage.removeItem(key);
      |}
      |</script>
      |""".stripMargin

  private def getLuoGuHeader: String =
    // language=html
    """
    |<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.21/dist/katex.min.css">
    |<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.21/dist/katex.min.js"></script>
    |<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.21/dist/contrib/auto-render.min.js"></script>
    |<script>
    |    document.addEventListener("DOMContentLoaded", function() {
    |        renderMathInElement(document.body, {
    |          // customised options
    |          // • auto-render specific keys, e.g.:
    |          delimiters: [
    |              {left: '$$', right: '$$', display: true},
    |              {left: '$', right: '$', display: false},
    |              {left: '\\(', right: '\\)', display: false},
    |              {left: '\\[', right: '\\]', display: true}
    |          ],
    |          // • rendering keys, e.g.:
    |          throwOnError : false
    |        });
    |    });
    |</script>
    |""".stripMargin
}
