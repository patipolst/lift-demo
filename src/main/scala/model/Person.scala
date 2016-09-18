package code.model

import net.liftweb.mapper._
import net.liftweb.util.FieldError

class Person extends LongKeyedMapper[Person] with IdPK with OneToMany[Long, Person] {
  def getSingleton = Person

  object name extends MappedString(this, 50) {
    def validateLetter(name: String) = {
      if (name.forall(_.isLetter)) Nil
      else List(FieldError(this, "Name can contain only letter"))
    }
    def validateEmpty(name: String) = {
      if (!name.trim().isEmpty()) Nil
      else List(FieldError(this, "Name is empty"))
    }
    override def validations = valUnique("name must be unique") _ :: validateLetter _ :: validateEmpty _ :: super.validations

    override def setFilter = trim _ :: Nil
  }

  object gender extends MappedGender(this)
  object food extends MappedString(this, 50)
  object books extends MappedOneToMany(Book, Book.personId)
}

object Person extends Person with LongKeyedMetaMapper[Person]

case class PersonData(name: String, gender: String, food: String)
