package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

object SignUpForm {

  case class SignUpData(email:String, password:String, firstName:String, lastName:String)

  /*
   * (?=.*[A-Z])      string should have at-least one uppercase letter.
   * (?=.*[!@#$&*])   string should have at-least one special case letter.
   * (?=.*[0-9])      string should have at-least one digit.
   * (?=.*[a-z])      string should have at-least one lowercase letter.
   * .{12,}           string is of min length 12.
   */
  val passwordRegex = """^(?=.*[A-Z])(?=.*[!@#$&*])(?=.*[0-9])(?=.*[a-z]).{12,}$""".r

  val passwordCheckConstraint: Constraint[String] = Constraint("constraints.passwordcheck")({
    plainText =>
      val errors = plainText match {
        case passwordRegex() => Nil
        case _ => Seq(ValidationError("Password does not meet standard. Should contain at-least one uppercase, one lowercase, one digit and one special character(!@#$&*)"))
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
