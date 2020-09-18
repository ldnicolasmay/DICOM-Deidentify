package edu.umich.med.alzheimers.dicom.copy

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters._

/**
 * Capture of `copy.conf` configs
 */
object CopyConfig {

  private val config: Config = ConfigFactory.parseFile(new File("src/main/resources/config/copy.conf"))

  val sourceDirPathStr: String = config.getString("config.sourceDirPathStr")
  val targetDirPathStr: String = config.getString("config.targetDirPathStr")

  val intermedDirsRegexArray: Array[String] =
    config.getStringList("config.intermedDirsRegexArray").asScala.toArray
  val dicomFilenameRegexArray: Array[String] =
    config.getStringList("config.dicomFilenameRegexArray").asScala.toArray
  val seriesDescriptionRegexArray: Array[String] =
    config.getStringList("config.seriesDescriptionRegexArray").asScala.toArray
}
