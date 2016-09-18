package lm.service

/**
  * Created by polpat on 25/7/2559.
  */
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

object UUIDService {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  private val uri = Props.get("service.uuid.uri") openOr "http://localhost:7020/v1.0/uuid"
  private val key = Props.get("service.uuid.key") openOr ""
  private val timeoutMs = Props.get("service.uuid.timeout").flatMap{str => tryo(str.toInt)}.openOr(7000)

  private val params = Map("key" -> key)
  private def getRequest = HttpRequest(HttpMethods.GET, uri = Uri(uri).withQuery(Uri.Query(params)))
  private def call(request: HttpRequest): Future[HttpResponse] = Future.firstCompletedOf(
    Http().singleRequest(request) ::
      after(timeoutMs.millis, system.scheduler)(Future.failed(new java.util.concurrent.TimeoutException)) :: Nil)

  private def callAndParse(request: HttpRequest): Future[JValue] = for {
    response <- call(request)
    unmarshed <- Unmarshal(response.entity).to[String]
  } yield parse(unmarshed)

  private def extractJsonData(j: JValue): Option[List[String]] = (j \ "data").extractOpt[List[String]]

  def getUUIDs: Future[Seq[String]] = callAndParse(getRequest).map {
    j: JValue =>
      extractJsonData(j) match {
        case Some(List(strs @ _*)) => List(strs: _*)
        case None => Nil
      }
  }
  def getUUID: Future[Option[String]] = getUUIDs.map { strs: Seq[String] => strs.headOption }
}
