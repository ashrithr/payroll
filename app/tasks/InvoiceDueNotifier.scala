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

class InvoiceDueNotifier @Inject()(val messagesApi: MessagesApi, mailer: Mailer, userService: UserService) extends Actor with I18nSupport {

  import InvoiceDueNotifier._

  override def receive: Receive = {
    case DueToday => dueToday
    case PastDue => pastDue
  }

  def dueToday = {
    val admins = userService.findAll.map(_.filter(_.role == Role.ADMIN)).map(_.flatMap(_.profiles.flatMap(_.email)))
    Invoice.find(Json.obj("hidden" -> false, "paymentReceived" -> false)).map { invoices =>
      invoices.foreach { invoice =>
        if(invoice.dueDate.withTimeAtStartOfDay().isEqual(DateTime.now.withTimeAtStartOfDay())) {
          admins.map { a =>
            mailer.invoicesDueToday(a)
          }
        }
      }
    }
  }

  def pastDue = {
    val admins = userService.findAll.map(_.filter(_.role == Role.ADMIN)).map(_.flatMap(_.profiles.flatMap(_.email)))
    Invoice.find(Json.obj("hidden" -> false, "paymentReceived" -> false)).map { invoices =>
      invoices.foreach { invoice =>
        if(invoice.dueDate.isBefore(DateTime.now)) {
          admins.map { a =>
            mailer.invoicesPastDue(a)
          }
        }
      }
    }
  }

}
