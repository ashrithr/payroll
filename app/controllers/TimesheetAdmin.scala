package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.typesafe.config.Config
import forms.{ClientForm, ContactForm, ProjectForm, VendorForm}
import models._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc.Controller
import services.UserService
import utils.{EnumReflector, Mailer}
import utils.auth.{UserEnv, WithRole}

import scala.concurrent.Future


class TimesheetAdmin @Inject() (val messagesApi: MessagesApi,
  val silhouette: Silhouette[UserEnv],
  userService: UserService,
  mailer: Mailer)(implicit config: Config)
  extends Controller with I18nSupport {

  private val logger = Logger(this.getClass)

  def index = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      contacts <- Contact.find()
      clients <- Client.find()
      vendors <- Vendor.find()
    } yield
      Ok(views.html.timesheets.admin(request.identity, request.authenticator.loginInfo, ContactForm.form, ClientForm.form, VendorForm.form, contacts, clients, vendors))
  }

  def createContact = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    ContactForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest),
      c => {
        val contact = Contact(c.firstName, c.lastName, c.email, c.title, c.officeNum, c.mobileNum, c.faxNum)
        logger.debug(s"Creating new contact: $contact")
        Contact.insert(contact)
        Future.successful(
          Redirect(routes.TimesheetAdmin.index() + "#contacts").flashing("success" -> "Contact saved!")
        )
      }
    )
  }

  def createClient = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    ClientForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.index() + "#clients").flashing("error" -> "At least one contact needed to save the client."))
      },
      c => {
        val client = Client(c.companyName, c.companyAddress, c.primaryContacts, c.secondaryContacts)
        logger.debug(s"Creating new client: $client")
        Client.insert(client)
        Future.successful(Redirect(routes.TimesheetAdmin.index() + "#clients").flashing("success" -> "Client saved!"))
      }
    )
  }


  def createVendor = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    VendorForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.index() + "#vendors").flashing("error" -> "At least one contact needed to save the vendor."))
      },
      c => {
        val vendor = Vendor(c.companyName, c.companyAddress, c.primaryContacts, c.secondaryContacts)
        logger.debug(s"Creating new vendor: $vendor")
        Vendor.insert(vendor)
        Future.successful(Redirect(routes.TimesheetAdmin.index() + "#vendors").flashing("success" -> "Vendor saved!"))
      }
    )
  }

  def projects = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    Project.find().map { projects =>
      Ok(views.html.timesheets.projects(request.identity, request.authenticator.loginInfo, projects))
    }
  }

  def createProjectForm = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      vendors <- Vendor.find()
      clients <- Client.find()
      consultants <- userService.findAll.map(_.filter(_.role == Role.CONSULTANT))
    } yield Ok(views.html.timesheets.createProject(request.identity, request.authenticator.loginInfo, ProjectForm.form, vendors, clients, consultants))
  }

  def createProject = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    ProjectForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.createProjectForm()).flashing("error" -> "Errors in form"))
      },
      p => {
        val project = Project(
          p.projectName,
          UUID.randomUUID(),
          p.startDate.map(s => DateTime.parse(s, DateTimeFormat.forPattern("MM/dd/yyyy"))),
          p.endDate.map(e => DateTime.parse(e, DateTimeFormat.forPattern("MM/dd/yyyy"))),
          p.preferredCurrency,
          p.biWeekly,
          !p.biWeekly,
          p.notes,
          p.vendorId,
          p.clientId,
          EnumReflector.withName[PaymentNet.Net](p.paymentNet),
          None //p.team
        )
        logger.debug(s"Creating new project: $project")
        Project.insert(project)
        Future.successful(Redirect(routes.TimesheetAdmin.projects()).flashing("success" -> "Project saved!"))
      }
    )
  }

  def getProject(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      project <- Project.findById(id)
      vendor <- Vendor.findById(project.get.vendorId)
      client <- Client.findById(project.get.clientId)
      users <- userService.findAll
    } yield Ok(views.html.timesheets.project(request.identity, request.authenticator.loginInfo, project, vendor, client, users))
  }

  def addConsultantToProject(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    val params = request.body.asJson
    params.map(d => d \ "team") match {
      case Some(JsDefined(team)) =>
        Project.push(id, Json.obj("team" -> Json.obj("userId" -> (team \ "consultant").as[String], "rate" -> (team \ "cost").as[String]))).map {
          case Left(ex) => BadRequest(Json.obj("success" -> false, "message" -> s"Failed to update : ${ex.message}"))
          case Right(c) => Ok(Json.obj("success" -> true, "message" -> s"Added consultant."))
        }
      case _ => Future.successful(BadRequest(Json.obj("success" -> false, "message" -> "Consultant not selected")))
    }
  }

  def removeConsultantFromProject(id: String, cId: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    Project.pull(id, Json.obj("team" -> Json.obj("userId" -> cId))).map {
      case Left(ex) => BadRequest(Json.obj("success" -> false, "message" -> s"Failed to update : ${ex.message}"))
      case Right(c) => Ok(Json.obj("success" -> true, "message" -> s"Deleted consultant."))
    }
  }

  def editProjectForm(id: String) = silhouette.SecuredAction(WithRole(Role.ADMIN) || WithRole(Role.OWNER)).async { implicit request =>
    for {
      vendors <- Vendor.find()
      clients <- Client.find()
      consultants <- userService.findAll.map(_.filter(_.role == Role.CONSULTANT))
      project <- Project.findById(id)
    } yield Ok(views.html.timesheets.editProject(request.identity, request.authenticator.loginInfo, ProjectForm.form, project.get, vendors, clients, consultants))
  }

  def editProject(id: String) = silhouette.SecuredAction(WithRole(Role.ADMIN) || WithRole(Role.OWNER)).async { implicit request =>
    ProjectForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.editProjectForm(id)).flashing("error" -> "Errors in form"))
      },
      p => {
        Project.findById(id).map { proj =>
          Project.update(
            id,
            proj.get.copy(
              projectName = p.projectName,
              startDate = p.startDate.map(s => DateTime.parse(s, DateTimeFormat.forPattern("MM/dd/yyyy"))),
              endDate = p.endDate.map(e => DateTime.parse(e, DateTimeFormat.forPattern("MM/dd/yyyy"))),
              preferredCurrency = p.preferredCurrency,
              biWeekly = p.biWeekly,
              monthly = !p.biWeekly,
              notes = p.notes,
              vendorId = p.vendorId,
              clientId = p.clientId,
              paymentNet = EnumReflector.withName[PaymentNet.Net](p.paymentNet)
            )
          )
          Redirect(routes.TimesheetAdmin.projects()).flashing("success" -> "Project updated!")
        }
      }
    )
  }

  def editContactForm(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      contact <- Contact.findById(id)
    } yield Ok(views.html.timesheets.editContact(request.identity, request.authenticator.loginInfo, ContactForm.form, contact.get))
  }

  def editContact(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    ContactForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.editContactForm(id)).flashing("error" -> "Errors in the form"))
      },
      c => {
        Contact.findById(id).map { contact =>
          Contact.update(
            id,
            contact.get.copy(
              firstName = c.firstName,
              lastName = c.lastName,
              email = c.email,
              title = c.title,
              officeNum = c.officeNum,
              mobileNum = c.mobileNum,
              faxNum = c.faxNum
            )
          )
          Redirect(routes.TimesheetAdmin.index() + "#contacts").flashing("success" -> "Contact updated!")
        }
      }
    )
  }

  def editClientForm(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      client <- Client.findById(id)
      contacts <- Contact.find()
    } yield Ok(views.html.timesheets.editClient(request.identity, request.authenticator.loginInfo, ClientForm.form, client.get, contacts))
  }

  def editClient(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    ClientForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.editClientForm(id)).flashing("error" -> "Errors in the form"))
      },
      c => {
        Client.findById(id).map { client =>
          Client.update(
            id,
            client.get.copy(
              companyAddress = c.companyAddress,
              companyName = c.companyName,
              primaryContacts = c.primaryContacts,
              secondaryContacts = c.secondaryContacts
            )
          )
          Redirect(routes.TimesheetAdmin.index() + "#clients").flashing("success" -> "Client updated!")
        }
      }
    )
  }

  def editVendorForm(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    for {
      vendor <- Vendor.findById(id)
      contacts <- Contact.find()
    } yield Ok(views.html.timesheets.editVendor(request.identity, request.authenticator.loginInfo, VendorForm.form, vendor.get, contacts))
  }

  def editVendor(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    VendorForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect(routes.TimesheetAdmin.editVendorForm(id)).flashing("error" -> "Errors in the form"))
      },
      v => {
        Vendor.findById(id).map { vendor =>
          Vendor.update(
            id,
            vendor.get.copy(
              companyAddress = v.companyAddress,
              companyName = v.companyName,
              primaryContacts = v.primaryContacts,
              secondaryContacts = v.secondaryContacts
            )
          )
          Redirect(routes.TimesheetAdmin.index() + "#vendors").flashing("success" -> "Vendor updated!")
        }
      }
    )
  }

  def pendingTimesheets = silhouette.SecuredAction(WithRole(Role.ADMIN) || WithRole(Role.OWNER)).async { implicit request =>
    for {
      timesheets <- Timesheet.find(Json.obj("status" -> Json.obj("$in" -> List(TimesheetStatus.SUBMITTED, TimesheetStatus.RE_SUBMITTED))))
      consultants <- userService.findAll
    } yield Ok(views.html.timesheets.pendingTimesheets(request.identity, request.authenticator.loginInfo, timesheets, consultants))
  }

  def approveTimesheet(id: String) = silhouette.SecuredAction(WithRole(Role.ADMIN) || WithRole(Role.OWNER)).async { implicit request =>
    val timesheet = Timesheet.findById(id)
    timesheet.map { ts =>
      Timesheet.update(
        id,
        ts.get.copy(
          status = TimesheetStatus.APPROVED,
          approvedAt = Some(DateTime.now),
          approvedBy = Some(request.identity.id.toString)
        )
      )
      for {
        consultant <- userService.find(ts.get.consultantId)
      } yield {
        // todo reformat
        mailer.timesheetApproval(consultant.get.profiles.head.email.get, request.identity.profiles.head.email.get, ts.get)
      }
      // TODO flash with ajax call does not seem to work, replace ajax call with simple form post request
      Redirect(routes.TimesheetAdmin.index()).flashing("success" -> "Successfully approved timesheet!")
    }
  }

  def denyTimesheet(id: String) = silhouette.SecuredAction(WithRole(Role.ADMIN) || WithRole(Role.OWNER)).async { implicit request =>
    val timesheet = Timesheet.findById(id)
    timesheet.map { ts =>
      Timesheet.update(
        id,
        ts.get.copy(
          status = TimesheetStatus.DENIED,
          disapprovedAt = Some(DateTime.now),
          disapprovedBy = Some(request.identity.id.toString)
        )
      )
      for {
        consultant <- userService.find(ts.get.consultantId)
      } yield {
        // todo reformat
        mailer.timesheetDenial(consultant.get.profiles.head.email.get, request.identity.profiles.head.email.get, ts.get)
      }
      // TODO flash with ajax call does not seem to work, replace ajax call with simple form post request
      Redirect(routes.TimesheetAdmin.pendingTimesheets()).flashing("success" -> "Successfully denied timesheet!")
    }
  }

}
