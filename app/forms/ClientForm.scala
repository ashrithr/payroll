package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.validation.Constraints._


object ClientForm {

  case class FormData(
    companyName: String,
    companyAddress: Option[String],
    primaryContacts: Seq[String], // Email will be sent out for invoices to this email addresses
    secondaryContacts: Option[Seq[String]]
  )

  def nonEmptySeq[T]: Constraint[Seq[T]] = Constraint[Seq[T]]("constraint.required") { o =>
    if (o.nonEmpty) Valid else Invalid(ValidationError("error.required"))
  }

  def form = Form[FormData](
    mapping(
      "companyName" -> nonEmptyText.verifying(minLength(6)),
      "companyAddress" -> optional(text),
      "primaryContacts" -> seq(nonEmptyText).verifying(nonEmptySeq),
      "secondaryContacts" -> optional(seq(text))
    )(FormData.apply)(FormData.unapply)
  )
}
