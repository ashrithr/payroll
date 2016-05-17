package models

import java.util.UUID

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

case class Consultant(
  userId: UUID,
  team: Int,
  billingRate: Int,
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Consultant extends DocumentDao[Consultant] {

  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is requiredw

  implicit val teamMemberJsonFormat = Json.format[Consultant]

  override val collectionName: String = "consultants"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("userId" -> IndexType.Hashed))

}