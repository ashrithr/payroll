package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

import scala.util.matching.Regex

object PasswordRecovery {

  val emailForm = Form(single("email" -> email))

  val passwordRegex = """^(?=.*[A-Z])(?=.*[!@#$&*])(?=.*[0-9])(?=.*[a-z]).{10,}$""".r

  val passwordCheckConstraint: Constraint[String] = Constraint("constraints.passwordcheck")({
    plainText =>
      val errors = plainText match {
        case passwordRegex() => Nil
        case _ => Seq(ValidationError("Password should contain at-least one uppercase, one lowercase, one digit and one special character(!@#$&*)"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })

  def resetPasswordForm(implicit messages:Messages) = Form(tuple(
    "password1" -> nonEmptyText.verifying(minLength(10)).verifying(passwordCheckConstraint),
    "password2" -> nonEmptyText
  ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2))

}
