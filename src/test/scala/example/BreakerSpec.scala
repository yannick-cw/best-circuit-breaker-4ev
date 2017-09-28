package example

import org.scalatest._

import scala.concurrent.Future
import Breaker.protect

class BreakerSpec extends AsyncFlatSpec with Matchers {

  case class TestException(msg: String) extends Exception(msg)

  "The breaker" should "return the async result if it succeeds" in {
    val expectedResult = "is good"
    val op             = Future.successful(expectedResult)
    protect(op).map(_ shouldBe expectedResult)
  }

  it should "return the failed async result if it fails" in {
    val failingOp = Future.failed(TestException("testfailure"))
    recoverToSucceededIf[TestException](protect(failingOp))
  }
}
