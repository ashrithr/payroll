package models

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

case class Timesheet(
  projectCode: String,
  consultantId: String,
  weekStart: DateTime,
  weekEnd: DateTime,
  submittedAt: Option[DateTime],
  savedAt: Option[DateTime],
  reSavedAt: Option[DateTime],
  withdrawnAt: Option[DateTime],
  resubmittedAt: Option[DateTime],
  approvedAt: Option[DateTime],
  approvedBy: Option[String],
  disapprovedAt: Option[DateTime],
  disapprovedBy: Option[String],
  totalHours: Double = 0.0,
  status: TimesheetStatus.Status = TimesheetStatus.NOT_SUBMITTED,
  invoiced: Boolean = false,
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Timesheet extends DocumentDao[Timesheet] {

  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

  implicit val timesheetJsonFormat = Json.format[Timesheet]

  override val collectionName: String = "timesheets"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("status" -> IndexType.Hashed))

}
