package tasks

import javax.inject.Inject

import akka.actor.{Actor, Props}
import models.{Invoice, Role}
import org.joda.time.DateTime
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import services.UserService
import utils.Mailer

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._

object InvoiceDueNotifier {
  def props = Props[InvoiceDueNotifier]

  case object DueToday
  case object PastDue
}

class InvoiceDueNotifier @Inject()(val messagesApi: MessagesApi, mailer: Mailer) extends Actor with I18nSupport {

  import InvoiceDueNotifier._

  override def receive: Receive = {
    case DueToday => dueToday
    case PastDue => pastDue
  }

  def dueToday = {
    Invoice.find(Json.obj("hidden" -> false, "paymentReceived" -> false)).map { invoices =>
      invoices.foreach { invoice =>
        if(invoice.dueDate.withTimeAtStartOfDay().isEqual(DateTime.now.withTimeAtStartOfDay())) {
          mailer.invoicesDueToday()
        }
      }
    }
  }

  def pastDue = {
    Invoice.find(Json.obj("hidden" -> false, "paymentReceived" -> false)).map { invoices =>
      invoices.foreach { invoice =>
        if(invoice.dueDate.isBefore(DateTime.now)) {
          mailer.invoicesPastDue()
        }
      }
    }
  }

}
