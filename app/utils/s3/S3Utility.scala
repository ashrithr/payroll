package utils.s3

import awscala.File
import awscala.s3.{Bucket, PutObjectResult, S3, S3Object}

object S3Utility {

  implicit val s3 = S3()

  def getBuckets: Seq[Bucket] = s3.buckets

  def getBucketByName(name: String): Option[Bucket] = s3.bucket(name)

  def createBucket(name: String): Bucket = s3.createBucket(name)

  def createObject(bucket: Bucket, name: String, file: File): PutObjectResult = bucket.put(name, file)

  def getObject(bucket: Bucket, name: String): Option[S3Object] = bucket.getObject(name)

}
