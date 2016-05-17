package models

import daos.core.{DocumentDao, TemporalModel}
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

case class Invoice(
  startDate: DateTime,
  endDate: DateTime,
  vendorId: String,
  clientId: String,
  consultantId: String,
  projectId: String,
  totalHours: Double,
  paymentNet: PaymentNet.Net,
  dueDate: DateTime,
  pricePerHour: Double,
  totalCost: Double,
  paymentReceived: Boolean = false,
  hidden: Boolean = false,
  var _id: Option[BSONObjectID] = None,
  var created: Option[DateTime] = None,
  var updated: Option[DateTime] = None
) extends TemporalModel

object Invoice extends DocumentDao[Invoice] {

  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat // This is required

  implicit val invoiceJsonFormat = Json.format[Invoice]

  override val collectionName: String = "invoices"

  override def ensureIndexes: Future[Boolean] = ensureIndex(List("" -> IndexType.Hashed))

}
