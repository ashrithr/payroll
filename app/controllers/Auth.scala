package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import forms.{PasswordRecovery, SignInForm, SignUpForm}
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import models.{Profile, Role, User, UserToken}
import modules.UserEnv
import services.{UserService, UserTokenService}
import utils.Mailer
import org.joda.time.DateTime

class Auth @Inject() (
  val messagesApi: MessagesApi,
  val env: Environment[UserEnv],
  val silhouette: Silhouette[UserEnv],
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  userService: UserService,
  userTokenService: UserTokenService,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  configuration: Configuration,
  mailer: Mailer) extends Controller with I18nSupport {

  def startSignUp = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => Redirect(routes.Application.index())
      case None => Ok(views.html.auth.startSignUp(SignUpForm.signUpForm))
    })
  }

  def handleStartSignUp = Action.async { implicit request =>
    SignUpForm.signUpForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.startSignUp(bogusForm))),
      signUpData => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(_) =>
            Future.successful(Redirect(routes.Auth.startSignUp()).flashing(
              "error" -> Messages("error.userExists", signUpData.email)))
          case None =>
            val profile = Profile(
              loginInfo = loginInfo,
              confirmed = false,
              email = Some(signUpData.email),
              firstName = Some(signUpData.firstName),
              lastName = Some(signUpData.lastName),
              fullName = Some(s"${signUpData.firstName} ${signUpData.lastName}"),
              passwordInfo = None,
              avatarUrl = None)
            for {
              avatarUrl <- avatarService.retrieveURL(signUpData.email)
              user <- userService.save(User(id = UUID.randomUUID(), profiles = List(profile.copy(avatarUrl = avatarUrl)), role = models.Role.SIMPLE_USER))
              _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(signUpData.password))
              token <- userTokenService.save(UserToken.create(user.id, signUpData.email, isSignUp = true))
            } yield {
              mailer.welcome(profile, link = routes.Auth.signUp(token.id.toString).absoluteURL())
              Ok(views.html.auth.finishSignUp(profile))
            }
        }
      }
    )
  }

  def signUp(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None =>
        Future.successful(NotFound(views.html.errors.notFound()))
      case Some(token) if token.isSignUp && !token.isExpired =>
        userService.find(token.userId).flatMap {
          case None => Future.failed(new IdentityNotFoundException(Messages("error.noUser")))
          case Some(user) =>
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              admins <- userService.findAll.map(_.filter(_.role == Role.ADMIN)).map(_.flatMap(_.profiles.flatMap(_.email)))
              _ <- userService.confirm(loginInfo)
              _ <- userTokenService.remove(id)
              result <- env.authenticatorService.embed(value, Redirect(routes.Application.index()))
            } yield {
              mailer.newUserSignUp(token.email, admins)
              result
            }
        }
      case Some(token) =>
        userTokenService.remove(id).map {_ => NotFound(views.html.errors.notFound())}
    }
  }

  def signIn = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => Redirect(routes.Application.index())
      case None => Ok(views.html.auth.signIn(SignInForm.signInForm))
    })
  }

  def authenticate = Action.async { implicit request =>
    SignInForm.signInForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.signIn(bogusForm))),
      signInData => {
        val credentials = Credentials(signInData.email, signInData.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          userService.retrieve(loginInfo).flatMap {
            case None =>
              Future.successful(Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.noUser")))
            case Some(user) if !user.profileFor(loginInfo).map(_.confirmed).getOrElse(false) =>
              Future.successful(Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.unregistered", signInData.email)))
            case Some(_) => for {
              authenticator <- env.authenticatorService.create(loginInfo).map {
                case authenticator if signInData.rememberMe =>
                  val c = configuration.underlying
                  authenticator.copy(
                    expirationDateTime = new DateTime() + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator => authenticator
              }
              value <- env.authenticatorService.init(authenticator)
              result <- env.authenticatorService.embed(value, Redirect(routes.Application.index()))
            } yield result
          }
        }.recover {
          case e:ProviderException => Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.invalidCredentials"))
        }
      }
    )
  }

  def signOut = silhouette.SecuredAction.async { implicit request =>
    env.authenticatorService.discard(request.authenticator, Redirect(routes.Application.index()))
  }

  def startResetPassword = Action { implicit request =>
    Ok(views.html.auth.startResetPassword(PasswordRecovery.emailForm))
  }

  def handleStartResetPassword = Action.async { implicit request =>
    PasswordRecovery.emailForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.startResetPassword(bogusForm))),
      email => userService.retrieve(LoginInfo(CredentialsProvider.ID, email)).flatMap {
        case None => Future.successful(Redirect(routes.Auth.startResetPassword()).flashing("error" -> Messages("error.noUser")))
        case Some(user) => for {
          token <- userTokenService.save(UserToken.create(user.id, email, isSignUp = false))
        } yield {
          mailer.resetPassword(email, link = routes.Auth.resetPassword(token.id.toString).absoluteURL())
          Ok(views.html.auth.resetPasswordInstructions(email))
        }
      }
    )
  }

  def resetPassword(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None =>
        Future.successful(NotFound(views.html.errors.notFound()))
      case Some(token) if !token.isSignUp && !token.isExpired =>
        Future.successful(Ok(views.html.auth.resetPassword(tokenId, PasswordRecovery.resetPasswordForm)))
      case _ => for {
        _ <- userTokenService.remove(id)
      } yield NotFound(views.html.errors.notFound())
    }
  }

  def handleResetPassword(tokenId:String) = Action.async { implicit request =>
    PasswordRecovery.resetPasswordForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.auth.resetPasswordInstructions(tokenId))),
      passwords => {
        val id = UUID.fromString(tokenId)
        userTokenService.find(id).flatMap {
          case None =>
            Future.successful(NotFound(views.html.errors.notFound()))
          case Some(token) if !token.isSignUp && !token.isExpired =>
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              _ <- authInfoRepository.save(loginInfo, passwordHasher.hash(passwords._1))
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              _ <- userTokenService.remove(id)
              result <- env.authenticatorService.embed(value, Ok(views.html.auth.resetPasswordDone()))
            } yield result
        }
      }
    )
  }
}
