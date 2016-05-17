package daos.core

import daos.exceptions.ServiceException
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.collection.JSONCollection
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference

import scala.concurrent.Future

/**
  * DAO for Mongo Documents
  */
trait DocumentDao[T <: TemporalModel] extends BaseDao {

  lazy val collection = db.collection[JSONCollection](collectionName)

  def insert(document: T)(implicit writer: OWrites[T]): Future[Either[ServiceException, T]] = {
    document._id = Some(BSONObjectID.generate)
    document.created = Some(DateTime.now)
    document.updated = Some(DateTime.now)
    Logger.debug(s"Inserting document: [collection=$collectionName, data=$document]")
    recover(collection.insert(document)) {
      document
    }
  }

  def find(query: JsObject = Json.obj())(implicit reader: Reads[T]): Future[List[T]] = {
    Logger.debug(s"Finding documents: [collection=$collectionName, query=$query]")
    collection.find(query).cursor[T](ReadPreference.nearest).collect[List]()
  }

  def findOne(query: JsObject)(implicit reader: Reads[T]): Future[Option[T]] = {
    Logger.debug(s"Finding one: [collection=$collectionName, query=$query]")
    collection.find(query).one[T]
  }

  def findById(id: String)(implicit reader: Reads[T]): Future[Option[T]] = findOne(DBQueryBuilder.id(id))

  def findById(id: BSONObjectID)(implicit reader: Reads[T]): Future[Option[T]] = findOne(DBQueryBuilder.id(id))

  def update(id: String, document: T)(implicit writer: OWrites[T]): Future[Either[ServiceException, T]] = {
    document.updated = Some(new DateTime())
    Logger.debug(s"Updating document: [collection=$collectionName, id=$id, document=$document]")
    recover(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.set(document))) {
      document
    }
  }

  def update(id: String, query: JsObject): Future[Either[ServiceException, JsObject]] = {
    val data = updated(query)
    Logger.debug(s"Updating by query: [collection=$collectionName, id=$id, query=$data]")
    recover(collection.update(DBQueryBuilder.id(id), data)) {
      data
    }
  }

  def push(id: String, data: JsObject): Future[Either[ServiceException, JsObject]] = {
    Logger.debug(s"pushing to document: [collection=$collectionName, id=$id, data=$data]")
    recover(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.push(data))) {
      data
    }
  }

  def push[S](id: String, field: String, data: S)(implicit writer: OWrites[S]): Future[Either[ServiceException, S]] = {
    Logger.debug(s"Pushing to document: [collection=$collectionName, id=$id, field=$field data=$data]")
    recover(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.push(field, data)
    )) {
      data
    }
  }

  def pull(id: String, data: JsObject): Future[Either[ServiceException, JsObject]] = {
    Logger.debug(s"pulling from document: [collection=$collectionName, id=$id, data=$data]")
    recover(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.pull(data))) {
      data
    }
  }

  def pull[S](id: String, field: String, query: S)(implicit writer: OWrites[S]): Future[Either[ServiceException, Boolean]] = {
    Logger.debug(s"Pulling from document: [collection=$collectionName, id=$id, field=$field query=$query]")
    recover(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.pull(field, query))) {
      true
    }
  }

  def unset(id: String, field: String): Future[Either[ServiceException, Boolean]] = {
    Logger.debug(s"Unsetting from document: [collection=$collectionName, id=$id, field=$field]")
    recover(collection.update(DBQueryBuilder.id(id), DBQueryBuilder.unset(field))) {
      true
    }
  }

  def remove(id: String): Future[Either[ServiceException, Boolean]] = remove(BSONObjectID(id))

  def remove(id: BSONObjectID): Future[Either[ServiceException, Boolean]] = {
    Logger.debug(s"Removing document: [collection=$collectionName, id=$id]")
    recover(
      collection.remove(DBQueryBuilder.id(id))
    ) {
      true
    }
  }

  def remove(query: JsObject, firstMatchOnly: Boolean = false): Future[Either[ServiceException, Boolean]] = {
    Logger.debug(s"Removing document(s): [collection=$collectionName, firstMatchOnly=$firstMatchOnly, query=$query]")
    recover(
      collection.remove(query, firstMatchOnly = firstMatchOnly)
    ) {
      true
    }
  }

  def updated(data: JsObject) = {
    data.validate((__ \ '$set).json.update(
      __.read[JsObject].map{ o => o ++ Json.obj("updated" -> DateTime.now) }
    )).fold(
      error => data,
      success => success
    )
  }

  def ensureIndex(
    key: List[(String, IndexType)],
    name: Option[String] = None,
    unique: Boolean = false,
    background: Boolean = false,
    dropDups: Boolean = false,
    sparse: Boolean = false,
    version: Option[Int] = None,
    options: BSONDocument = BSONDocument()) = {

    val index = Index(key, name, unique, background, dropDups, sparse, version, options)
    Logger.info(s"Ensuring index: $index")
    collection.indexesManager.ensure(index)
  }

}
