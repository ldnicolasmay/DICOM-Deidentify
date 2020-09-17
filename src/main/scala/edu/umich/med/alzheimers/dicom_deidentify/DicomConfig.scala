package edu.umich.med.alzheimers.dicom_deidentify

import java.io.File
import scala.jdk.CollectionConverters._
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Capture of `dicom.conf` configs
 */
object DicomConfig {

  private val config: Config = ConfigFactory.parseFile(new File("src/main/resources/config/dicom.conf"))

  val sourceDirPathStr: String = config.getString("config.sourceDirPathStr")
  val targetDirPathStr: String = config.getString("config.targetDirPathStr")

  val intermedDirsRegexArray: Array[String] =
    config.getStringList("config.intermedDirsRegexArray").asScala.toArray
  val dicomFilenameRegexArray: Array[String] =
    config.getStringList("config.dicomFilenameRegexArray").asScala.toArray
  val seriesDescriptionRegexArray: Array[String] =
    config.getStringList("config.seriesDescriptionRegexArray").asScala.toArray

  val dicomAttributesToReplaceWithZero: List[String] =
    config.getStringList("config.dicomAttributesToReplaceWithZero").asScala.toList
}
