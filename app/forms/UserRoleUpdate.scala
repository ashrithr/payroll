package forms

import play.api.data.Form
import play.api.data.Forms._

object UserRoleUpdate {

  case class FormData(role: String)

  def form = Form[FormData](
    mapping(
      "role" -> nonEmptyText
    )
    ((role) => FormData(role))
    (formData => Some(formData.role))
  )

}
