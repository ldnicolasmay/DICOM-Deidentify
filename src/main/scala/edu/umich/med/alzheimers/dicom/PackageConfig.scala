package edu.umich.med.alzheimers.dicom

import java.io.File
import java.nio.file.{Path, Paths}

import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters._

/**
 * Capture of `copy.conf` configs
 */
object PackageConfig {

  private val configPath: Path =
    Paths.get("src/main/resources/config/package_test.conf")
  private val configFile: File = configPath.toFile
  private val config: Config = ConfigFactory.parseFile(configFile)

  private val appDirPathStr: String = config.getString("config.appDirPathStr")
  val appDirPath: Path = Paths.get(appDirPathStr)

  val intermedDirsRegexArray: Array[String] =
    config.getStringList("config.intermedDirsRegexArray").asScala.toArray
  val dicomFilenameRegexArray: Array[String] =
    config.getStringList("config.dicomFilenameRegexArray").asScala.toArray
  val seriesDescriptionRegexArray: Array[String] =
    config.getStringList("config.seriesDescriptionRegexArray").asScala.toArray

  val intermedDirsRegex: String = PackageConfig.intermedDirsRegexArray.mkString(sep = "|")
  val dicomFileRegex: String = PackageConfig.dicomFilenameRegexArray.mkString("|")
  val seriesDescriptionRegex: String = PackageConfig.seriesDescriptionRegexArray.mkString("|")

  val idPrefixStringArray: Array[String] = config.getStringList("config.idPrefixStringArray").asScala.toArray
  val expectedIdPrefixStr: String = config.getString("config.expectedIdPrefixStr")
}
