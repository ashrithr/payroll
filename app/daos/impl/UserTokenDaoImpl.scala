package daos.impl

import java.util.UUID
import javax.inject.Inject

import daos.UserTokenDao
import models.UserToken
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import play.modules.reactivemongo.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class UserTokenDaoImpl @Inject() (reactiveMongoApi: ReactiveMongoApi) extends UserTokenDao {

  val tokens = reactiveMongoApi.db.collection[JSONCollection]("tokens")

  def find(id:UUID):Future[Option[UserToken]] =
    tokens.find(Json.obj("id" -> id)).one[UserToken]

  def save(token:UserToken):Future[UserToken] = for {
    _ <- tokens.insert(token)
  } yield token

  def remove(id:UUID):Future[Unit] = for {
    _ <- tokens.remove(Json.obj("id" -> id))
  } yield ()

}
