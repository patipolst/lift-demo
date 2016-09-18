package bootstrap.liftweb

import net.liftweb.http.{ LiftRules, S }
import net.liftweb.mapper._
import net.liftweb.util.{ Props, Mailer }
import net.liftweb.common.{ Full, Empty }

import javax.mail.{ Authenticator, PasswordAuthentication}

import code.model._
import code.api._

class Boot {
  def boot = {
    LiftRules.addToPackages("code")
    LiftRules.statelessDispatch.append(TestAPI)
    LiftRules.statelessDispatch.append(PersonAPI)
    LiftRules.statelessDispatch.append(BookAPI)
    LiftRules.statelessDispatch.append(MailerAPI)

    LiftRules.supplementalHeaders.default.set(List(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "origin, content-type, authorization")
    )

    //H2 DB connection section
    if(!DB.jndiJdbcConnAvailable_?){
      val vendor = new StandardDBVendor(
        Props.get("db.driver") openOr "org.h2.Driver",
        Props.get("db.url") openOr "jdbc:h2:mem:testdb",
        Empty, //Props.get("db.user"),
        Empty //Props.get("db.password")
      )

      S.addAround(DB.buildLoanWrapper)
      MapperRules.createForeignKeys_? = (_) => true
      LiftRules.unloadHooks.append(vendor.closeAllConnections_!_)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
      Schemifier.schemify(true, Schemifier.infoF _, Person, Book)

      // Check db is created when boot
      println("Person: " + Person.findAll())
      println("Book: " + Book.findAll())
    }

    // NOTE ADD Email config
    // configMailer("smtp.abc.com", "abc@abc.com", "123") // gmail by default
  }

  def configMailer(host: String, user: String, password: String) = {
    Props.get("mail.smtp.auth") openOr "true"
    Props.get("mail.smtp.starttls.enable") openOr "true"
    Props.get("mail.smtp.host") openOr host
    // Props.get("mail.user") openOr user
    // Props.get("mail.password") openOr password
    Mailer.authenticator = Full(new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication(user, password)
    })
  }
}
