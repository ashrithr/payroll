package models

import play.api.libs.json.{Reads, Writes}
import reactivemongo.bson.{BSON, BSONHandler, BSONString}
import utils.EnumUtils

object TimesheetStatus extends Enumeration {

  type Status = Value
  val NOT_SUBMITTED = Value("not_submitted")
  val RE_SUBMITTED = Value("re_submitted")
  val SAVED = Value("saved")
  val RE_SAVED = Value("re_saved")
  val SUBMITTED = Value("submitted")
  val APPROVED = Value("approved")
  val APPROVAL_WITHDRAWN = Value("approval_withdrawn")
  val DENIED = Value("denied")

  def isStatusType(s: String) = values.exists(_.toString == s)

  implicit val enumReads: Reads[Status] = EnumUtils.enumReads(TimesheetStatus)
  implicit val enumWrites: Writes[Status] = EnumUtils.enumWrites

  implicit object BSONEnumHandler extends BSONHandler[BSONString, Status] {
    def read(doc: BSONString) = TimesheetStatus.Value(doc.value)
    def write(stats: Status) = BSON.write(stats.toString)
  }

}
