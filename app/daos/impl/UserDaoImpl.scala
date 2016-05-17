package daos.impl

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import daos.UserDao
import models.Role.Role
import models.User._
import models.{Profile, User}
import play.api.libs.json._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class UserDaoImpl @Inject() (reactiveMongoApi: ReactiveMongoApi) extends UserDao {

  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  def find(loginInfo:LoginInfo): Future[Option[User]] =
    users.find(Json.obj("profiles.loginInfo" -> loginInfo)).one[User]

  def find(userId:UUID): Future[Option[User]] =
    users.find(Json.obj("id" -> userId)).one[User]

  def find(email: String): Future[Option[User]] =
    users.find(Json.obj("profiles.email" -> email)).one[User]

  def findAll(): Future[List[User]] = users.find(Json.obj()).cursor[User]().collect[List]()

  def save(user:User): Future[User] =
    users.insert(user).map(_ => user)

  def confirm(loginInfo:LoginInfo): Future[User] = for {
    _ <- users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo),
      Json.obj(
        "$set" -> Json.obj("profiles.$.confirmed" -> true)
      ))
    user <- find(loginInfo)
  } yield user.get

  def link(user:User, profile:Profile) = for {
    _ <- users.update(Json.obj(
      "id" -> user.id
    ), Json.obj(
      "$push" -> Json.obj("profiles" -> profile)
    ))
    user <- find(user.id)
  } yield user.get

  def update(profile:Profile) = for {
    _ <- users.update(Json.obj(
      "profiles.loginInfo" -> profile.loginInfo
    ), Json.obj(
      "$set" -> Json.obj("profiles.$" -> profile)
    ))
    user <- find(profile.loginInfo)
  } yield user.get

  def update(user: User, role: Role) = for {
    _ <- users.update(
      Json.obj("id" -> user.id),
      Json.obj("$set" -> Json.obj("role" -> role))
    )
    user <- find(user.id)
  } yield user.get

}
