package forms

import play.api.data.Form
import play.api.data.Forms._

object InvoiceForm {

  case class FormData(
    project: String,
    consultant: String,
    startDate: String,
    endDate: String
  )

  def form = Form[FormData](
    mapping(
      "project" -> nonEmptyText,
      "consultant" -> nonEmptyText,
      "startDate" -> nonEmptyText,
      "endDate" -> nonEmptyText
    )(FormData.apply)(FormData.unapply)
  )

}
