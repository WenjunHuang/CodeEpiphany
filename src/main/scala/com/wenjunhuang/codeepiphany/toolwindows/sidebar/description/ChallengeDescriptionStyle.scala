package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import java.awt.Color

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefScrollbarsHelper

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.{AtCoder, CodeForces, HackerRank, LeetCode, LeetCodeCN}
import com.wenjunhuang.codeepiphany.utils.extensions.*

object ChallengeDescriptionStyle {
  def getStyle(styleProvider: ChallengeDescriptionStyleProvider, dojo: Option[CodeDojo]): String = {

    val padding = styleProvider.bodyPadding.map { case (top, right, bottom, left) =>
      s"${top}px ${right}px ${bottom}px ${left}px"
    }.getOrElse("0")

    // language=CSS
    s"""
       |$normalizeCss
       |
       |body {
       |    line-height: ${styleProvider.lineHeight};
       |    min-height: 100%;
       |    position: relative;
       |    background-color: ${styleProvider.backgroundColor.webRgba()};
       |    font-family :${styleProvider.fontName},-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji;
       |    font-size: ${styleProvider.fontSize}px;
       |    padding: $padding;
       |    color: ${styleProvider.foregroundColor.webRgba()};
       |}
       |
       |a {
       |  color: ${styleProvider.linkActiveForegroundColor.webRgba()}
       |}
       |
       |table td, table th {
       |    border: 1px solid ${styleProvider.separatorColor.webRgba()};
       |}
       |
       |hr {
       |    background-color: ${styleProvider.separatorColor.webRgba()};
       |}
       |
       |kbd, tr {
       |    border: 1px solid ${styleProvider.separatorColor.webRgba()};
       |}
       |
       |blockquote {
       |    border-left: 2px solid ${styleProvider.linkActiveForegroundColor.webRgba(0.4)}
       |}
       |
       |blockquote, code, pre {
       |    overflow: auto;
       |    background-color: ${styleProvider.panelBackground.webRgba()}
       |}
       |
       |${JBCefScrollbarsHelper.getOverlayScrollbarStyle}
       |
       |${dojo.map(styleOfDojo(_, styleProvider)).getOrElse("")}
       |
       |""".stripMargin
  }

  private def styleOfDojo(dojo: CodeDojo, styleProvider: ChallengeDescriptionStyleProvider): String =
    dojo match
      case LeetCode | LeetCodeCN =>
        getLeetcodeCNStyle(styleProvider)
      case HackerRank =>
        getHackerRankStyle(styleProvider)
      case CodeForces =>
        getCodeForcesStyle(styleProvider)
      case AtCoder =>
        getAtCoderStyle(styleProvider)


  private def getHackerRankStyle(styleProvider: ChallengeDescriptionStyleProvider): String =
    // language=CSS
    s"""
      |#container p {
      |    margin-top: 5px;
      |    margin-bottom: 0;
      |    line-height: 2;
      |}
      |#container img{
      |    margin-top: 15px;
      |    padding: 15px;
      |}
      |#container pre {
      |    overflow-x: auto;
      |    padding: 20px;
      |    border: none;
      |    border-radius: 5px;
      |    font-size: 14px;
      |    line-height: 20px;
      |}
      |#container strong {
      |    color: ${styleProvider.contrastedForeground.webRgba()};
      |    font-weight: bolder;
      |}
      |
      |""".stripMargin

  private def getLeetcodeCNStyle(styleProvider: ChallengeDescriptionStyleProvider): String =
    // language=CSS
    s"""
       |#container pre {
       |    background-color: unset;
       |    border-left: 2px solid ${styleProvider.contrastedForeground.webRgba()};
       |    margin-bottom: 1rem;
       |    margin-top: 1rem;
       |    padding-left: 1rem;
       |}
       |#container p {
       |  margin-bottom: 1rem;
       |}
       |
       |#container ul {
       |    list-style-type: disc;
       |    margin-bottom: 1rem;
       |    margin-left: 1rem;
       |    margin-right: 1rem;
       |}
       |
       |#container ul > li {
       |    margin-bottom: .75rem;
       |}
       |""".stripMargin

