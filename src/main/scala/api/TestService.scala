package code.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.after
import akka.stream.ActorMaterializer
import net.liftweb.json._
import net.liftweb.util.Helpers.tryo
import net.liftweb.util.Props

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import code.model._
import net.liftweb.json._, JsonDSL._

object TestService {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  // private val uri = "http://localhost:8080/"
  // private val key = "person"
  private val timeoutMs = 5000

  // private val params = Map("key" -> key)
  private def getRequest = HttpRequest(HttpMethods.GET, uri = "http://localhost:8080/person")
  private def call(request: HttpRequest): Future[HttpResponse] = Future.firstCompletedOf(
    Http().singleRequest(request) ::
      after(timeoutMs.millis, system.scheduler)(Future.failed(new java.util.concurrent.TimeoutException)) :: Nil)

  private def callAndParse(request: HttpRequest): Future[JValue] = for {
    response <- call(request)
    unmarshed <- Unmarshal(response.entity).to[String]
  } yield parse(unmarshed)

  private def extractJsonData(j: JValue): Option[List[PersonExtract]] = (j \ "data").extractOpt[List[PersonExtract]]
  private def extractJsonData2(j: JValue): Option[PersonExtract] = j.extractOpt[PersonExtract]

  def getPersons: Future[Seq[JValue]] = callAndParse(getRequest).map {
    j: JValue =>
    println(j)
      extractJsonData(j) match {
        case Some(List(strs @ _*)) => strs.map(a => formatToJson(a))
        case None => Nil
      }
  }

  private def httpEntityJson(json: JValue = JNothing): HttpEntity.Strict =
    HttpEntity(ContentTypes.`application/json`, compactRender(json))

  def postJsonRequest(uri: String, json: JValue = JNothing): HttpRequest =
    HttpRequest(HttpMethods.POST, uri = Uri(uri), entity = httpEntityJson(json))

  def createPerson(json: JValue = JNothing): Future[JValue] = {
    val createRequest = postJsonRequest("http://localhost:8080/person/create1", json)
    callAndParse(createRequest).map {
      j: JValue =>
      println(j)
      extractJsonData2(j) match {
        case Some(a) => formatToJson(a)
        case None => j
      }
    }
  }

  // def getPerson: Future[Option[JValue]] = {
  //   getPersons.map { strs: Seq[JValue] => strs.headOption }
  // }

  case class PersonExtract(id: Long, name: String, gender: String, food: String, books: List[Book])

  def formatToJson(p: PersonExtract): JValue = {
    (("id" -> JInt(p.id)) ~ ("name" -> JString(p.name)) ~ ("gender" -> JString(p.gender.toString())) ~
    ("food" -> JString(p.food)) ~ ("books" -> (p.books.map(b => BookAPI.formatBookToJson(b)))))
  }
}
