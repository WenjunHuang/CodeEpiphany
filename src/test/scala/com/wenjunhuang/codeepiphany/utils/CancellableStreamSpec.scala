package com.wenjunhuang.codeepiphany.utils

import cats.effect.kernel.Resource.ExitCase
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Ref}
import fs2.Stream
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*

class CancellableStreamSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  "CancellableStream" - {
    "should process items in order" in {
      for {
        results <- Ref[IO].of(List.empty[Int])
        processedRef <- Ref[IO].of(false)
        queueResource = CancellableStream.setup[Int, Unit](0.millis) { ctx =>
          for {
            _ <- results.update(list => list :+ ctx.value)
            _ <- IO.sleep(10.millis) // 给一点处理时间
            _ <- processedRef.set(true)
          } yield ()
        }
        _ <- queueResource.use { queue =>
          for {
            _ <- queue.offer(Some(1))
            _ <- processedRef.get.iterateUntil(identity) // 等待处理完成
            _ <- processedRef.set(false)
            _ <- queue.offer(Some(2))
            _ <- processedRef.get.iterateUntil(identity)
            _ <- processedRef.set(false)
            _ <- queue.offer(Some(3))
            _ <- processedRef.get.iterateUntil(identity)
          } yield ()
        }
        finalResults <- results.get
      } yield {
        finalResults should be(List(1, 2, 3))
      }
    }

    "should cancel previous operation when new item arrives" in {
      for {
        processingRef <- Ref[IO].of(Set.empty[Int])
        completedRef <- Ref[IO].of(Set.empty[Int])
        cancelledRef <- Ref[IO].of(Set.empty[Int])
        queueResource = CancellableStream.setup[Int, Unit](0.millis) { ctx =>
          Stream
            .eval(processingRef.update(_ + ctx.value))
            .evalMap(_ => IO.sleep(500.millis)) // 模拟长时间操作
            .onFinalizeCaseWeak {
              case ExitCase.Canceled =>
                IO.delay {
                  println(s"Operation for ${ctx.value} was cancelled , ${System.currentTimeMillis()}")
                } *>
                cancelledRef.update(_ + ctx.value)
              case _                 => IO.unit
            }
            .interruptWhen(ctx.signal)
            .evalMap{ _ =>
              IO.delay {
                println(s"Operation for ${ctx.value} completed, ${System.currentTimeMillis()}")
              } *>
              completedRef.update(_ + ctx.value)
            }
            .compile
            .drain
        }
        _ <- queueResource.use { queue =>
          for {
            _ <- queue.offer(Some(1))
            _ <- IO.sleep(100.millis) // 确保第一个操作开始
            _ <- queue.offer(Some(2))
            _ <- IO.sleep(600.millis) // 等待第二个操作完成
          } yield ()
        }
        processing <- processingRef.get
        completed <- completedRef.get
        canceled <- cancelledRef.get
      } yield {
        processing should be(Set(1, 2)) // 正确：两个操作都开始了
        canceled should be(Set(1))       // 正确：第一个操作被取消了
        completed should be(Set(2))     // 正确：只有第二个操作完成了，第一个被取消了
      }
    }

    "should debounce rapid requests" in {
      for {
        processedItems <- Ref[IO].of(List.empty[Int])
        queueResource = CancellableStream.setup[Int, Unit](200.millis) { ctx =>
          processedItems.update(list => list :+ ctx.value)
        }
        _ <- queueResource.use { queue =>
          for {
            _ <- queue.offer(Some(1))
            _ <- IO.sleep(50.millis)
            _ <- queue.offer(Some(2))
            _ <- IO.sleep(50.millis)
            _ <- queue.offer(Some(3))
            _ <- IO.sleep(300.millis) // 等待去抖动时间结束
          } yield ()
        }
        results <- processedItems.get
      } yield {
        results should be(List(3)) // 只有最后一个请求被处理
      }
    }

    "should handle None as termination signal" in {
      for {
        results <- Ref[IO].of(List.empty[Int])
        fiber <- CancellableStream
          .setup[Int, Unit](0.millis) { ctx =>
            results.update(list => list :+ ctx.value)
          }
          .use { queue =>
            for {
              _ <- queue.offer(Some(1))
              _ <- queue.offer(Some(2))
              _ <- queue.offer(None)
              _ <- queue.offer(Some(3)) // 这个不应该被处理
              _ <- IO.sleep(100.millis)
            } yield ()
          }
          .start
        _ <- fiber.join
        finalResults <- results.get
      } yield {
        finalResults should be(List(1, 2))
      }
    }
  }
} 