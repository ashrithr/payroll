package tasks

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Scheduler @Inject() (
  val system: ActorSystem,
  @Named("invoice-due-notifier") val invoiceDueNotifierActor: ActorRef
  )(implicit ec: ExecutionContext) {

  /**
    * Invoice notifications for today and past due
    */
  system.scheduler.schedule(0.seconds, 1.day, invoiceDueNotifierActor, InvoiceDueNotifier.DueToday)
  system.scheduler.schedule(0.seconds, 7.days, invoiceDueNotifierActor, InvoiceDueNotifier.PastDue)

}
