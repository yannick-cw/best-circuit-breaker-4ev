package example

import scala.concurrent.{ExecutionContext, Future}

case class FailedFastException()
    extends RuntimeException(
      "Failed fast because circuit breaker is in open state")

class Breaker {

  @volatile private var isFailed = false

  def protect[A](op: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    if (!isFailed) {
      val result = op
      result.failed.foreach(_ => {
        isFailed = true
      })
      result
    } else Future.failed(FailedFastException())
  }
}
