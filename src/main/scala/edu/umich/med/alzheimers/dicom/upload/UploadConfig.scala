package edu.umich.med.alzheimers.dicom.upload

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

object UploadConfig {

  private val config: Config =
    ConfigFactory.parseFile(new File("src/main/resources/config/upload_test.conf"))

  val sourceDirPathStr: String = config.getString("config.sourceDirPathStr")
  val uploadDepth: Int = config.getInt("config.uploadDepth")
  val awsAccessKeyId: String = config.getString("config.awsAccessKeyId")
  val awsSecretAccessKey: String = config.getString("config.awsSecretAccessKey")
  val s3BucketStr: String = config.getString("config.s3BucketStr")
  val s3KeyPrefixStr: String = config.getString("config.s3KeyPrefixStr")
}
