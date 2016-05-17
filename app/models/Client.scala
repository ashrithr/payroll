package models

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

import scala.concurrent.Future

case class Client(
  companyName: String,
  companyAddress: Option[String],
  primaryContacts: Seq[String],
  secondaryContacts: Option[Seq[String]],
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Client extends DocumentDao[Client] {

  implicit val clientJsonFormat = Json.format[Client]

  override val collectionName: String = "clients"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("companyName" -> IndexType.Hashed))

}