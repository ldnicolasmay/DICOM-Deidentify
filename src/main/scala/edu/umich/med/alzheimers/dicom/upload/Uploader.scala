package edu.umich.med.alzheimers.dicom.upload

import java.io.IOException
import java.nio.file.FileVisitResult.{CONTINUE, SKIP_SIBLINGS, SKIP_SUBTREE}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, FileVisitor, Path}

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.regions.{Region, Regions}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Zipped file uploaded implementing Java `FileVisitor`
 *
 * @param sourceDirPath      `Path` of source directory
 * @param nodeDepth          `Int` depth of this node in its `DirNode` tree
 * @param uploadDepth        `Int` depth at which to pick up zip files for upload
 * @param s3BucketStr        `String` of S3 bucket
 * @param s3KeyPrefixStr     `String` of S3 key prefix
 * @param awsAccessKeyId     `String` of AWS access key ID
 * @param awsSecretAccessKey `String` of AWS secret access key
 */
class Uploader(
                val sourceDirPath: Path,
                val nodeDepth: Int,
                val uploadDepth: Int,
                val s3BucketStr: String,
                val s3KeyPrefixStr: String,
                val awsAccessKeyId: String,
                val awsSecretAccessKey: String
              )
  extends FileVisitor[Path] {

  /** Logger */
  private def logger: Logger = Uploader.logger

  /**
   * Performs actions before visiting a directory
   *
   * @param dir   `Path` of directory to act on before visit
   * @param attrs `BasicFileAttributes` of `dir`
   * @return `FileVisitResult` to `CONTINUE`, `SKIP_SUBTREE`, or `SKIP_SIBLINGS`
   */
  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (nodeDepth < uploadDepth) {
      CONTINUE
    }
    else if (nodeDepth == uploadDepth) {
      logger.error(s"Directory ${dir.toString} nodeDepth @ ${nodeDepth} equals uploadDepth @ ${uploadDepth}; " +
        s"only zip files should be at configured uploadDepth ${uploadDepth}")
      SKIP_SUBTREE
    }
    else {
      SKIP_SIBLINGS
    }
  }

  /**
   * Performs actions after visiting a directory
   *
   * @param dir `Path` of directory to act on after visit
   * @param exc `IOException` thrown from directory visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
    if (exc != null) {
      exc.printStackTrace()
      logger.error(s"Error during visit of ${dir.toString}")
    }

    CONTINUE
  }

  /**
   * Performs actions when visiting a file
   *
   * @param file  `Path` of file to visit
   * @param attrs `BasicFileAttributes` of file
   * @return `FileVisitResult` to `CONTINUE`, `SKIP_SUBTREE`, or `SKIP_SIBLINGS`
   */
  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (nodeDepth < uploadDepth) {
      CONTINUE
    }
    else if (nodeDepth == uploadDepth) {
      val s3BucketKeyStr: String = s3BucketStr + "/" + s3KeyPrefixStr
      logger.info(s"Upload ${file.toString} to ${s3BucketKeyStr} with depth ${nodeDepth} @ depth ${uploadDepth}")
      Uploader.uploadFile(file, s3BucketStr, s3KeyPrefixStr, awsAccessKeyId, awsSecretAccessKey)
      SKIP_SUBTREE
    }
    else {
      SKIP_SIBLINGS
    }
  }

  /**
   * Performs actions when visiting a file fails
   *
   * @param file `Path` of file whose visit failed
   * @param exc  `IOException` thrown by failed file visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
    if (exc != null) {
      logger.error(s"visitFileFailed=${file.toString}")
    }

    CONTINUE
  }
}

/**
 * Companion object for `Uploader` class
 */
object Uploader {

  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[Uploader])

  /**
   * Upload file to S3 bucket
   *
   * @param file               `Path` of file to upload
   * @param s3BucketStr        `String` of S3 bucket
   * @param s3KeyPrefixStr     `String` of S3 key prefix
   * @param awsAccessKeyId     `String` of AWS access key ID
   * @param awsSecretAccessKey `String` of AWS secret access key
   */
  def uploadFile(
                  file: Path,
                  s3BucketStr: String,
                  s3KeyPrefixStr: String,
                  awsAccessKeyId: String,
                  awsSecretAccessKey: String): Unit = {
    // Build S3 client
    val usEast1: Region = Region.getRegion(Regions.US_EAST_1)
    val awsCredentials: BasicAWSCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey)
    val s3client: AmazonS3Client = new AmazonS3Client(awsCredentials)
    s3client.setRegion(usEast1)

    // Upload file to S3 bucket
    try {
      s3client.putObject(s3BucketStr, s3KeyPrefixStr + file.getFileName.toString, file.toFile)
    }
    catch {
      case e: AmazonServiceException =>
        logger.error(s"Unable to upload: uploadFile(${file.toString}): $e")
      case e: Exception =>
        e.printStackTrace()
    }
  }
}
