package edu.umich.med.alzheimers.dicom.zip

import java.nio.file.{Path, Paths}
import java.util.concurrent.Callable

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
class Zip extends Callable[Int] {

  /** Logger */
  private def logger: Logger = Zip.logger

  @throws(classOf[Exception])
  override def call(): Int = {

    // Capture source and target directory paths from config
    val sourceDirPath: Path = Paths.get(ZipConfig.sourceDirPathStr)
    val targetDirPath: Path = Paths.get(ZipConfig.targetDirPathStr)
    logger.info(s"sourceDirPath=${sourceDirPath.getFileName.toString}")
    logger.info(s"targetDirPath=${targetDirPath.getFileName.toString}")

    // Capture zip depth from config
    val zipDepth: Int = ZipConfig.zipDepth

    // Create source DirNode tree
    val sourceDirNode = DirNode(sourceDirPath, 0, ".*", ".*")
    logger.info(s"sourceDirNode=${sourceDirNode.path.toString}")
    logger.info(s"sourceDirNode, ${sourceDirNode.countSubNodes()} nodes")

    // Zip source DirNode tree at zip depth
    val sourceDirNodeDepth = sourceDirNode.depth
    val zipper = new Zipper(sourceDirPath, targetDirPath, sourceDirNodeDepth, zipDepth)
    sourceDirNode.zipNode(zipper)
    logger.info(s"${sourceDirNode.path.toString} zipped to ${targetDirPath.toString}")

    0
  }
}

object Zip {
  /** Logger */
  val logger: Logger = LoggerFactory.getLogger(classOf[Zip])

  def main(args: Array[String]): Unit = {
    val exitCode: Int = new CommandLine(new Zip()).execute(args: _*)
    System.exit(exitCode)
  }
}