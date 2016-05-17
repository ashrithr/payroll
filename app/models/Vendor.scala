package models

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

import scala.concurrent.Future

case class Vendor(
  companyName: String,
  companyAddress: Option[String],
  primaryContacts: Seq[String],
  secondaryContacts: Option[Seq[String]],
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Vendor extends DocumentDao[Vendor] {

  implicit val vendorJsonFormat = Json.format[Vendor]

  override val collectionName: String = "vendors"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("companyName" -> IndexType.Hashed))

}
