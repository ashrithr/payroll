package services

import java.io.File
import java.util.UUID
import javax.inject.Inject

import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, Request}
import play.api.{Logger, Mode}
import utils.s3.S3Utility

class S3Service @Inject() (environment: play.api.Environment) {

  def uploadFile(request: Request[MultipartFormData[TemporaryFile]]): String = {
    Logger.trace(s"Called uploadFile function: $request")
    request.body.file("file").map { file =>
      val fileName = file.filename
      val contentType = file.contentType
      Logger.trace(s"File name : $fileName, content type : $contentType")
      val uniqueFile = new File(s"/tmp/${UUID.randomUUID()}_$fileName")
      file.ref.moveTo(uniqueFile, replace = true)
      // TODO: replace `isProd`
      if (environment.mode == Mode.Prod) {
        try {
          val bucket = S3Utility.getBucketByName("test").getOrElse(S3Utility.createBucket("test"))
          val result = S3Utility.createObject(bucket, fileName, uniqueFile)
          s"File uploaded on S3 with Key : ${result.key}"
        } catch {
          case t: Throwable => Logger.error(t.getMessage, t); t.getMessage
        }
      } else {
        s"File($fileName) uploaded"
      }
    }.getOrElse {
      "Missing file"
    }
  }

}
