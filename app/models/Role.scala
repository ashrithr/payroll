package models

import play.api.libs.json.{Reads, Writes}
import reactivemongo.bson.{BSON, BSONHandler, BSONString}
import utils.EnumUtils

object Role extends Enumeration {
  type Role = Value
  val SIMPLE_USER = Value("simple_user")
  val CONSULTANT = Value("consultant")
  val ADMIN = Value("admin")
  val OWNER = Value("owner")

  def isRoleType(r: String) = values.exists(_.toString == r)

  implicit val enumReads: Reads[Role] = EnumUtils.enumReads(Role)
  implicit val enumWrites: Writes[Role] = EnumUtils.enumWrites

  implicit object BSONEnumHandler extends BSONHandler[BSONString, Role] {
    def read(doc: BSONString) = Role.Value(doc.value)
    def write(stats: Role) = BSON.write(stats.toString)
  }
}