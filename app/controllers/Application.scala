package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.typesafe.config.Config
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import utils.auth.UserEnv

import scala.concurrent.{ExecutionContext, Future}

/** As we are creating the actor and storing a reference to it, if the controller was not scoped as
  * singleton, this would mean a new actor would be created every time the controller was created */
@Singleton
class Application @Inject() (val messagesApi: MessagesApi,
  val env: Environment[UserEnv],
  val silhouette: Silhouette[UserEnv]
  )(implicit ec: ExecutionContext, config: Config) // for actor system
  extends Controller with I18nSupport {

  def index = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity, request.authenticator.map(_.loginInfo))))
  }

  def profile = silhouette.SecuredAction { implicit request =>
    Ok(views.html.profile(request.identity, request.authenticator.loginInfo))
  }
}
