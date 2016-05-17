package models

import java.util.UUID

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

case class TeamMember(userId: String, rate: String)

case class Project(
  projectName: String,
  projectCode: UUID,
  startDate: Option[DateTime],
  endDate: Option[DateTime],
  preferredCurrency: String,
  biWeekly: Boolean,
  monthly: Boolean,
  notes: Option[String],
  vendorId: String,
  clientId: String,
  paymentNet: PaymentNet.Net,
  team: Option[List[TeamMember]],
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Project extends DocumentDao[Project] {

  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

  implicit val teamMemberJsonFormat = Json.format[TeamMember]
  implicit val projectJsonFormat = Json.format[Project]

  override val collectionName: String = "projects"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("projectName" -> IndexType.Hashed))

}