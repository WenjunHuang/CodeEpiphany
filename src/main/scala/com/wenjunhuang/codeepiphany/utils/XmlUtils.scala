package com.wenjunhuang.codeepiphany.utils

import com.intellij.util.xmlb.Converter
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language}
import org.typelevel.ci.CIString

object XmlUtils {

  class LanguageConverter extends Converter[Language] {
    override def fromString(value: String): Language =
      Language.fromCIString(CIString(value)).orNull
    override def toString(value: Language): String =
      value.value
  }

  class CodeDojoConverter extends Converter[CodeDojo] {
    override def fromString(s: String): CodeDojo = CodeDojo.fromCIString(CIString(s)).orNull

    override def toString(dojo:CodeDojo): String = dojo.value
  }

  class StringOptionConverter extends OptionConverter[String]

  class LanguageOptionConverter extends OptionConverter[Language]

  implicit val languageConverter: Converter[Language] = new LanguageConverter

  implicit val stringConverter: Converter[String] = new Converter[String] {
    override def fromString(value: String): String = value
    override def toString(value: String): String   = value
  }

  class OptionConverter[T: Converter] extends Converter[Option[T]] {
    override def fromString(value: String): Option[T] =
      if (value == null || value.isEmpty) None else Some(implicitly[Converter[T]].fromString(value))
    override def toString(value: Option[T]): String =
      value.map(it => implicitly[Converter[T]].toString(it)).orNull
  }

}
