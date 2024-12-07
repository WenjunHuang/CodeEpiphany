package com.wenjunhuang.codeepiphany.utils

import fs2.{Chunk, Stream}
import org.cef.network.CefPostDataElement
import org.http4s.{Entity, EntityEncoder, Headers}

trait CefOps {

  private val makeChunk: CefPostDataElement => Chunk[Byte] = boundary =>
    boundary.getBytesCount match
      case 0 => Chunk.empty
      case bc =>
        val bytes = Array.ofDim[Byte](bc)
        boundary.getBytes(bc, bytes)
        Chunk.array(bytes)

  implicit def elementsEntity[F[_]]: EntityEncoder[F, Vector[CefPostDataElement]] = new EntityEncoder[F, Vector[CefPostDataElement]] {
    override def headers: Headers = Headers.empty

    override def toEntity(a: Vector[CefPostDataElement]): Entity[F] =
      if a.isEmpty then Entity.empty
      else
        val stream = a.tail.foldLeft(Stream.chunk[F,Byte](makeChunk(a.head)))((acc, elem) => acc ++ Stream.chunk(makeChunk(elem)))
        Entity(stream)
  }
}
