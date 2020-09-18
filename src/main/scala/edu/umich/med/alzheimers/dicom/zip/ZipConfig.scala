package edu.umich.med.alzheimers.dicom.zip

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

object ZipConfig {

  private val config: Config = ConfigFactory.parseFile(new File("src/main/resources/config/zip.conf"))

  val dicomZipConfigZipDepth = config.getInt("config.zipDepth")
}
