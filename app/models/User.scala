package models

import java.util.UUID

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import com.mohiva.play.silhouette.api.util.PasswordInfo
import play.api.libs.json.Json

case class Profile(
  loginInfo:LoginInfo,
  confirmed: Boolean,
  email:Option[String],
  firstName: Option[String],
  lastName: Option[String],
  fullName: Option[String],
  passwordInfo:Option[PasswordInfo],
  avatarUrl: Option[String]) {
}

case class User(id: UUID, profiles: List[Profile], role: Role.Role) extends Identity {

  def profileFor(loginInfo: LoginInfo) = profiles.find(_.loginInfo == loginInfo)
  def fullName(loginInfo: LoginInfo) = profileFor(loginInfo).flatMap(_.fullName)
  def email(loginInfo: LoginInfo) = profileFor(loginInfo).flatMap(_.email)

}

object User {

  // Generates Writes and Reads for Profile, PasswordInfo and User
  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val profileJsonFormat = Json.format[Profile]
  implicit val userJsonFormat = Json.format[User]

}