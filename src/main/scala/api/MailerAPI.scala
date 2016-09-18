package code.api

import net.liftweb.http.rest.RestHelper
import net.liftweb.http._
import net.liftweb.util.Mailer
import net.liftweb.util.Mailer._

object MailerAPI extends RestHelper {
  serve {
    List("mailer") prefix {
      case Post(_ :: Nil, req) => sendMail()
      case Options(_ :: Nil, req) => OkResponse()
    }
  }

  def sendMail(): LiftResponse = {
    Mailer.sendMail(
      From("patipol.s@dotography.com"),
      Subject("Hello"),
      To("patipol.s@dotography.com"),
      PlainMailBodyType("Hello from Lift") )

      PlainTextResponse("mail has been sent")
  }
}
