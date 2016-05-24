package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.typesafe.config.Config
import daos.UserDao
import forms.UserRoleUpdate
import models.Role
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import services.UserService
import utils.auth.{UserEnv, WithRole}

class UserController @Inject() (val messagesApi: MessagesApi,
  val silhouette: Silhouette[UserEnv],
  val userDao: UserDao,
  val userService: UserService)(implicit config: Config)
  extends Controller with I18nSupport {

  private val logger = Logger(this.getClass)

  def users = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    userService.findAll.map { users =>
      Ok(views.html.users(request.identity, request.authenticator.loginInfo, users))
    }
  }

  def getUser(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)).async { implicit request =>
    userService.find(UUID.fromString(id)).map { user =>
      if(request.identity.role == Role.OWNER) {
        Ok(views.html.user(request.identity, request.authenticator.loginInfo, user, UserRoleUpdate.form, Role.values.toSeq))
      } else {
        if(user.get.role == Role.OWNER) { // admin cannot edit owner account
          Forbidden("Not authorized to view owner account")
        } else {
          Ok(views.html.user(request.identity, request.authenticator.loginInfo, user, UserRoleUpdate.form, Role.values.toSeq diff Seq(Role.withName("owner"), Role.withName("admin"))))
        }
      }
    }
  }

  def updateUserRole(id: String) = silhouette.SecuredAction(WithRole(Role.OWNER) || WithRole(Role.ADMIN)) { implicit request =>
    UserRoleUpdate.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest
      },
      roleData => {
        logger.debug(s"Updating user with id $id to ${roleData.role}")
        userService.updateRole(UUID.fromString(id), Role.withName(roleData.role))
        Redirect(routes.UserController.getUser(id))
      }
    )
  }

  // TODO
  def deleteUser(id: String) = ???

}
