package code.model

import net.liftweb.mapper._
import net.liftweb.util.FieldError

class Book extends LongKeyedMapper[Book] with IdPK {
  def getSingleton = Book

  object name extends MappedString(this, 100) {
    def validateEmpty(name: String) = {
      if (!name.trim().isEmpty()) Nil
      else List(FieldError(this, "Name is empty"))
    }
    override def validations = validateEmpty _ :: super.validations

    override def setFilter = trim _ :: Nil
  }

  object description extends MappedString(this, 255)
  object addedDate extends MappedDateTime(this) {
    override def defaultValue = new java.util.Date
  }
  object personId extends MappedLongForeignKey(this, Person)
}

object Book extends Book with LongKeyedMetaMapper[Book]

case class BookData(name: String, description: String, personId: Long)
