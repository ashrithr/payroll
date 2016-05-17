package tasks

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Scheduler @Inject() (
  val system: ActorSystem,
  @Named("invoice-due-notifier") val invoiceDueNotifierActor: ActorRef
  )(implicit ec: ExecutionContext) {

  //system.scheduler.schedule(0.microseconds, 10.seconds, invoiceDueNotifierActor, InvoiceDueNotifier.DueToday)

}
