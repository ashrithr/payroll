package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

object ContactForm {

  case class FormData(
    firstName: String,
    lastName: String,
    email: String,
    title: Option[String],
    officeNum: Option[String],
    mobileNum: Option[String],
    faxNum: Option[String]
  )

  def form(implicit messages: Messages) = Form[FormData](
    mapping(
      "firstName" -> nonEmptyText.verifying(minLength(2)),
      "lastName" -> nonEmptyText.verifying(minLength(2)),
      "email" -> email,
      "title" -> optional(text),
      "officeNum" -> optional(text),
      "mobileNum" -> optional(text),
      "faxNum" -> optional(text)
    )(FormData.apply)(FormData.unapply)
  )

}
