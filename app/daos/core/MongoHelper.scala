package daos.core

import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson.{BSONObjectID, BSONValue}

/**
  * Helper around MongoDB resources
  */
trait MongoHelper extends ContextHelper {

  // TODO figure out how to replace `current.injector` with DI
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]

  lazy val db = reactiveMongoApi.db

}

object MongoHelper extends MongoHelper {

  def identity(bson: BSONValue) = bson.asInstanceOf[BSONObjectID].stringify

}
