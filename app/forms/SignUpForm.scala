package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

object SignUpForm {

  case class SignUpData(email:String, password:String, firstName:String, lastName:String)

  val allNumbers = """\d*""".r
  val allLetters = """[A-Za-z]*""".r

  val passwordCheckConstraint: Constraint[String] = Constraint("constraints.passwordcheck")({
    plainText =>
      val errors = plainText match {
        case allNumbers() => Seq(ValidationError("Password is all numbers, use a mix of numbers and letters."))
        case allLetters() => Seq(ValidationError("Password is all letters, use a mix of numbers and letters."))
        case _ => Nil
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
  })


  def signUpForm(implicit messages:Messages) = Form[SignUpData](
    mapping(
      "email" -> email,
      "password" -> tuple(
        "password1" -> nonEmptyText.verifying(minLength(12)).verifying(passwordCheckConstraint),
        "password2" -> nonEmptyText
      ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2),
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText
    )
    ((email, password, firstName, lastName) => SignUpData(email, password._1, firstName, lastName)) //apply
    (signUpData => Some((signUpData.email, ("",""), signUpData.firstName, signUpData.lastName))) //unapply
  )

}
