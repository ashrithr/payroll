package daos.core

import daos.exceptions.ServiceException
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import reactivemongo.api.gridfs.{FileToSave, GridFS, ReadFile}
import reactivemongo.bson.BSONValue
import reactivemongo.play.json.JSONSerializationPack

import scala.concurrent.Future

trait FileDao extends BaseDao {

  /*lazy val gfs = GridFS[JSONSerializationPack.type](db, collectionName)

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsValue]
  type JSONFileToSave = FileToSave[JSONSerializationPack.type, JsValue]

  def insert(enumerator: Enumerator[Array[Byte]], file: JSONFileToSave): Future[JSONReadFile] = {
    gfs.save(enumerator, file)
  }

  def find(query: JsObject = Json.obj()) = {
    Logger.debug(s"Finding files: [collection=$collectionName, query=$query]")
    gfs.find[JsObject, JSONReadFile](query)
  }

  def findById(id: String): Future[Option[JSONReadFile]] = find(DBQueryBuilder.id(id)).headOption

  def findOne(query: JsObject = Json.obj()): Future[Option[JSONReadFile]] = {
    Logger.debug(s"Finding one file: [collection=$collectionName, query=$query]")
    gfs.find(query).headOption
  }

  def removeById(id: String): Future[Either[ServiceException, Boolean]] = {
    recover(gfs.remove(Json toJson id)) {
      true
    }
  }

  def enumerate(file: ReadFile[JSONSerializationPack.type ,_ <: BSONValue]): Enumerator[Array[Byte]] = {
    gfs.enumerate(file)
  }

  override def ensureIndexes = {
    // Let's build an index on our gridfs chunks collection if none:
    gfs.ensureIndex.map {
      case status =>
        Logger.info(s"GridFS index: [collection=$collectionName, status=$status]")
        List(status)
    }
  }*/

}
