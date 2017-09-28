package example

import org.scalatest._

import scala.concurrent.Future
import scala.concurrent.duration._

class BreakerSpec extends AsyncFlatSpec with Matchers {

  case class TestException(msg: String) extends Exception(msg)

  def defaultBreaker = Breaker(0, 1.second)

  "The breaker" should "return the async result if it succeeds" in {
    val expectedResult = "is good"
    val op = Future.successful(expectedResult)
    defaultBreaker.protect(op).map(_ shouldBe expectedResult)
  }

  val failingOp = Future.failed(TestException("testfailure"))

  it should "return the failed async result if it fails" in {
    recoverToSucceededIf[TestException](defaultBreaker.protect(failingOp))
  }

  it should "return a failed Future with an FailedFastException when already one operation failed" in {
    val breaker = defaultBreaker

    for {
      _ <- breaker.protect(failingOp).failed
      assertion <- recoverToSucceededIf[FailedFastException](
        breaker.protect(failingOp))
    } yield assertion
  }

  it should "not call the operation a second time when already one operation failed" in {
    var counter = 0

    def op = Future {
      counter += 1
      throw TestException("testfailure")
    }

    val breaker = defaultBreaker

    for {
      _ <- breaker.protect(op).failed
      _ <- breaker.protect(op).failed
    } yield {
      counter shouldBe 1
    }
  }

  it should "open circuit breaker after configured failures per time" in {
    val breaker = Breaker(2, 200.millis)

    var counter = 0

    def op = Future {
      counter += 1
      throw TestException("testfailure")
    }

    for {
      _ <- breaker.protect(op).failed
      _ <- breaker.protect(op).failed
      _ <- breaker.protect(op).failed
      _ <- breaker.protect(op).failed
    } yield {
      counter shouldBe 3
    }
  }
}
