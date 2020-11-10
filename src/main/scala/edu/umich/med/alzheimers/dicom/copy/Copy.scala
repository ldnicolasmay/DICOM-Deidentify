package edu.umich.med.alzheimers.dicom.copy

import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.{COPY_ATTRIBUTES, REPLACE_EXISTING}
import java.nio.file.{CopyOption, Path, Paths}
import java.util.concurrent.Callable

import edu.umich.med.alzheimers.dicom.{
  PackageConfig, childFileNodeExistsIn, dicomFileFilter, dicomFileSeriesDescripFilter, intermedDirNameFilter,
  nonemptyDirNodesFilter, numberOfFilesFilter
}
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
class Copy extends Callable[Int] {

  /** Logger */
  private def logger: Logger = Copy.logger

  @throws(classOf[Exception])
  override def call(): Int = {

    // Capture source and target directory paths from config
    val sourceDirPath: Path = Paths.get(CopyConfig.sourceDirPathStr)
    val targetDirPath: Path = Paths.get(CopyConfig.targetDirPathStr)
    logger.info(s"sourceDirPath=${sourceDirPath.getFileName.toString}")
    logger.info(s"targetDirPath=${targetDirPath.getFileName.toString}")

    // Capture Package configs
    val intermedDirsRegex: String = sourceDirPath.getFileName.toString + "|" + PackageConfig.intermedDirsRegex
    val dicomFileRegex: String = PackageConfig.dicomFilenameRegexArray.mkString("|")
    val seriesDescriptionRegex: String = PackageConfig.seriesDescriptionRegexArray.mkString("|")
    logger.info(s"intermedDirsRegex=${intermedDirsRegex}")
    logger.info(s"dicomFileRegex=${dicomFileRegex}")
    logger.info(s"seriesDescriptionRegex=${seriesDescriptionRegex}")

    // Create source and target DirNode trees
    val sourceDirNode = DirNode(sourceDirPath, 0, intermedDirsRegex, dicomFileRegex)
    logger.info(s"sourceDirNode=${sourceDirNode.path.toString}")
    logger.info(s"sourceDirNode, ${sourceDirNode.countSubNodes()} nodes")
    val targetDirNode = DirNode(targetDirPath, 0, intermedDirsRegex, dicomFileRegex)
    logger.info(s"targetDirNode=${targetDirNode.path.toString}")
    logger.info(s"targetDirNode, ${targetDirNode.countSubNodes()} nodes")

    // Filter source DirNode tree using regexes and focused filters
    val sourceDirNodeFiltered = sourceDirNode
      .filterChildDirNodesWith(intermedDirNameFilter(intermedDirsRegex))
      .filterChildFileNodesWith(dicomFileFilter(dicomFileRegex))
      .filterChildDirNodesWith(numberOfFilesFilter(210))
      .filterChildFileNodesWith(dicomFileSeriesDescripFilter(seriesDescriptionRegex))
      .filterChildDirNodesWith(nonemptyDirNodesFilter)
    logger.info(s"sourceDirNodeFiltered, ${sourceDirNodeFiltered.countSubNodes()} nodes")


    // Substitute target DirNode tree root path with source DirNode tree root path,
    // facilitating identification of source- and target- DirNode tree discrepancies
    val targetDirNodeWithSourceRoot = targetDirNode
      .substituteRootNodeName(
        targetDirNode.path.getFileName.toString,
        sourceDirNode.path.getFileName.toString
      )
    logger.info(s"targetDirNodeWithSourceRoot, ${targetDirNodeWithSourceRoot.countSubNodes()} nodes")

    // Filter source DirNode tree based for files that do NOT already exist in target DirNode tree
    val sourceDirNodeFilteredMinusTargetNodeWithSourceRoot = sourceDirNodeFiltered
      .filterChildNodesWith(node => !targetDirNodeWithSourceRoot.hasNode(node))
    logger.info(s"sourceDirNodeFilteredMinusTargetNodeWithSourceRoot, " +
      s"${sourceDirNodeFilteredMinusTargetNodeWithSourceRoot.countSubNodes()} nodes")

    // Copy directories and files
    // TODO: Create config and/or command-line options for controlling file overwriting and attribute-copying
    val copyOptions: Seq[CopyOption] = Seq(REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS)
    val copier = new Copier(sourceDirPath, targetDirPath, copyOptions)
    sourceDirNodeFilteredMinusTargetNodeWithSourceRoot.copyNode(copier)
    logger.info("sourceDirNodeFilteredMinusTargetNodeWithSourceRoot copied to disk")

    0
  }
}

object Copy {
  /** Logger */
  val logger: Logger = LoggerFactory.getLogger(classOf[Copy])

  def main(args: Array[String]): Unit = {
    val exitCode: Int = new CommandLine(new Copy()).execute(args: _*)
    System.exit(exitCode)
  }
}