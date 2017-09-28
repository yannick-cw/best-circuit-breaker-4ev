package example

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

case class FailedFastException() extends RuntimeException("Failed fast because circuit breaker is in open state")

class Breaker(allowedFailure: Int, perTime: FiniteDuration) {

  private var isFailed = false

  def protect[A](op: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    if (!isFailed) {
      val setFail = (t: Throwable) => {
        isFailed = true
        t
      }
      op.transform(identity, setFail)
    } else Future.failed(FailedFastException())
  }
}

object Breaker {
  def apply(allowedFailure: Int, perTime: FiniteDuration): Breaker = new Breaker(allowedFailure, perTime)
}
