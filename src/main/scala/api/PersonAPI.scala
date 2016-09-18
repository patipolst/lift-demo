package code.api

import net.liftweb.http.rest.{RestHelper, RestContinuation}
import net.liftweb.http._
import net.liftweb.json._, JsonDSL._
import net.liftweb.util.BasicTypesHelpers._
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.FieldError
import code.model._
import scala.util.{ Success}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object PersonAPI extends RestHelper {
  serve {
    List("person") prefix {
      case Get(Nil, req) => getAllPersons(Person.findAll())
      case Get(AsLong(pId) :: Nil, req) => findPerson(pId)
      case Get("findparm" :: Nil, req) => findPersonUsingParm(req.params.get("id").getOrElse(Nil))
      case JsonPost("create" :: Nil, jval -> req) => createPersons(jval)
      case JsonPost("create1" :: Nil, jval -> req) => createPerson(jval)
      case JsonPut("update" :: AsLong(pId) :: Nil, jval -> req) => updatePerson(pId, jval)
      case Delete("delete" :: AsLong(pId) :: Nil, req) => deletePerson(pId)
      case Options(_ :: Nil, req) => OkResponse()
      case Get("test" :: Nil, req) => {
        RestContinuation.async { resp =>
            TestService.getPersons.onComplete {
              case Success(Nil) => resp(JsonResponse("status" -> "Person not found"))
              case Success(v) => resp(JsonResponse(v))
              case _ => resp(JString("uh oh!"))
            }
          }
      }
      case JsonPost("testpost" :: Nil, jval -> req) => {
        RestContinuation.async { resp =>
            TestService.createPerson(jval).onComplete {
              // case Success(Nil) => resp(JsonResponse("status" -> "cannot create person"))
              case Success(v) => resp(JsonResponse(v))
              case s @ _ => println(s); resp(JString("uh oh!"))
            }
          }
      }
    }
  }

  def createPerson(jPerson: JValue): LiftResponse = {
    jPerson.extractOpt[PersonData] match {
      case None => JsonResponse("status" -> "cannot create person")
      case Some(p) => {
        val createdPerson = Person.create.name(p.name).food(p.food)
        if (p.gender.equalsIgnoreCase("female")) createdPerson.gender(Genders.Female)
        createdPerson.validate match {
          case Nil => createdPerson.saveMe(); JsonResponse(formatPersonToJson(createdPerson))
          case errors: List[FieldError] => JsonResponse("error" -> errors.map(e => formatFieldErrorToJson(e)))
        }
      }
    }
  }

  // def createPersons(jPerson: JValue): LiftResponse = {
  //   jPerson.extractOpt[List[PersonData]] match {
  //     case None => JsonResponse("status" -> "cannot create person")
  //     case Some(persons) => {
  //       val createdPersons = persons.map(p => {
  //         val tempPerson = Person.create.name(p.name).food(p.food)
  //         if (p.gender.equalsIgnoreCase("female")) tempPerson.gender(Genders.Female)
  //         tempPerson.validate match {
  //           case Nil => tempPerson.saveMe()
  //           case errors: List[FieldError] => tempPerson
  //         }
  //       })
  //       createdPersons match {
  //         case errors if createdPersons.exists(p => p.id < 0) => {
  //           DB.rollback(DefaultConnectionIdentifier);
  //           JsonResponse("status" -> "cannot create person[Rollback]")
  //         }
  //         case _ => JsonResponse("data" -> createdPersons.map(p => formatPersonToJson(p)))
  //       }
  //     }
  //   }
  // }

  def createPersons(jPerson: JValue): LiftResponse = {
    jPerson.extractOpt[List[PersonData]] match {
      case None => JsonResponse("status" -> "cannot create person")
      case Some(persons) => {
        val createdPersons = persons.map(p => {
          val tempPerson = Person.create.name(p.name).food(p.food)
          if (p.gender.equalsIgnoreCase("female")) tempPerson.gender(Genders.Female)
          tempPerson.validate match {
            case Nil => tempPerson
            case errors: List[FieldError] => errors
          }
        })
        createdPersons match {
          case allPersons if createdPersons.forall(_.isInstanceOf[Person]) => {
            val savePersons = allPersons.map(p => p.asInstanceOf[Person].saveMe)
            JsonResponse("data" -> savePersons.map(p => formatPersonToJson(p)))
          }
          case _ => {
            val errors = createdPersons.filterNot(_.isInstanceOf[Person]).map(p => p.asInstanceOf[List[FieldError]]).flatten
            JsonResponse("error" -> errors.map(a => a.field + ": " + a.msg.toString()))
          }
        }
      }
    }
  }

  def getAllPersons(persons: List[Person]): LiftResponse = {
    persons match {
      case Nil => JsonResponse("status" -> "Person not found")
      case _ => JsonResponse("data" -> persons.map(p => formatPersonToJson(p)))
    }
  }

  def findPerson(pId: Long): LiftResponse = {
    Person.find(pId) match {
      case Empty => JsonResponse("status" -> s"Person Id: $pId not found")
      case Full(p) => JsonResponse(formatPersonToJson(p))
      case _: Failure => JsonResponse("status" -> "bad request")
    }
  }

  def findPersonUsingParm(pIds: List[String]): LiftResponse = {
    pIds match {
      case Nil => JsonResponse("status" -> "Person Id not given")
      case ids: List[String] => {
        val persons = ids.map(id => Person.find(id))
        persons match {
          case emptyList if persons.forall(_.isEmpty) => JsonResponse("status" -> "Person not found")
          case _ => JsonResponse("data" -> persons.filterNot(_.isEmpty).map(p => formatPersonToJson(p.get)))
        }
      }
    }
  }

  def deletePerson(pId: Long): LiftResponse = {
    Person.find(pId) match {
      case Empty => JsonResponse("status" -> s"Person Id: $pId not found")
      case Full(p) => {
        val tempPersonJson = formatPersonToJson(p)
        Book.findAll(By(Book.personId, pId)).map(b => Book.delete_!(b))
        Person.delete_!(p)
        JsonResponse("deleted" -> tempPersonJson)
      }
      case _: Failure => JsonResponse("status" -> "bad request")
    }
  }

  def updatePerson(pId: Long, jPerson: JValue): LiftResponse = {
    Person.find(pId) match {
      case Empty => JsonResponse("status" -> s"Person Id: $pId not found")
      case Full(foundPerson) => {
        jPerson.extractOpt[PersonData] match {
          case None => JsonResponse("status" -> s"Person Id: $pId cannot be updated")
          case Some(p) => {
            foundPerson.name.set(p.name)
            foundPerson.food.set(p.food)
            foundPerson.saveMe()
            JsonResponse("updated" -> formatPersonToJson(foundPerson))
          }
        }
      }
      case _: Failure => JsonResponse("status" -> "bad request")
    }
  }

  def formatPersonToJson(p: Person): JValue = {
    (("id" -> JInt(p.id.get)) ~ ("name" -> JString(p.name)) ~ ("gender" -> JString(p.gender.toString())) ~
    ("food" -> JString(p.food)) ~ ("books" -> (p.books.map(b => BookAPI.formatBookToJson(b)))))
  }

  def formatFieldErrorToJson(f: FieldError): JValue = {
    ((f.field.toString() -> f.msg.toString()))
  }
}
