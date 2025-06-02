package com.wenjunhuang.codeepiphany.utils

import cats.effect.{ IO, Resource }
import cats.effect.std.Queue
import fs2.Stream
import fs2.concurrent.SignallingRef

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

object CancellableStream {
  case class StreamContext[T](value: T, signal: SignallingRef[IO, Boolean])

  def setup[T, R](
    debounceTime: FiniteDuration
  )(process: StreamContext[T] => IO[R]): Resource[IO, Queue[IO, Option[T]]] = {
    for {
      queue      <- Resource.eval(Queue.unbounded[IO, Option[T]])
      initSignal <- Resource.eval(SignallingRef.of[IO, Boolean](false))
      _ <- Resource.make(
        Stream
          .fromQueueNoneTerminated(queue)
          .evalMapAccumulate(initSignal) { (lastSignal, value) =>
            for {
              _         <- lastSignal.set(true)
              newSignal <- SignallingRef.of[IO, Boolean](false)
            } yield (newSignal, StreamContext(value, newSignal))
          }
          .debounce(debounceTime)
          .evalMap { case (signal, ctx) => process(ctx) }
          .compile
          .drain
          .start
      )(fiber => fiber.cancel)
    } yield queue
  }
}