  private def getCodeForcesStyle(styleProvider: ChallengeDescriptionStyleProvider):String = {
    // language=CSS
    s"""
       |@media print {
       |    div.print-content article.node .node-blog .clearfix div.item-body p a { display: none; }
       |
       |    .compact-problemset div.ttypography {
       |        margin: 0 !important;
       |    }
       |
       |    .compact-problemset .problem-statement p {
       |        margin-bottom: 0.75em !important;
       |        page-break-inside: avoid;
       |    }
       |    .compact-problemset .problem-frames {
       |        column-count: 2;
       |    }
       |
       |    .compact-problemset .problem-statement .input,
       |    .compact-problemset .problem-statement .output {
       |        page-break-inside: avoid;
       |    }
       |
       |    .compact-problemset .problem-statement .output {
       |        page-break-inside: avoid;
       |    }
       |
       |    .compact-problemset .problem-statement {
       |        margin: 0.25em;
       |        line-height: 1.45em;
       |        font-size: 1.4rem;
       |    }
       |
       |    .compact-problemset #header {
       |        display: none;
       |    }
       |
       |    .compact-problemset .problem-statement .time-limit,
       |    .compact-problemset .problem-statement .memory-limit {
       |        display: inline;
       |    }
       |
       |    .compact-problemset .problem-statement .time-limit::after {
       |        content: ", ";
       |    }
       |
       |    .compact-problemset .problem-statement .property-title {
       |        display: none;
       |    }
       |
       |    .compact-problemset .problem-statement .input-file,
       |    .compact-problemset .problem-statement .output-file {
       |        display: none;
       |    }
       |
       |    .compact-problemset .problem-statement .sample-tests .section-title,
       |    .compact-problemset .problem-statement .note .section-title {
       |        display: none;
       |    }
       |
       |    .compact-problemset .input-output-copier {
       |        display: none;
       |    }
       |}
       |
       |.problem-statement {
       |    margin: 0.5em;
       |    /*font-family: verdana,arial,sans-serif;*/
       |    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
       |    line-height: 1.5em;
       |    font-size: 1.4rem;
       |}
       |
       |.problem-statement .epigraph {
       |/*    margin-left: 67%;*/
       |/*    width: 33%;*/
       |}
       |
       |.problem-statement .epigraph-text {
       |    margin-left: 67%;
       |    width: 33%;
       |}
       |
       |.problem-statement .epigraph-source {
       |    border-top: 1px solid #888;
       |    text-align: right;
       |}
       |
       |
       |.problem-statement .lstlisting {
       |    padding: 0.5em;
       |}
       |
       |.problem-statement .tex-tabular {
       |    margin: 1em 0;
       |    border-collapse: collapse;
       |    border-spacing: 0;
       |    border: initial !important;
       |}
       |
       |.problem-statement .tex-tabular * {
       |    border: initial !important;
       |}
       |
       |.problem-statement .tex-tabular tr:hover * {
       |    background: initial;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-border-left {
       |    border-left: 1px #ccc solid !important;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-border-right {
       |    border-right: 1px #ccc solid !important;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-border-top {
       |    border-top: 1px #ccc solid !important;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-border-bottom {
       |    border-bottom: 1px #ccc solid !important;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-text-align-left {
       |    text-align: left;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-text-align-center {
       |    text-align: center;
       |}
       |
       |.problem-statement .tex-tabular .tex-tabular-text-align-right {
       |    text-align: right;
       |}
       |
       |.problem-statement .tex-tabular td {
       |    padding: 0.4em;
       |    vertical-align: middle;
       |}
       |
       |.problem-statement p {
       |    margin: 0 0 1em 0 !important;
       |}
       |
       |.problem-statement .header {
       |    margin-bottom: 1em;
       |    text-align: center;
       |}
       |
       |.problem-statement .header .title {
       |    font-size: 150%;
       |    margin-bottom: 0.5em;
       |}
       |
       |.problem-statement .header .title {
       |    font-size: 150%;
       |}
       |
       |.problem-statement ul {
       |    list-style: disc outside;
       |    margin-top: 1em !important;
       |    margin-bottom: 1em !important;
       |}
       |
       |.problem-statement ol {
       |    list-style: decimal outside;
       |    margin-top: 1em !important;
       |    margin-bottom: 1em !important;
       |}
       |
       |.problem-statement li {
       |    line-height: 1.5em;
       |}
       |
       |.problem-statement .property-title {
       |    display: inline;
       |    padding-right: 4px;
       |}
       |
       |.problem-statement .property-title:after {
       |    content: ":";
       |}
       |
       |.problem-statement .time-limit, .problem-statement .memory-limit, .problem-statement .input-file, .problem-statement .output-file {
       |    margin: 0 auto;
       |}
       |
       |.problem-statement .legend {
       |    margin-bottom: 1em;
       |}
       |
       |.problem-statement .section-title {
       |    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
       |    /*font-family: arial,sans-serif;*/
       |    font-size: 115%;
       |    font-weight: bold;
       |}
       |
       |.problem-statement .input-specification,
       |    .problem-statement .output-specification,
       |    .problem-statement .sample-tests,
       |    .problem-statement .author,
       |    .problem-statement .resource,
       |    .problem-statement .date {
       |    /*margin-bottom: 1em;*/
       |}
       |
       |.problem-statement .output-specification {
       |    margin-bottom: 1em;
       |}
       |
       |.problem-statement .sample-tests .sample-test {
       |}
       |
       |.problem-statement .sample-tests .input, .problem-statement .sample-tests .output {
       |    border: 1px solid #888;
       |}
       |
       |.problem-statement .sample-tests .output {
       |    margin-bottom: 1em;
       |    position: relative;
       |    top: -1px;
       |}
       |
       |.problem-statement .sample-tests pre {
       |    line-height: 1.25em;
       |    padding: 0.25em;
       |    margin: 0;
       |}
       |
       |.problem-statement .sample-tests {
       |    font-family: Consolas, "Lucida Console", "Andale Mono", "Bitstream Vera Sans Mono", "Courier New", Courier, monospace;
       |    font-size: 0.9em;
       |}
       |
       |.problem-statement .sample-tests .title {
       |    /*font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;*/
       |    /*font-family: arial,sans-serif;*/
       |    font-size: 1.3em;
       |    padding: 0.25em;
       |    border-bottom: 1px solid #888;
       |    text-transform: lowercase;
       |    font-weight: bold;
       |}
       |
       |.problem-statement .test {
       |    margin-bottom: 3em;
       |}
       |
       |.problem-statement .test-title {
       |    /*font-size: 150%;*/
       |    font-weight: bold;
       |}
       |
       |.problem-statement .test-stem, .problem-statement .test-explanation-note {
       |    margin: 0.5em 0 0.5em 0;
       |}
       |
       |.problem-statement input[type="submit"] {
       |    margin-top: 0.5em;
       |    margin-right: 1em;
       |    padding: 0 1em;
       |}
       |
       |.problemindexholder {
       |    position: relative;
       |}
       |
       |div .problem-statement-overlay {
       |    position: absolute;
       |    top: 0;
       |    left: 0;
       |    height: 100%;
       |    width: 100%;
       |    background-color: #000;
       |    z-index: 50;
       |    opacity: 0.2;
       |}
       |
       |.load-answers-waiting-indicator {
       |    position: absolute;
       |    top: 49%;
       |    left: 49%;
       |}
       |
       |.problem-statement input[type="radio"] {
       |    margin-right: 0.5em;
       |}
       |
       |.problem-statement input[type="checkbox"] {
       |    margin-right: 0.5em;
       |}
       |
       |
       |.problem-statement input[type="text"] {
       |    width: 20em;
       |}
       |
       |.problem-statement textarea {
       |    width: 20em;
       |    height: 7em;
       |}
       |
       |.problem-statement .test-form {
       |    line-height: 1.75em;
       |}
       |
       |.problem-statement .test-form {
       |    line-height: 1.75em;
       |}
       |
       |.tex-formula {
       |    font-family: times new roman,sans-serif;
       |    vertical-align: middle;
       |    margin: 0;
       |    border:medium none;
       |    position: relative;
       |    bottom: 2px;
       |}
       |
       |.tex-span {
       |    font-size: 125%;
       |    font-family: times new roman,sans-serif;
       |    white-space: nowrap;
       |}
       |
       |.tex-font-size-tiny {
       |    font-size: 70%;
       |}
       |
       |.tex-font-size-script {
       |    font-size: 75%;
       |}
       |
       |.tex-font-size-footnotes {
       |    font-size: 85%;
       |}
       |
       |.tex-font-size-small {
       |    font-size: 85%;
       |}
       |
       |.tex-font-size-normal {
       |    font-size: 100%;
       |}
       |
       |.tex-font-size-large-1 {
       |    font-size: 115%;
       |}
       |
       |.tex-font-size-large-2 {
       |    font-size: 130%;
       |}
       |
       |.tex-font-size-large-3 {
       |    font-size: 145%;
       |}
       |
       |.tex-font-size-huge-1 {
       |    font-size: 175%;
       |}
       |
       |.tex-font-size-huge-2 {
       |    font-size: 200%;
       |}
       |
       |.tex-font-style-rm {
       |}
       |
       |.tex-font-style-sf {
       |    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
       |    /*font-family: arial,sans-serif;*/
       |}
       |
       |.tex-font-style-tt {
       |    font-size: 110%;
       |    font-family: courier new,monospace;
       |}
       |
       |.tex-font-style-md {
       |}
       |
       |.tex-font-style-bf {
       |    font-weight: bold;
       |}
       |
       |.tex-font-style-up {
       |}
       |
       |.tex-font-style-it {
       |    font-style: italic;
       |}
       |
       |.tex-font-style-sl {
       |    font-style: italic;
       |}
       |
       |.tex-font-style-sc {
       |    text-transform: uppercase;
       |}
       |
       |.tex-font-style-striked {
       |    text-decoration: line-through;
       |}
       |
       |.tex-font-style-underline {
       |    text-decoration: underline;
       |}
       |
       |.tex-graphics {
       |    display: block;
       |}
       |
       |.tex-font-style-part {
       |    font-size: 187.5%;
       |    font-weight: bold;
       |    font-family: Tahoma, Arial, Helvetica, sans-serif;
       |}
       |
       |.tex-font-style-chapter {
       |    font-size: 162.5%;
       |    font-weight: bold;
       |    font-family: Tahoma, Arial, Helvetica, sans-serif;
       |}
       |
       |.tex-font-style-section {
       |    font-size: 137.5%;
       |    font-weight: bold;
       |}
       |
       |.tex-font-style-subsection {
       |    font-size: 125%;
       |    font-weight: bold;
       |}
       |
       |.tex-font-style-subsubsection {
       |    font-size: 112.5%;
       |    font-weight: bold;
       |}
       |
       |.tex-font-style-paragraph {
       |    font-size: 100%;
       |    font-weight: bold;
       |}
       |
       |.tex-font-style-subparagraph {
       |    font-size: 100%;
       |    font-style: italic;
       |}
       |
       |.problem-statement .tex-tabular .tex-graphics {
       |    max-width: 100%;
       |}
       |
       |.problem-statement .tex-tabular td > p {
       |    margin-bottom: 0 !important;
       |}
       |
       |.problem-statement .test-example-line-even {
       |    background-color: ${styleProvider.backgroundColor.webRgba()};
       |}
       |
       |.statement-footnote {
       |    font-size: 85%;
       |    position: relative;
       |}
       |
       |.statement-footnote::before {
       |    content: "";
       |    position: absolute;
       |    top: -2px;
       |    width: 25%;
       |    border-top: 1px solid #888;
       |}
       |
       |.statement-footnote p {
       |    margin-bottom: 0.25em !important;
       |}
       |
       |.statement-footnote p:last-child {
       |    margin-bottom: 1em;
       |}
       |
       |.problem-statement .header .input-standard,
       |.problem-statement .header .output-standard {
       |    display: none;
       |}
       |
       |.test-form-item textarea {
       |    width: 100%;
       |    font-family: Consolas, "Lucida Console", "Andale Mono", "Bitstream Vera Sans Mono", "Courier New", Courier, monospace;
       |    height: 20rem;
       |    font-size: 1.3rem;
       |}
       |.MathJax_Display {text-align: center; margin: 1em 0em; position: relative; display: block!important; text-indent: 0; max-width: none; max-height: none; min-width: 0; min-height: 0; width: 100%}
       |.MathJax .merror {background-color: #FFFF88; color: #CC0000; border: 1px solid #CC0000; padding: 1px 3px; font-style: normal; font-size: 90%}
       |.MathJax .MJX-monospace {font-family: monospace}
       |.MathJax .MJX-sans-serif {font-family: sans-serif}
       |#MathJax_Tooltip {background-color: InfoBackground; color: InfoText; border: 1px solid black; box-shadow: 2px 2px 5px #AAAAAA; -webkit-box-shadow: 2px 2px 5px #AAAAAA; -moz-box-shadow: 2px 2px 5px #AAAAAA; -khtml-box-shadow: 2px 2px 5px #AAAAAA; filter: progid:DXImageTransform.Microsoft.dropshadow(OffX=2, OffY=2, Color='gray', Positive='true'); padding: 3px 4px; z-index: 401; position: absolute; left: 0; top: 0; width: auto; height: auto; display: none}
       |.MathJax {display: inline; font-style: normal; font-weight: normal; line-height: normal; font-size: 100%; font-size-adjust: none; text-indent: 0; text-align: left; text-transform: none; letter-spacing: normal; word-spacing: normal; word-wrap: normal; white-space: nowrap; float: none; direction: ltr; max-width: none; max-height: none; min-width: 0; min-height: 0; border: 0; padding: 0; margin: 0}
       |.MathJax:focus, body :focus .MathJax {display: inline-table}
       |.MathJax.MathJax_FullWidth {text-align: center; display: table-cell!important; width: 10000em!important}
       |.MathJax img, .MathJax nobr, .MathJax a {border: 0; padding: 0; margin: 0; max-width: none; max-height: none; min-width: 0; min-height: 0; vertical-align: 0; line-height: normal; text-decoration: none}
       |img.MathJax_strut {border: 0!important; padding: 0!important; margin: 0!important; vertical-align: 0!important}
       |.MathJax span {display: inline; position: static; border: 0; padding: 0; margin: 0; vertical-align: 0; line-height: normal; text-decoration: none; box-sizing: content-box}
       |.MathJax nobr {white-space: nowrap!important}
       |.MathJax img {display: inline!important; float: none!important}
       |.MathJax * {transition: none; -webkit-transition: none; -moz-transition: none; -ms-transition: none; -o-transition: none}
       |.MathJax_Processing {visibility: hidden; position: fixed; width: 0; height: 0; overflow: hidden}
       |.MathJax_Processed {display: none!important}
       |.MathJax_test {font-style: normal; font-weight: normal; font-size: 100%; font-size-adjust: none; text-indent: 0; text-transform: none; letter-spacing: normal; word-spacing: normal; overflow: hidden; height: 1px}
       |.MathJax_test.mjx-test-display {display: table!important}
       |.MathJax_test.mjx-test-inline {display: inline!important; margin-right: -1px}
       |.MathJax_test.mjx-test-default {display: block!important; clear: both}
       |.MathJax_ex_box {display: inline-block!important; position: absolute; overflow: hidden; min-height: 0; max-height: none; padding: 0; border: 0; margin: 0; width: 1px; height: 60ex}
       |.MathJax_em_box {display: inline-block!important; position: absolute; overflow: hidden; min-height: 0; max-height: none; padding: 0; border: 0; margin: 0; width: 1px; height: 60em}
       |.mjx-test-inline .MathJax_left_box {display: inline-block; width: 0; float: left}
       |.mjx-test-inline .MathJax_right_box {display: inline-block; width: 0; float: right}
       |.mjx-test-display .MathJax_right_box {display: table-cell!important; width: 10000em!important; min-width: 0; max-width: none; padding: 0; border: 0; margin: 0}
       |.MathJax .MathJax_HitBox {cursor: text; background: white; opacity: 0; filter: alpha(opacity=0)}
       |.MathJax .MathJax_HitBox * {filter: none; opacity: 1; background: transparent}
       |#MathJax_Tooltip * {filter: none; opacity: 1; background: transparent}
       |@font-face {font-family: MathJax_Main; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Main-Regular.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Main-Regular.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Main-bold; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Main-Bold.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Main-Bold.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Main-italic; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Main-Italic.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Main-Italic.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Math-italic; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Math-Italic.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Math-Italic.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Caligraphic; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Caligraphic-Regular.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Caligraphic-Regular.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Size1; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Size1-Regular.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Size1-Regular.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Size2; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Size2-Regular.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Size2-Regular.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Size3; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Size3-Regular.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Size3-Regular.otf?V=2.7.9') format('opentype')}
       |@font-face {font-family: MathJax_Size4; src: url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/woff/MathJax_Size4-Regular.woff?V=2.7.9') format('woff'), url('https://codeforces.com/mathjax.codeforces.org/fonts/HTML-CSS/TeX/otf/MathJax_Size4-Regular.otf?V=2.7.9') format('opentype')}
       |.MathJax .noError {vertical-align: ; font-size: 90%; text-align: left; color: black; padding: 1px 3px; border: 1px solid}
       |
       |""".stripMargin
  }

