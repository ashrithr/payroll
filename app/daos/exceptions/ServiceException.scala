package daos.exceptions

/**
  * Trait for service exceptions
  */
trait ServiceException {

  val message: String
  val nestedException: Throwable

}
