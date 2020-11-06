package edu.umich.med.alzheimers.dicom.copy

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Capture of `copy.conf` configs
 */
object CopyConfig {

  private val config: Config =
    ConfigFactory.parseFile(new File("src/main/resources/config/copy_test.conf"))

  val sourceDirPathStr: String = config.getString("config.sourceDirPathStr")
  val targetDirPathStr: String = config.getString("config.targetDirPathStr")
}