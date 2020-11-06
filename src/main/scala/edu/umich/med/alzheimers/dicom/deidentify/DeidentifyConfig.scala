package edu.umich.med.alzheimers.dicom.deidentify

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters._

/**
 * Capture of `copy.conf` configs
 */
object DeidentifyConfig {

  private val config: Config =
    ConfigFactory.parseFile(new File("src/main/resources/config/deidentify_test.conf"))

  val sourceDirPathStr: String = config.getString("config.sourceDirPathStr")
  val reformatPatientId: Boolean = config.getBoolean("config.reformatPatientId")
  val dicomAttributesToReplaceWithZero: List[String] =
    config.getStringList("config.dicomAttributesToReplaceWithZero").asScala.toList
}
