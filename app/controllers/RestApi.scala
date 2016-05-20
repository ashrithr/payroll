package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.typesafe.config.Config
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json._
import models.User._
import play.api.mvc.{Controller, RequestHeader}
import utils.auth.UserEnv

import scala.concurrent.Future

class RestApi @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[UserEnv],
  val config: Config)
  extends Controller with I18nSupport {

  val errorHandler = new SecuredErrorHandler {
    override def onNotAuthenticated(implicit request: RequestHeader) = {
      Future.successful(Unauthorized(Json.obj("error" -> Messages("error.profileUnauthenticated"))))
    }

    override def onNotAuthorized(implicit request: RequestHeader) = {
      Future.successful(Forbidden(Json.obj("error" -> Messages("error.profileUnauthorized"))))
    }
  }

  def profile = silhouette.SecuredAction.async { implicit request =>
    val json = Json.toJson(request.identity.profileFor(request.authenticator.loginInfo).get)
    val prunedJson = json.transform(
      (__ \ 'loginInfo).json.prune andThen
      (__ \ 'passordInfo).json.prune andThen
      (__ \ 'oauth1Info).json.prune)
    prunedJson.fold(
      _ => Future.successful(InternalServerError(Json.obj("error" -> Messages("error.profileError")))),
      js => Future.successful(Ok(js))
    )
  }

}
