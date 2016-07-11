package controllers

import java.io.ByteArrayInputStream
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.typesafe.config.{Config, ConfigFactory}
import forms.{InvoiceForm, InvoicePaymentForm}
import models._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Controller
import services.UserService
import utils.PdfGenerator
import utils.auth.{UserEnv, WithRole}
import utils.DateTimeUtils._
import net.ceedubs.ficus.Ficus._
import play.api.Logger

import scala.concurrent.Future

class InvoiceController @Inject() (val messagesApi: MessagesApi,
  val silhouette: Silhouette[UserEnv],
  val userService: UserService)(implicit config: Config)
  extends Controller with I18nSupport {

  private val logger = Logger(this.getClass)

  def index() = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      // sort invoices by their created timestamp
      invoices <- Invoice.find(Json.obj("hidden" -> false)).map(_.sortBy(_.created))
      consultants <- userService.findAll.map(_.filter(_.role == Role.CONSULTANT))
    } yield {
      Ok(views.html.invoices.invoices(request.identity, request.authenticator.loginInfo, invoices, consultants))
    }
  }

  def createInvoiceForm() = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      projects <- Project.find()
      consultants <- userService.findAll.map(_.filter(_.role == Role.CONSULTANT))
    } yield Ok(views.html.invoices.createInvoice(request.identity, request.authenticator.loginInfo, InvoiceForm.form, projects, consultants))
  }

  def createInvoice() = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    InvoiceForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.InvoiceController.createInvoiceForm()).flashing("error" -> "Errors in form"))
      },
      i => {
        val startDate = DateTime.parse(i.startDate, DateTimeFormat.forPattern("MM/dd/yyyy"))
        val endDate = DateTime.parse(i.endDate, DateTimeFormat.forPattern("MM/dd/yyyy"))
        val currentDate = DateTime.now
        for {
          project <- Project.findById(i.project)
          timesheets <- Timesheet.find(
            Json.obj(
              "projectCode" -> project.get._id.get.stringify,
              "consultantId" -> i.consultant,
              "weekStart" -> Json.obj("$gte" -> startDate),
              "weekEnd" -> Json.obj("$lte" -> endDate)
            )
          )
        } yield {
          val priceOfConsultant = project.get.team.getOrElse(List()).find(c => c.userId == i.consultant).get.rate
          val totalHours = timesheets.map(t => t.totalHours).sum
          val invoice = Invoice(
            startDate,
            endDate,
            project.get.vendorId,
            project.get.clientId,
            i.consultant,
            i.project,
            totalHours,
            project.get.paymentNet,
            currentDate.plusDays(project.get.paymentNet.toString.toInt),
            priceOfConsultant.toDouble,
            priceOfConsultant.toDouble * totalHours.toDouble
          )
          // create invoice
          Invoice.insert(invoice)
          // mark all timesheets as invoiced
          timesheets.map { ts =>
            Timesheet.update(
              ts._id.get.stringify,
              ts.copy(
                invoiced = true
              )
            )
          }
          Redirect(routes.InvoiceController.index()).flashing("success" -> "Invoice saved!")
        }
      }
    )
  }

  def hideInvoice(invoiceId: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      invoice <- Invoice.findById(invoiceId)
    } yield {
      Invoice.update(
        invoiceId,
        invoice.get.copy(
          hidden = true
        )
      )
      Redirect(routes.InvoiceController.index()).flashing("success" -> "Successfully updated timesheet to be hidden")
    }
  }

  def generateInvoice(invoiceId: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      invoice <- Invoice.findById(invoiceId)
      vendor <- Vendor.findById(invoice.get.vendorId)
      client <- Client.findById(invoice.get.clientId)
      consultant <- userService.find(UUID.fromString(invoice.get.consultantId))
    } yield {
      val content = views.html.pdfs.invoice.render(invoice.get, consultant.get, vendor.get, client.get, config).body.getBytes()
      val outputStream = PdfGenerator.generate(new ByteArrayInputStream(content), config)

      Ok(outputStream.toByteArray).as("application/pdf").withHeaders("Content-Disposition" -> "filename=test.pdf")
    }
  }

  def invoicesDue = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      // reverse sort invoices based on due date
      invoices <- Invoice.find(Json.obj("paymentReceived" -> false, "hidden" -> false)).map(_.sortBy(_.dueDate)(Ordering.fromLessThan(_ isBefore _)))
      consultants <- userService.findAll.map(_.filter(_.role == Role.CONSULTANT))
    } yield {
      Ok(views.html.invoices.invoicesDue(request.identity, request.authenticator.loginInfo, invoices, consultants, InvoicePaymentForm.form))
    }
  }

  def paymentReceived(invoiceId: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    InvoicePaymentForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.InvoiceController.invoicesDue()).flashing("error" -> "Errors in form"))
      },
      pd => {
        val paymentAmount = pd.paymentReceivedAmount
        for {
          invoice <- Invoice.findById(invoiceId)
        } yield {
          Invoice.update(
            invoiceId,
            invoice.get.copy(
              paymentReceivedDate = Some(DateTime.now),
              paymentReceivedAmount = Some(paymentAmount.toDouble),
              paymentReceived = true
            )
          )
          Redirect(routes.InvoiceController.invoicesDue()).flashing("success" -> "Successfully updated timesheet attribute paymentReceived")
        }
      }
    )
  }

  def accounting = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      invoices <- Invoice.find(Json.obj("hidden" -> false))
      consultants <- userService.findAll.map(_.filter(_.role == Role.CONSULTANT))
      projects <- Project.find()
    } yield {
      Ok(views.html.invoices.accounting(request.identity, request.authenticator.loginInfo, invoices, consultants, projects))
    }
  }

  def accountingConsultant(cId: String, pId: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      consultant <- userService.find(UUID.fromString(cId))
      project <- Project.findById(pId)
      vendor <- Vendor.findById(project.get.vendorId)
      invoices <- Invoice.find(Json.obj("projectId" -> pId, "consultantId" -> cId))
    } yield {
      Ok(views.html.invoices.accountingConsultant(request.identity, request.authenticator.loginInfo, invoices, consultant, project, vendor))
    }
  }

}