  private def getAtCoderStyle(styleProvider: ChallengeDescriptionStyleProvider):String = {
    //language=CSS
    s"""
       |span.lang-en {
       |display: inline;
       |}
       |span.lang-ja {
       |display: none;
       |}
       |.form-code-submit {
       | display: none;
       |}
       |a.btn.btn-default.btn-sm {
       |display:none;
       |}
       |.contest-title {
       |  color: white !important;
       |}
       |@media (max-width: 991px) {
       |  .contest-title {
       |    display: block;
       |    white-space: nowrap;
       |    width: 350px;
       |    overflow: hidden;
       |    text-overflow: ellipsis;
       |  }
       |}
       |
       |small.contest-duration {
       |  color: #666;
       |  margin-bottom: 5px;
       |}
       |small.contest-duration a { color: #666;}
       |small.back-to-home { margin-top: 1px;}
       |
       |#contest-nav-tabs { padding-top: 5px;}
       |.cnvtb-fixed {
       |  position: -webkit-sticky;
       |  position: sticky;
       |  top:50px;
       |  z-index: 999;
       |  background-color: #FFF;
       |}
       |#fix-cnvtb, #fix-cnvtb:hover { color: #000;}
       |#fix-cnvtb.disabled, #fix-cnvtb.disabled:hover { color: #BBB;}
       |
       |/* code editor */
       |.ace_gutter-cell { padding-left: 5px;}
       |#editor, #submission-code {
       |  font-family: Consolas, Monaco, 'Courier New', Courier, monospace !important;
       |  font-size: 12px;
       |  line-height: 17px;
       |  width: 100%;
       |}
       |#editor {
       |  min-height: 425px;
       |  border-radius: 3px;
       |}
       |#submission-code .ace_cursor { color: #aaa;}
       |#submission-code .ace_scrollbar-v { top: 18px;}
       |#plain-textarea { min-height: 425px;}
       |@media (min-width: 768px) {
       |  .editor-label { text-align: right;}
       |  .editor-buttons .btn {
       |    margin-top: 5px;
       |    width: 128px;
       |  }
       |}
       |@media (max-width: 767px) {
       |  .editor-buttons .btn { margin-bottom: 5px;}
       |}
       |.editor-buttons .btn-default:focus { background-color: white;}
       |.editor-buttons .btn-default.active:focus { background-color: #e6e6e6;}
       |
       |.source-code-for-copy {
       |  /*position: absolute;
       |  left: -100000px;
       |  top: -1000000px;
       |  width: 0;
       |  height: 0;
       |  overflow: hidden;*/
       |  display: none;
       |}
       |
       |.div-btn-copy {
       |  position: relative;
       |  display: block;
       |}
       |.div-btn-copy+pre { padding-right: 45px;}
       |.btn-copy { vertical-align: top;}
       |.btn-pre {
       |  position: absolute;
       |  top: 0;
       |  right: 0;
       |  display: block;
       |  padding: 0 6px;
       |  font-size: 12px;
       |  color: #767676;
       |  cursor: pointer;
       |  background-color: #fff;
       |  border: 1px solid #ccc;
       |  border-radius: 0 4px 0 4px;
       |  z-index: 30;
       |}
       |.btn-pre:hover {
       |  color: #fff;
       |  background-color: #563d7c;
       |  border-color: #563d7c;
       |}
       |
       |.customtest-textarea {
       |  font-family: Consolas, 'Courier New', Courier, Monaco, monospace;
       |}
       |
       |
       |html { overflow-y: scroll;}
       |
       |
       |div .div-profile-box { text-align: center;}
       |.insert-participant-box {
       |  padding: 34px 20px;
       |  margin-bottom: 5px;
       |  background-color: #f5f5f5;
       |  border-radius: 6px;
       |}
       |div.insert-participant-cls {
       |  margin: 30px auto 0 auto;
       |  width: 300px;
       |}
       |.insert-participant-box h1 {
       |  font-size: 300%;
       |  word-wrap: break-word;
       |  margin: 0;
       |}
       |.insert-participant {
       |  margin-top: 30px;
       |  text-align: center;
       |}
       |.insert-participant-notice { text-align: center;}
       |
       |
       |/* standings */
       |#standings-watching { margin-right: 25px;}
       |.standings-per-page {
       |  margin-left: 5px;
       |  cursor: pointer;
       |}
       |.standings-per-page.selected {
       |  color: black;
       |  font-weight: bold;
       |}
       |.standings-user-btn { visibility: hidden;}
       |td:hover .standings-user-btn { visibility: visible;}
       |#standings-select-country { width: 160px;}
       |#standings-select-affiliation { width: 300px;}
       |#refresh, #auto-refresh { margin-right: 5px;}
       |#last-refresh { color: #888;}
       |.standings-username {
       |  text-align: left !important;
       |  padding-left: 12px !important;
       |  white-space: nowrap;
       |}
       |.standings-frozen { background-color: rgba(0, 82, 255, 0.27) !important;}
       |.standings-rank {
       |  padding-top: 4px !important;
       |}
       |.standings-result { padding: 0 !important;}
       |.standings-result p {
       |  margin: 0 0 3px;
       |  color: #888;
       |  font-size: 90%;
       |}
       |.standings-ac {
       |  color: #00AA3E;
       |  font-weight: bold;
       |}
       |.standings-wa { color: #FF0000;}
       |.standings-score {
       |  color: #0000FF;
       |  font-weight: bold;
       |}
       |.standings-fa td { border-top: 2px solid #ddd !important;}
       |.standings-statistics td, .standings-fa td {
       |  padding: 5px !important;
       |  font-size: 80%;
       |}
       |.standings-statistics td p, .standings-fa td p {
       |  margin: 0;
       |  color: #888;
       |}
       |.fav-btn-standings { cursor: pointer; }
       |
       |.diff-inc { font-weight: bold; }
       |.diff-zero { color: #666; }
       |.diff-dec { color: #BBB; }
       |
       |.sort-th {
       |  padding-right: 10px !important;
       |  background-image: url(//img.atcoder.jp/assets/icon/sort-bg.gif);
       |  background-repeat: no-repeat;
       |  background-position: center right;
       |  cursor: pointer;
       |  -moz-user-select: none !important;
       |  -webkit-user-select: none !important;
       |  -ms-user-select: none !important;
       |}
       |.sort-asc { background-image: url(//img.atcoder.jp/assets/icon/sort-asc.gif) !important;}
       |.sort-desc { background-image: url(//img.atcoder.jp/assets/icon/sort-desc.gif) !important;}
       |.img-flag-btn { cursor: pointer;}
       |/* end standings */
       |
       |#contest-countdown-timer {
       |  text-align: center;
       |  color: #777;
       |  font-size: 24px;
       |}
       |
       |
       |#contest-statement { font-size:medium;}
       |#contest-statement section ol { margin-top: 2ex;}
       |#contest-statement section ol li { line-height: 20px;}
       |#contest-statement section ul { margin-top: 2ex;}
       |#contest-statement section ul li { line-height: 20px;}
       |#contest-statement h3 { margin-top: 2ex;}
       |
       |
       |.selector-inline-label-left { margin-right: 5px;}
       |.selector-inline-label {
       |  margin-right: 5px;
       |  margin-left:  5px;
       |}
       |
       |
       |/* task statement */
       |#task-statement { font-size:medium;}
       |#task-statement p { font-size: medium;}
       |#task-statement h3 {
       |  margin-top:2ex;
       |  margin-bottom: 5px;
       |}
       |#task-statement { line-height: 23px;}
       |#task-statement ul li { line-height: 23px;}
       |#task-statement ol.linenums { margin: 0px;}
       |#task-statement .prettyprint.linenums{
       |  box-shadow:none;
       |  box-shadow: 5px 0 0 #FBFBFC inset;
       |}
       |#task-statement .linenums li { padding-left: 0px;}
       |#task-statement .img-caption {
       |  text-align: center;
       |  width:100%;
       |  max-width:1000px !important;
       |  margin: 20px auto;
       |  text-align:center;
       |}
       |#task-statement .img-caption img { margin: 0px 0px 15px 0px;}
       |#task-statement .img-nocaption img { margin: 7px 0px 7px 0px;}
       |#task-statement .img-caption #caption {
       |  font-size: 13px;
       |  margin: -20px 0px 15px 0px;
       |}
       |#task-statement .part { margin: 0px 0 35px 0;}
       |#task-statement code {
       |  padding: 0;
       |  margin: 0;
       |  border-width: 0;
       |  font-size: 14px;
       |}
       |
       |p, li { line-height: 170%;}
       |.io-style pre {
       |  margin : 10px;
       |  padding-left: 13px;
       |  font-size: 16px;
       |  word-spacing: 0.8ex;
       |}
       |.io-style dot {
       |  content: ":";
       |  font-size:80%;
       |}
       |/*code:before {
       |  content: "'";
       |  margin-right: 0.15em;
       |  color: #666;
       |}
       |code:after {
       |  content: "'";
       |  margin-left: 0.15em;
       |  color: #666;
       |}*/
       |
       |#task-lang-btn span { cursor: pointer;}
       |/* end task statement */
       |
       |.ad_box {
       |  margin-top: 30px;
       |  text-align: center;
       |}
       |.ad_box a {
       |  font-size: 16px;
       |  color: black;
       |}
       |.ad_box p { margin-top: 15px;}
       |
       |.sponsor_banners a {
       |  margin: 5px 5px;
       |}
       |.sponsor_banners img { width: 100%;}
       |.sponsor_banner_0 img { max-width: 400px; max-height: 200px;}
       |.sponsor_banner_1 img { max-width: 300px; max-height: 150px;}
       |.sponsor_banner_2 img { max-width: 200px; max-height: 100px;}
       |.sponsor_banner_3 img { max-width: 150px; max-height: 75px;}
       |.sponsor_banner_4 img { max-width: 120px; max-height: 60px;}
       |.sponsor_banner_5 img { max-width: 100px; max-height: 50px;}
       |.sponsor_banner_6 img { max-width: 80px; max-height: 40px;}
       |
       |""".stripMargin
  }
  private val normalizeCss: String =
    // language=CSS
    """
      |/*! normalize.css v8.0.1 | MIT License | github.com/necolas/normalize.css */
      |
      |/* Document
      |   ========================================================================== */
      |
      |/**
      | * 1. Correct the line height in all browsers.
      | * 2. Prevent adjustments of font size after orientation changes in iOS.
      | */
      |
      |html {
      |  line-height: 1.15; /* 1 */
      |  -webkit-text-size-adjust: 100%; /* 2 */
      |}
      |
      |/* Sections
      |   ========================================================================== */
      |
      |/**
      | * Remove the margin in all browsers.
      | */
      |
      |body {
      |  margin: 0;
      |}
      |
      |/**
      | * Render the `main` element consistently in IE.
      | */
      |
      |main {
      |  display: block;
      |}
      |
      |/**
      | * Correct the font size and margin on `h1` elements within `section` and
      | * `article` contexts in Chrome, Firefox, and Safari.
      | */
      |
      |h1 {
      |  font-size: 2em;
      |  margin: 0.67em 0;
      |}
      |
      |/* Grouping content
      |   ========================================================================== */
      |
      |/**
      | * 1. Add the correct box sizing in Firefox.
      | * 2. Show the overflow in Edge and IE.
      | */
      |
      |hr {
      |  box-sizing: content-box; /* 1 */
      |  height: 0; /* 1 */
      |  overflow: visible; /* 2 */
      |}
      |
      |/**
      | * 1. Correct the inheritance and scaling of font size in all browsers.
      | * 2. Correct the odd `em` font sizing in all browsers.
      | */
      |
      |pre {
      |  font-family: monospace, monospace; /* 1 */
      |  font-size: 1em; /* 2 */
      |}
      |
      |/* Text-level semantics
      |   ========================================================================== */
      |
      |/**
      | * Remove the gray background on active links in IE 10.
      | */
      |
      |a {
      |  background-color: transparent;
      |}
      |
      |/**
      | * 1. Remove the bottom border in Chrome 57-
      | * 2. Add the correct text decoration in Chrome, Edge, IE, Opera, and Safari.
      | */
      |
      |abbr[title] {
      |  border-bottom: none; /* 1 */
      |  text-decoration: underline; /* 2 */
      |  text-decoration: underline dotted; /* 2 */
      |}
      |
      |/**
      | * Add the correct font weight in Chrome, Edge, and Safari.
      | */
      |
      |b,
      |strong {
      |  font-weight: bolder;
      |}
      |
      |/**
      | * 1. Correct the inheritance and scaling of font size in all browsers.
      | * 2. Correct the odd `em` font sizing in all browsers.
      | */
      |
      |code,
      |kbd,
      |samp {
      |  font-family: monospace, monospace; /* 1 */
      |  font-size: 1em; /* 2 */
      |}
      |
      |/**
      | * Add the correct font size in all browsers.
      | */
      |
      |small {
      |  font-size: 80%;
      |}
      |
      |/**
      | * Prevent `sub` and `sup` elements from affecting the line height in
      | * all browsers.
      | */
      |
      |sub,
      |sup {
      |  font-size: 75%;
      |  line-height: 0;
      |  position: relative;
      |  vertical-align: baseline;
      |}
      |
      |sub {
      |  bottom: -0.25em;
      |}
      |
      |sup {
      |  top: -0.5em;
      |}
      |
      |/* Embedded content
      |   ========================================================================== */
      |
      |/**
      | * Remove the border on images inside links in IE 10.
      | */
      |
      |img {
      |  border-style: none;
      |}
      |
      |/* Forms
      |   ========================================================================== */
      |
      |/**
      | * 1. Change the font styles in all browsers.
      | * 2. Remove the margin in Firefox and Safari.
      | */
      |
      |button,
      |input,
      |optgroup,
      |select,
      |textarea {
      |  font-family: inherit; /* 1 */
      |  font-size: 100%; /* 1 */
      |  line-height: 1.15; /* 1 */
      |  margin: 0; /* 2 */
      |}
      |
      |/**
      | * Show the overflow in IE.
      | * 1. Show the overflow in Edge.
      | */
      |
      |button,
      |input { /* 1 */
      |  overflow: visible;
      |}
      |
      |/**
      | * Remove the inheritance of text transform in Edge, Firefox, and IE.
      | * 1. Remove the inheritance of text transform in Firefox.
      | */
      |
      |button,
      |select { /* 1 */
      |  text-transform: none;
      |}
      |
      |/**
      | * Correct the inability to style clickable types in iOS and Safari.
      | */
      |
      |button,
      |[type="button"],
      |[type="reset"],
      |[type="submit"] {
      |  -webkit-appearance: button;
      |}
      |
      |/**
      | * Remove the inner border and padding in Firefox.
      | */
      |
      |button::-moz-focus-inner,
      |[type="button"]::-moz-focus-inner,
      |[type="reset"]::-moz-focus-inner,
      |[type="submit"]::-moz-focus-inner {
      |  border-style: none;
      |  padding: 0;
      |}
      |
      |/**
      | * Restore the focus styles unset by the previous rule.
      | */
      |
      |button:-moz-focusring,
      |[type="button"]:-moz-focusring,
      |[type="reset"]:-moz-focusring,
      |[type="submit"]:-moz-focusring {
      |  outline: 1px dotted ButtonText;
      |}
      |
      |/**
      | * Correct the padding in Firefox.
      | */
      |
      |fieldset {
      |  padding: 0.35em 0.75em 0.625em;
      |}
      |
      |/**
      | * 1. Correct the text wrapping in Edge and IE.
      | * 2. Correct the color inheritance from `fieldset` elements in IE.
      | * 3. Remove the padding so developers are not caught out when they zero out
      | *    `fieldset` elements in all browsers.
      | */
      |
      |legend {
      |  box-sizing: border-box; /* 1 */
      |  color: inherit; /* 2 */
      |  display: table; /* 1 */
      |  max-width: 100%; /* 1 */
      |  padding: 0; /* 3 */
      |  white-space: normal; /* 1 */
      |}
      |
      |/**
      | * Add the correct vertical alignment in Chrome, Firefox, and Opera.
      | */
      |
      |progress {
      |  vertical-align: baseline;
      |}
      |
      |/**
      | * Remove the default vertical scrollbar in IE 10+.
      | */
      |
      |textarea {
      |  overflow: auto;
      |}
      |
      |/**
      | * 1. Add the correct box sizing in IE 10.
      | * 2. Remove the padding in IE 10.
      | */
      |
      |[type="checkbox"],
      |[type="radio"] {
      |  box-sizing: border-box; /* 1 */
      |  padding: 0; /* 2 */
      |}
      |
      |/**
      | * Correct the cursor style of increment and decrement buttons in Chrome.
      | */
      |
      |[type="number"]::-webkit-inner-spin-button,
      |[type="number"]::-webkit-outer-spin-button {
      |  height: auto;
      |}
      |
      |/**
      | * 1. Correct the odd appearance in Chrome and Safari.
      | * 2. Correct the outline style in Safari.
      | */
      |
      |[type="search"] {
      |  -webkit-appearance: textfield; /* 1 */
      |  outline-offset: -2px; /* 2 */
      |}
      |
      |/**
      | * Remove the inner padding in Chrome and Safari on macOS.
      | */
      |
      |[type="search"]::-webkit-search-decoration {
      |  -webkit-appearance: none;
      |}
      |
      |/**
      | * 1. Correct the inability to style clickable types in iOS and Safari.
      | * 2. Change font properties to `inherit` in Safari.
      | */
      |
      |::-webkit-file-upload-button {
      |  -webkit-appearance: button; /* 1 */
      |  font: inherit; /* 2 */
      |}
      |
      |/* Interactive
      |   ========================================================================== */
      |
      |/*
      | * Add the correct display in Edge, IE 10+, and Firefox.
      | */
      |
      |details {
      |  display: block;
      |}
      |
      |/*
      | * Add the correct display in all browsers.
      | */
      |
      |summary {
      |  display: list-item;
      |}
      |
      |/* Misc
      |   ========================================================================== */
      |
      |/**
      | * Add the correct display in IE 10+.
      | */
      |
      |template {
      |  display: none;
      |}
      |
      |/**
      | * Add the correct display in IE 10.
      | */
      |
      |[hidden] {
      |  display: none;
      |}
      |""".stripMargin

  private val FONT_SIZE = "--default-font-size"
  private val SCALE     = "--scale"
}
