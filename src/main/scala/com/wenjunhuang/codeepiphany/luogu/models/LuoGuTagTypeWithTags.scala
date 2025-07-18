package com.wenjunhuang.codeepiphany.luogu.models

import io.circe.Json
import io.circe.derivation.ConfiguredCodec
import io.circe.parser.*

import scala.io.Source
import scala.util.Using

case class LuoGuTag(id: Int, name: String, `type`: String, parent: Option[Int] = None) derives ConfiguredCodec
case class LuoGuTagGroup(id: Int, name: String, `type`: String, tags: List[LuoGuTag]) derives ConfiguredCodec
case class LuoGuTagTypeWithTags(id: String, value: String, tagGroups: List[LuoGuTagGroup] = Nil) derives ConfiguredCodec

object LuoGuTagTypeWithTags {
  lazy val ALL_TAG_TYPES: List[LuoGuTagTypeWithTags] = load()

  private def load(): List[LuoGuTagTypeWithTags] = {
    Using.resources(
      Source.fromResource("luogu/tags/TagsType.json", classOf[LuoGuTagTypeWithTags].getClassLoader),
      Source.fromResource("luogu/tags/TagsByType.json", classOf[LuoGuTagTypeWithTags].getClassLoader)
    ) { (tagTypeSource, tagSource) =>
      val tagTypes = decode[List[LuoGuTagTypeWithTags]](tagTypeSource.mkString).toTry.get
      val tagsJson = decode[Map[String, Json]](tagSource.mkString).getOrElse(Map.empty)

      tagTypes.map { tt =>
        tagsJson
          .get(tt.id)
          .map { tagsJson =>
            val rawTags = tagsJson.as[List[LuoGuTag]].getOrElse(Nil)
            rawTags.partition(_.parent.isEmpty) match {
              case (parents, children) =>
                val tagGroups = parents.map { parent =>
                  val childrenTags = children.filter(_.parent.contains(parent.id))
                  LuoGuTagGroup(parent.id, parent.name, parent.`type`, childrenTags)
                }
                tt.copy(tagGroups = tagGroups)
            }
          }
          .getOrElse(tt)
      }
    }
  }

}
