package edu.umich.med.alzheimers.dicom.deidentify

import java.nio.file.{Path, Paths}
import java.util.concurrent.Callable

import edu.umich.med.alzheimers.dicom.PackageConfig
import edu.umich.med.alzheimers.dicom.filesystem.DirNode
import org.slf4j.{Logger, LoggerFactory}
import picocli.CommandLine
import picocli.CommandLine.Command

/**
 * Driver class
 */
@Command(
  name = "java -jar DICOM-Deidentify.jar",
  mixinStandardHelpOptions = true,
  version = Array("DICOM-Deidentify 0.1"),
  description = Array("Driver object"),
  sortOptions = false,
  showDefaultValues = true,
  headerHeading = "Usage:%n",
  synopsisHeading = "%n",
  descriptionHeading = "%nDescription:%n%n",
  parameterListHeading = "%nParameters:%n",
  optionListHeading = "%nOptions:%n"
)
class Deidentify extends Callable[Int] {

  /** Logger */
  private def logger: Logger = Deidentify.logger

  @throws(classOf[Exception])
  override def call(): Int = {

    // Capture source directory path from config
    val sourceDirPath: Path = Paths.get(DeidentifyConfig.sourceDirPathStr)
    logger.info(s"sourceDirPath=${sourceDirPath.getFileName.toString}")

    // Capture Package configs
    val intermedDirsRegex: String = sourceDirPath.getFileName.toString + "|" + PackageConfig.intermedDirsRegex
    val dicomFileRegex: String = PackageConfig.dicomFilenameRegexArray.mkString("|")
    val seriesDescriptionRegex: String = PackageConfig.seriesDescriptionRegexArray.mkString("|")
    logger.info(s"intermedDirsRegex=${intermedDirsRegex}")
    logger.info(s"dicomFileRegex=${dicomFileRegex}")
    logger.info(s"seriesDescriptionRegex=${seriesDescriptionRegex}")

    // Create source `DirNode` tree
    val sourceDirNode: DirNode = DirNode(sourceDirPath, 0, intermedDirsRegex, dicomFileRegex)
    logger.info(s"sourceDirNode=${sourceDirNode.path.toString}")

    // Deidentify files
    val deidentifier = new Deidentifier(sourceDirPath)
    sourceDirNode.deidentifyNode(deidentifier)
    logger.info(s"sourceDirNode deidentified")

    0
  }
}

object Deidentify {
  val logger: Logger = LoggerFactory.getLogger(classOf[Deidentify])

  def main(args: Array[String]): Unit = {
    val exitCode: Int = new CommandLine(new Deidentify()).execute(args: _*)
    System.exit(exitCode)
  }
}