package example

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

case class FailedFastException()
    extends RuntimeException(
      "Failed fast because circuit breaker is in open state")

class Breaker(allowedFailure: Int, perTime: FiniteDuration) {

  private var failureCount = 0
  private var failureTimespanStart = 0L

  def protect[A](op: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    val now = System.nanoTime()

    if (!isFailed(now)) {
      val setFail = (t: Throwable) => {
        setFailed(now)
        t
      }
      op.transform(identity, setFail)
    } else Future.failed(FailedFastException())
  }

  private def isFailed(now: Long) =
    failureCount > allowedFailure && inTimespan(now)

  private def inTimespan(now: Long) =
    now < failureTimespanStart + perTime.toNanos

  private def setFailed(now: Long): Unit = synchronized {
    if (inTimespan(now))
      failureCount += 1
    else {
      failureTimespanStart = now
      failureCount = 1
    }
  }
}

object Breaker {
  def apply(allowedFailure: Int, perTime: FiniteDuration): Breaker =
    new Breaker(allowedFailure, perTime)
}
