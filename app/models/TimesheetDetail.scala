package models

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

case class TimesheetDetail(
  workDay: DateTime,
  hours: Double = 0.0,
  loggedAt: DateTime,
  timesheetId: String,
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object TimesheetDetail extends DocumentDao[TimesheetDetail] {

  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

  implicit val timesheetDetailJsonFormat = Json.format[TimesheetDetail]

  override val collectionName: String = "timesheet_details"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("workDay" -> IndexType.Hashed))

}