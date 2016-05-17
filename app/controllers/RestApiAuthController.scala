package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.{Environment, LoginEvent, Silhouette}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.Token
import modules.UserEnv
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import services.UserService
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class RestApiAuthController @Inject() (
  val messagesApi: MessagesApi,
  val env: Environment[UserEnv],
  val silhouette: Silhouette[UserEnv],
  credentialsProvider: CredentialsProvider,
  userService: UserService)
  extends Controller with I18nSupport {

  implicit val restFormat = (
    (__ \ "identifier").format[String] ~
    (__ \ "password").format[String])(Credentials.apply, unlift(Credentials.unapply)
  )

  def authenticate = Action.async(parse.json[Credentials]) { implicit request =>
    credentialsProvider.authenticate(request.body).flatMap { loginInfo =>
      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          env.authenticatorService.create(loginInfo).flatMap { authenticator =>
            env.eventBus.publish(LoginEvent(user, request))
            env.authenticatorService.init(authenticator).flatMap { token =>
              env.authenticatorService.embed(token,
                Ok(Json.toJson(Token(token.value, authenticator.expirationDateTime)))
              )
            }
          }
        case None =>
          Future.failed(new IdentityNotFoundException("Couldn't find user"))
      }
    }
  }


}
