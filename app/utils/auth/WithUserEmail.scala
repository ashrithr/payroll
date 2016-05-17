package utils.auth

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.mvc.Request

import scala.concurrent.Future


case class WithUserEmail(session: String) extends Authorization[User, CookieAuthenticator] {

  override def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request: Request[B]): Future[Boolean] = {
    Future.successful(user.profiles.map(_.email).head.get == session)
  }

}
