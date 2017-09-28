package example

import org.scalatest._

import scala.concurrent.Future

class Breaker extends AsyncFlatSpec with Matchers {
  "The breaker" should "return the async result if it succeeds" in {
    val expectedResult = "is good"
    val op = Future.successful(expectedResult)
    protect(op).map(_ shouldBe expectedResult)
  }
}
