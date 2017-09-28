package example

import scala.concurrent.Future

object Breaker {
  def protect[A](op: Future[A]): Future[A] = op
}
