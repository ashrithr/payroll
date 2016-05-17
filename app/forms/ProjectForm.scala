package forms

import models.{PaymentNet, TeamMember}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.validation.Constraints._
import reactivemongo.bson.BSONObjectID

object ProjectForm {

  case class FormData(
    projectName: String,
    startDate: Option[String],
    endDate: Option[String],
    preferredCurrency: String,
    biWeekly: Boolean,
    monthly: Boolean,
    notes: Option[String],
    vendorId: String,
    clientId: String,
    paymentNet: String
    //team: List[TeamMember]
  )

  def nonEmptySeq[T]: Constraint[Seq[T]] = Constraint[Seq[T]]("constraint.required") { o =>
    if (o.nonEmpty) Valid else Invalid(ValidationError("error.required"))
  }

  def form = Form[FormData](
    mapping(
      "projectName" -> nonEmptyText.verifying(minLength(6)),
      "startDate" -> optional(nonEmptyText),
      "endDate" -> optional(nonEmptyText),
      "preferredCurrency" -> nonEmptyText,
      "biWeekly" -> boolean,
      "monthy" -> boolean,
      "notes" -> optional(text),
      "vendorId" -> nonEmptyText,
      "clientId" -> nonEmptyText,
      "paymentNet" -> nonEmptyText
      /*"team" -> list(
        mapping(
          "userId" -> nonEmptyText,
          "rate" -> nonEmptyText.verifying(pattern("""\d+(\.\d{1,2})?""".r, error = "Valid amount is required"))
        )(TeamMember.apply)(TeamMember.unapply)
      )*/
    )(FormData.apply)(FormData.unapply)
  )

}
