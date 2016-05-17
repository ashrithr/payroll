package daos.core

import daos.exceptions._
import play.api.Logger
import reactivemongo.api.commands.WriteResult
import reactivemongo.core.errors.DatabaseException

import scala.concurrent.Future

/**
  * Base DAO for Mongo resources
  */
trait BaseDao extends MongoHelper {

  val collectionName: String

  def ensureIndexes: Future[Boolean]

  def recover[S](operation: Future[WriteResult])(success: => S): Future[Either[ServiceException, S]] = {
    operation.map {
      lastError => lastError.inError match {
        case true =>
          Logger.error(s"DB operation did not perform successfully: [lastError=$lastError]")
          Left(DBServiceException(lastError.message))
        case false =>
          Right(success)
      }
    } recover {
      case exception =>
        Logger.error(s"DB operation failed: [message=${exception.getMessage}]")

        val handling: Option[Either[ServiceException, S]] = exception match {
          case e: DatabaseException => {
            e.code.map(code => {
              Logger.error(s"DatabaseException: [code=$code, isNotAPrimaryError=${e.isNotAPrimaryError}]")
              code match {
                case 10148 => {
                  Left(OperationNotAllowedException("", nestedException = e))
                }
                case 11000 => {
                  Left(DuplicateResourceException(nestedException = e))
                }
              }
            })
          }
        }
        handling.getOrElse(Left(UnexpectedServiceException(exception.getMessage, nestedException = exception)))
    }
  }

}
