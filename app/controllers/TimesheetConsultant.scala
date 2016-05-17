package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models._
import modules.UserEnv
import org.joda.time.{DateTime, Interval}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsObject, Json, _}
import play.api.mvc.Controller
import services.UserService
import utils.auth.{WithRole, WithUserEmail}

import scala.concurrent.Future

class TimesheetConsultant @Inject() (val messagesApi: MessagesApi,
  val silhouette: Silhouette[UserEnv],
  userService: UserService)
  extends Controller with I18nSupport {

  def view(userEmail: String, date: String) = silhouette.SecuredAction((WithUserEmail(userEmail) && WithRole(Role.CONSULTANT)) || (WithRole(Role.OWNER) || WithRole(Role.ADMIN))).async { implicit request =>
    // TODO handle malformed dates such as 2015.12.31 or 31-12-2015
    val dateTime = DateTime.parse(date)
    val weekStart = dateTime.dayOfWeek().withMinimumValue()
    val weekEnd = dateTime.dayOfWeek().withMaximumValue()
    var containsNone = true

    for {
      user <- userService.find(userEmail)
      users <- userService.findAll
      projects <- Project.find(Json.obj("team.userId" -> user.get.id))
      projectIds <- Future(projects.map(p => p._id.get.stringify))
      timesheets <- Timesheet.find(Json.obj("projectCode" -> Json.obj("$in" -> projectIds), "consultantId" -> user.get.id, "weekStart" -> DateTime.parse(date).dayOfWeek().withMinimumValue(), "weekEnd" -> DateTime.parse(date).dayOfWeek().withMaximumValue()))
      timesheetIds <- Future(timesheets.map(t => t._id.get.stringify))
      timesheetDetails <- TimesheetDetail.find(Json.obj("timesheetId" -> Json.obj("$in" -> timesheetIds)))
    } yield {
      projects.foreach { project =>
        val dateRange = new Interval(project.startDate.get, project.endDate.get)
        if (dateRange.contains(weekStart) || dateRange.contains(weekEnd)) {
          containsNone = false
        }
      }
      Ok(views.html.timesheets.weekView(request.identity, request.authenticator.loginInfo, dateTime, projects, containsNone, timesheets, timesheetDetails, users))
    }
  }

  def save(projectCode: String, userId: String, date: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN) || WithRole(Role.CONSULTANT)).async { implicit request =>
    request.body.asJson.map {
      case JsObject(fields) =>
        /** TODO abstract following logic to separate method findOrCreateBy in DocumentDao */
        val query = Json.obj("projectCode" -> projectCode, "consultantId" -> userId, "weekStart" -> DateTime.parse(date).dayOfWeek().withMinimumValue(), "weekEnd" -> DateTime.parse(date).dayOfWeek().withMaximumValue())
        Timesheet.findOne(query).map {
          case Some(timesheet) =>
            addTimesheetDetail(fields, timesheet)
          case _ =>
            Timesheet.insert(Timesheet(projectCode, userId, DateTime.parse(date).dayOfWeek().withMinimumValue(), DateTime.parse(date).dayOfWeek().withMaximumValue(), None, None, None, None, None, None)).map {
              case Left(ex) =>
                Future.successful(Ok(Json.obj("success" -> false, "message" -> s"Failed while inserting timesheet. Reason: ${ex.message}")))
              case Right(timesheet) =>
                addTimesheetDetail(fields, timesheet)
            }
        }
        Future.successful(Ok(Json.obj("success" -> true, "message" -> s"Inserted/updated timesheet data.")))
      case _ =>
        Future.successful(BadRequest(Json.obj("success" -> false, "message" -> s"Expecting something days and hours.")))
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("success" -> false, "message" -> s"Expecting JSON")))
    }
  }

  def submit(projectCode: String, userId: String, date: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN) || WithRole(Role.CONSULTANT)).async { implicit request =>
    val timesheet = Timesheet.findOne(Json.obj("projectCode" -> projectCode, "consultantId" -> userId, "weekStart" -> DateTime.parse(date).dayOfWeek().withMinimumValue(), "weekEnd" -> DateTime.parse(date).dayOfWeek().withMaximumValue()))
    timesheet.map {
      case Some(ts) =>
        Timesheet.update(ts._id.get.stringify, ts.copy(status = TimesheetStatus.SUBMITTED, submittedAt = Some(DateTime.now)))
        Ok(Json.obj("success" -> true, "message" -> s"Successfully updated timesheet"))
      case _ =>
        BadRequest(Json.obj("success" -> false, "message" -> s"Cannot find timesheet for the specified details"))
    }
  }

  private[this] def addTimesheetDetail(fields: scala.collection.Map[String, JsValue], timesheet: Timesheet) = {
    fields.foreach { case (dateString, hoursString) =>
      TimesheetDetail.findOne(Json.obj("workDay" -> DateTime.parse(dateString), "timesheetId" -> timesheet._id.get.stringify)).map {
        case Some(tsd) =>
          TimesheetDetail.update(
            tsd._id.get.stringify, TimesheetDetail(DateTime.parse(dateString), hoursString.as[String].toDouble, DateTime.now, timesheet._id.get.stringify)
          )
        case _ =>
          TimesheetDetail.insert(
            TimesheetDetail(DateTime.parse(dateString), hoursString.as[String].toDouble, DateTime.now, timesheet._id.get.stringify)
          )
      }
    }
    // Update totalHours
    Timesheet.update(
      timesheet._id.get.stringify, timesheet.copy(
        totalHours = fields.map { case(dateString, hoursString) => hoursString.as[String].toDouble }.foldLeft(0.0)(_ + _),
        status = TimesheetStatus.SAVED,
        savedAt = Some(DateTime.now)
      )
    )
  }

}
