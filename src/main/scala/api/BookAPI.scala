package code.api

import net.liftweb.http.rest.RestHelper
import net.liftweb.http._
import net.liftweb.json._, JsonDSL._
import net.liftweb.util.BasicTypesHelpers._
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.FieldError
import scala.util.{ Try, Success, Failure => tryFailure }
import java.text.SimpleDateFormat
import code.model._

object BookAPI extends RestHelper {
  serve {
    List("book") prefix {
      case Get(Nil, req) => getAllBooks(Book.findAll())
      case Get(AsLong(bId) :: Nil, req) => findBook(bId)
      case Get("findparm" :: Nil, req) => findBookUsingParm(req.params.get("id").getOrElse(Nil))
      case Get("person" :: AsLong(pId) ::  Nil, req) => getAllBooks(Book.findAll(By(Book.personId, pId)))
      case JsonPost("create" :: Nil, jval -> req) => createBooks(jval)
      case JsonPost("create1" :: Nil, jval -> req) => createBook(jval)
      case JsonPut("update" :: AsLong(bId) :: Nil, jval -> req) => updateBook(bId, jval)
      case Delete("delete" :: AsLong(bId) :: Nil, req) => deleteBook(bId)
      case Options(_ :: Nil, req) => OkResponse()
    }
  }

  def createBook(jBook: JValue): LiftResponse = {
    jBook.extractOpt[BookData] match {
      case None => JsonResponse("status" -> "cannot create book")
      case Some(b) => {
        val createdBook = Book.create.personId(b.personId).name(b.name).description(b.description)
        createdBook.validate match {
          case Nil => {
            Try(Book.create.personId(b.personId).name(b.name).description(b.description).saveMe())
            match {
              case Success(b) => JsonResponse(formatBookToJson(b))
              case tryFailure(e) => JsonResponse("status" -> "cannot create book")
            }
          }
          case errors: List[FieldError] => JsonResponse("error" -> errors.map(e => formatFieldErrorToJson(e)))
        }
      }
    }
  }

  // NOTE: no book's name validation
  // def createBooks(jBook: JValue): LiftResponse = {
  //   jBook.extractOpt[List[BookData]] match {
  //     case None => JsonResponse("status" -> "cannot create book")
  //     case Some(books) => {
  //       Try(books.map(b => Book.create.personId(b.personId).name(b.name).
  //       description(b.description).saveMe()))
  //       match {
  //         case Success(books) => JsonResponse(books.map(b => formatBookToJson(b)))
  //         // if one failed, it will rollback all transactions
  //         case tryFailure(e) => DB.rollback(DefaultConnectionIdentifier); JsonResponse("status" -> "cannot create book[Rollback]")
  //       }
  //     }
  //   }
  // }

// NOTE: can use find by ids to get List[Box[Person]] to check as well
  def createBooks(jBook: JValue): LiftResponse = {
    jBook.extractOpt[List[BookData]] match {
      case None => JsonResponse("status" -> "cannot create book")
      case Some(books) => {
        val createdBooks = books.map(b => {
          val tempBook = Book.create.personId(b.personId).name(b.name).description(b.description)
          tempBook.validate match {
            case Nil => tempBook
            case errors: List[FieldError] => errors
          }
        })
        createdBooks match {
          case allBooks if createdBooks.forall(_.isInstanceOf[Book]) => {
            Try(books.map(b => Book.create.personId(b.personId).name(b.name).description(b.description).saveMe()))
            match {
              case Success(books) => JsonResponse("data" -> books.map(b => formatBookToJson(b)))
              case tryFailure(e) => {
                DB.rollback(DefaultConnectionIdentifier)
                JsonResponse("status" -> "cannot create book[Rollback]")
              }
            }
          }
          case _ => {
            // JsonResponse("error" -> createdBooks.filterNot(_.isInstanceOf[Book]).map(e => e.toString()))//formatFieldErrorToJson(e.asInstanceOf[FieldError])))
            val errors = createdBooks.filterNot(_.isInstanceOf[Book]).map(p => p.asInstanceOf[List[FieldError]]).flatten
            JsonResponse("error" -> errors.map(a => a.field + ": " + a.msg.toString()))
          }
        }
      }
    }
  }

  def getAllBooks(books: List[Book]): LiftResponse = {
    books match {
      case Nil => JsonResponse("status" -> "Book not found")
      case _ => JsonResponse("data" -> books.map(b => formatBookToJson(b)))
    }
  }

  def findBook(bId: Long): LiftResponse = {
    Book.find(bId) match {
      case Empty => JsonResponse("status" -> s"Book Id: $bId not found")
      case Full(b) => JsonResponse(formatBookToJson(b))
      case _: Failure => JsonResponse("status" -> "bad request")
    }
  }

  def findBookUsingParm(bIds: List[String]): LiftResponse = {
    bIds match {
      case Nil => JsonResponse("status" -> "Book Id not given")
      case ids: List[String] => {
        val books = ids.map(id => Book.find(id))
        books match {
          case empty if books.forall(_.isEmpty) => JsonResponse("status" -> "Book not found")
          case _ => JsonResponse("data" -> books.filterNot(_.isEmpty).map(p => formatBookToJson(p.get)))
        }
      }
    }
  }

  def deleteBook(bId: Long): LiftResponse = {
    Book.find(bId) match {
      case Empty => JsonResponse("status" -> s"Book Id: $bId not found")
      case Full(b) => {
        Book.delete_!(b)
        JsonResponse("deleted" -> formatBookToJson(b))
      }
      case _: Failure => JsonResponse("status" -> "bad request")
    }
  }

  def updateBook(bId: Long, jBook: JValue): LiftResponse = {
    Book.find(bId) match {
      case Empty => JsonResponse("status" -> s"Book Id: $bId not found")
      case Full(foundBook) => {
        jBook.extractOpt[BookData] match {
          case None => JsonResponse("status" -> s"Book Id: $bId cannot be updated")
          case Some(b) => {
            foundBook.personId.set(b.personId)
            foundBook.name.set(b.name)
            foundBook.description.set(b.description)
            Try(foundBook.saveMe()) match {
              case Success(b) => JsonResponse("updated" -> formatBookToJson(b))
              case tryFailure(e) => JsonResponse("status" -> s"Book Id: $bId cannot be updated")
            }
          }
        }
      }
      case _: Failure => JsonResponse("status" -> "bad request")
    }
  }

  def formatBookToJson(b: Book): JValue = {
    (("id" -> JInt(b.id.get)) ~ ("name" -> JString(b.name)) ~
    ("description" -> JString(b.description)) ~
    ("addedDate" -> JString((new SimpleDateFormat("dd/MM/yyyy")).format(b.addedDate.get))) ~
    ("personId" -> JInt(b.personId.get)))
  }

  def formatFieldErrorToJson(f: FieldError): JValue = {
    ((f.field.toString() -> f.msg.toString()))
  }
}
