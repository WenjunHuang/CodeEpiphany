package com.wenjunhuang.codeepiphany.utils

import org.typelevel.ci.CIString

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.xmlb.Converter

import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }

object XmlUtils {

  class LanguageConverter extends Converter[Language] {
    override def fromString(value: String): Language = Language.fromCIString(CIString(value)).orNull
    override def toString(value: Language): String   = value.value
  }

  class LanguageVersionConverter extends Converter[LanguageVersion] {
    override def fromString(s: String): LanguageVersion =
      if StringUtil.isEmpty(s) then LanguageVersion.AnyVersion
      else LanguageVersion.SpecificVersion(s)

    override def toString(t: LanguageVersion): String = t.version
  }

  class CodeDojoConverter extends Converter[CodeDojo] {
    override def fromString(s: String): CodeDojo = CodeDojo.fromCIString(CIString(s)).orNull

    override def toString(dojo: CodeDojo): String = dojo.value
  }

  class StringOptionConverter extends OptionConverter[String]

  class LanguageOptionConverter extends OptionConverter[Language]

  class LanguageVersionOptionConverter extends OptionConverter[LanguageVersion]

  class CodeDojoOptionConverter extends OptionConverter[CodeDojo]

  class IntOptionConverter extends OptionConverter[Int]

  class LongOptionConverter extends OptionConverter[Long]

  implicit val codeDojoConverter: Converter[CodeDojo] = new CodeDojoConverter

  implicit val languageConverter: Converter[Language] = new LanguageConverter

  implicit val languageVersionConverter: Converter[LanguageVersion] = new LanguageVersionConverter

  implicit val intConverter: Converter[Int] = new Converter[Int] {
    override def fromString(s: String): Int = s.toInt

    override def toString(t: Int): String = t.toString
  }
  implicit val longConverter: Converter[Long] = new Converter[Long] {
    override def fromString(s: String): Long = s.toLong

    override def toString(t: Long): String = t.toString
  }

  implicit val stringConverter: Converter[String] = new Converter[String] {
    override def fromString(value: String): String = value
    override def toString(value: String): String   = value
  }

  class OptionConverter[T: Converter] extends Converter[Option[T]] {
    override def fromString(value: String): Option[T] =
      if value == null then null
      else
        try Option(implicitly[Converter[T]].fromString(value))
        catch case e => None
    override def toString(value: Option[T]): String =
      value.map(it => implicitly[Converter[T]].toString(it)).orNull
  }

}
