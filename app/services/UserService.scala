package services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import daos.UserDao
import models.Role.Role
import models.{Profile, User}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class UserService @Inject() (userDao: UserDao) extends IdentityService[User] {

  def retrieve(loginInfo:LoginInfo):Future[Option[User]] = userDao.find(loginInfo)

  def save(user:User) = userDao.save(user)

  def find(id:UUID) = userDao.find(id)

  def find(email:String) = userDao.find(email)

  def findAll = userDao.findAll()

  def updateRole(id: UUID, role: Role) = {
    userDao.find(id) flatMap {
      case Some(user) =>
        userDao.update(user, role)
        Future.successful(user)
    }
  }

  def confirm(loginInfo:LoginInfo) = userDao.confirm(loginInfo)

  def link(user:User, socialProfile:CommonSocialProfile) = {
    val profile = toProfile(socialProfile)
    if (user.profiles.exists(_.loginInfo == profile.loginInfo)) Future.successful(user) else userDao.link(user, profile)
  }

  def save(socialProfile:CommonSocialProfile) = {
    val profile = toProfile(socialProfile)
    userDao.find(profile.loginInfo).flatMap {
      case None => userDao.save(User(UUID.randomUUID(), List(profile), models.Role.SIMPLE_USER))
      case Some(user) => userDao.update(profile)
    }
  }

  private def toProfile(p:CommonSocialProfile) = Profile(
    loginInfo = p.loginInfo,
    confirmed = true,
    email = p.email,
    firstName = p.firstName,
    lastName = p.lastName,
    fullName = p.fullName,
    passwordInfo = None,
    avatarUrl = p.avatarURL
  )

}
