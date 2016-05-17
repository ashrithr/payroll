package models

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.api.indexes.IndexType

import scala.concurrent.Future

case class Contact(
  firstName: String,
  lastName: String,
  email: String,
  title: Option[String],
  officeNum: Option[String],
  mobileNum: Option[String],
  faxNum: Option[String],
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Contact extends DocumentDao[Contact] {

  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

  implicit val contactJsonFormat = Json.format[Contact]

  override val collectionName: String = "contacts"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("email" -> IndexType.Hashed))

}