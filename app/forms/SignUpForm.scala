package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

object SignUpForm {

  case class SignUpData(email:String, password:String, firstName:String, lastName:String)

  def signUpForm(implicit messages:Messages) = Form[SignUpData](
    mapping(
      "email" -> email,
      "password" -> tuple(
        "password1" -> nonEmptyText.verifying(minLength(6)),
        "password2" -> nonEmptyText
      ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2),
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText
    )
    ((email, password, firstName, lastName) => SignUpData(email, password._1, firstName, lastName)) //apply
    (signUpData => Some((signUpData.email, ("",""), signUpData.firstName, signUpData.lastName))) //unapply
  )

}
