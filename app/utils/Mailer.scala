package utils

import javax.inject.Inject

import com.typesafe.config.{Config, ConfigFactory}
import models.Profile
import play.api.i18n.Messages
import play.api.libs.mailer._
import play.api.libs.concurrent.Execution.Implicits._
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Future

class Mailer @Inject() (mailer:MailerClient) {

  val config: Config = ConfigFactory.load()
  val from = config.as[String]("play.mailer.from")
  val replyTo = config.as[Option[String]]("play.mailer.reply")

  def sendEmailAsync(recipients:String*)(subject:String, bodyHtml:Option[String], bodyText:Option[String]) = {
    Future {
      sendEmail(recipients:_*)(subject, bodyHtml, bodyText)
    } recover {
      case e => play.api.Logger.error("error sending email", e)
    }
  }

  def sendEmail(recipients:String*)(subject:String, bodyHtml:Option[String], bodyText:Option[String]) {
    val email = Email(subject = subject, from = from, to = recipients, bodyHtml = bodyHtml, bodyText = bodyText, replyTo = replyTo)
    mailer.send(email)
    ()
  }

  def welcome(profile:Profile, link:String)(implicit messages:Messages) = {
    sendEmailAsync(profile.email.get)(
      subject = Messages("mail.welcome.subject"),
      bodyHtml = Some(views.html.mails.welcome(profile.firstName.get, link).toString),
      bodyText = Some(views.html.mails.welcomeText(profile.firstName.get, link).toString)
    )
  }

  def resetPassword(email:String, link:String)(implicit messages:Messages) = {
    sendEmailAsync(email)(
      subject = Messages("mail.reset.subject"),
      bodyHtml = Some(views.html.mails.resetPassword(email, link).toString),
      bodyText = Some(views.html.mails.resetPasswordText(email, link).toString)
    )
  }

  /*Admin notifications*/

  def newUserSignUp(user: String, emails: List[String])(implicit messages: Messages) = {
    sendEmailAsync(emails:_*)(
      subject = Messages("mail.notify.admin.new.user.subject"),
      bodyHtml = Some(views.html.mails.newUserSignUp(user).toString),
      bodyText = Some(views.html.mails.newUserSignUpText(user).toString)
    )
  }

  def invoicesDueToday(emails: List[String])(implicit messages: Messages) = {
    sendEmailAsync(emails:_*)(
      subject = Messages("mail.notify.admin.invoices.due.today.subject"),
      bodyHtml = Some(views.html.mails.invoicesDueToday().toString),
      bodyText = Some(views.html.mails.invoicesDueTodayText().toString)
    )
  }

  def invoicesPastDue(emails: List[String])(implicit messages: Messages) = {
    sendEmailAsync(emails:_*)(
      subject = Messages("mail.notify.admin.invoices.past.due.subject"),
      bodyHtml = Some(views.html.mails.invoicesPastDue().toString),
      bodyText = Some(views.html.mails.invoicesPastDueText().toString)
    )
  }

}