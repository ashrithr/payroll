package daos

import java.util.UUID

import models.UserToken

import scala.concurrent.Future

trait UserTokenDao {

  def find(id:UUID): Future[Option[UserToken]]
  def save(token:UserToken):Future[UserToken]
  def remove(id:UUID):Future[Unit]

}
