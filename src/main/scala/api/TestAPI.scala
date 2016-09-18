package code.api

import net.liftweb.http.rest.{ RestHelper, RestContinuation }
import net.liftweb.http._
import net.liftweb.json._, JsonDSL._
import net.liftweb.util.BasicTypesHelpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{ Success, Failure }

case class Man(name: String, age: Option[Int], phoneNumber: List[PhoneNumber])
case class PhoneNumber(number: String)

object TestAPI extends RestHelper {
  serve {
    // case Req(List("sayhello"), _, GetRequest) => PlainTextResponse("Hellooooo !")
    case Get("sayhello" :: Nil, req) => PlainTextResponse("Hello kub")
    case Req("echo" :: word :: Nil, _, _) => PlainTextResponse(word)
    case Req("echo-number" :: AsInt(num) :: Nil, _, _) => PlainTextResponse(add1(num).toString())
    case "testpost" :: Nil Post req => req.body.map(ab => new String(ab, "utf-8")).map(s => PlainTextResponse(s))
    case Get("echo-param" :: Nil, req) => PlainTextResponse(req.params.toString())
    case Get("echo-param-dog" :: Nil, req) => PlainTextResponse(req.params.get("dog").getOrElse(Nil).toString())
    case Get("test-json" :: Nil, req) => JsonResponse(
      // JObject(
      // JField("name", JString("john")) ::
      // JField("age", JInt(18)) :: Nil
      // )
      ("name" -> "john") ~ ("age" -> 18)
    )
    case Get("test-future" :: Nil, req) => RestContinuation.async { reply =>
      val futureResult = doFuture
      doFuture.onComplete {
        case Success(str) => reply(PlainTextResponse(str))
        case Failure(e) => reply(PlainTextResponse(e.toString()))
      }
    }
    case JsonPost("test-extract" :: Nil, jval -> req) => PlainTextResponse(jval.extractOpt[List[Man]].toString())
  }

  def add1(i: Int): Int =  i + 1

  def doFuture: Future[String] = Future {
    Thread.sleep(1000)
    "ok"
  }

}
