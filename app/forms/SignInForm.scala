package forms

import play.api.data.Form
import play.api.data.Forms._

object SignInForm {

  case class SignInData(email:String, password:String, rememberMe:Boolean)

  val signInForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )
    (SignInData.apply)
    (SignInData.unapply)
  )

}
