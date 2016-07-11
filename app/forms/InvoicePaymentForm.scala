package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

object InvoicePaymentForm {

  case class FormData(
    paymentReceivedAmount: BigDecimal
  )

  def form(implicit messages: Messages) = Form[FormData](
    mapping(
      "paymentReceivedAmount" -> bigDecimal(10, 2)
    )(FormData.apply)(FormData.unapply)
  )

}
