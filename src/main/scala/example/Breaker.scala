package example

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

case class FailedFastException() extends RuntimeException("Failed fast because circuit breaker is in open state")

class Breaker(allowedFailure: Int, perTime: FiniteDuration, protectionTime: FiniteDuration) {

  private var failureCount         = 0
  private var failureTimespanStart = 0L
  private var openTime             = 0L

  def protect[A](op: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    val now = System.nanoTime()

    if (!isOpen(now)) {
      val setFail = (t: Throwable) => {
        setFailed(now)
        t
      }
      op.transform(identity, setFail)
    } else Future.failed(FailedFastException())
  }

  private def isOpen(now: Long) =
    now < openTime + protectionTime.toNanos

  private def inTimespan(now: Long) =
    now < failureTimespanStart + perTime.toNanos

  private def setFailed(now: Long): Unit = synchronized {
    if (inTimespan(now)) {
      failureCount += 1
    } else {
      failureTimespanStart = now
      failureCount = 1
    }
    if (failureCount > allowedFailure) openTime = now
  }
}

object Breaker {
  def apply(allowedFailure: Int, perTime: FiniteDuration, protectionTime: FiniteDuration): Breaker =
    new Breaker(allowedFailure, perTime, protectionTime)
}
