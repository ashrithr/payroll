package utils.auth

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.User

trait ApiEnv {
  type I = User
  type A = JWTAuthenticator
}
