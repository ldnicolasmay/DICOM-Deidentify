package edu.umich.med.alzheimers.dicom

import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.{COPY_ATTRIBUTES, REPLACE_EXISTING}
import java.nio.file.{CopyOption, Path, Paths}
import java.util.concurrent.Callable

import edu.umich.med.alzheimers.dicom.copy.{Copier, CopyConfig}
import edu.umich.med.alzheimers.dicom.deidentify.{Deidentifier, DeidentifyConfig}
import edu.umich.med.alzheimers.dicom.zip.{Zipper, ZipConfig}
import edu.umich.med.alzheimers.dicom.filesystem.DirNode
import org.slf4j.{Logger, LoggerFactory}
import picocli.CommandLine
import picocli.CommandLine.{Command, Option}

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
class CopyDeidentifyZip extends Callable[Int] {

  /** Set up logger for Main class */
  private def logger: Logger = CopyDeidentifyZip.logger

  /** Capture CLI options */
  @Option(
    names = Array("-v", "--verbose"),
    description = Array("Verbose output"),
    paramLabel = "VERBOSE"
  )
  var verbose: Boolean = _

  @Option(
    names = Array("-t", "--print-file-trees"),
    description = Array("Print file trees"),
    paramLabel = "PRINT_FILE_TREES"
  )
  var printFileTrees: Boolean = _

  @throws(classOf[Exception])
  override def call(): Int = {

    // Capture Copy configs
    val copySourceDirPath: Path = Paths.get(CopyConfig.sourceDirPathStr)
    val copyTargetDirPath: Path = Paths.get(CopyConfig.targetDirPathStr)
    logger.info(s"sourceDirPath=${copySourceDirPath.getFileName.toString}")
    logger.info(s"targetDirPath=${copyTargetDirPath.getFileName.toString}")

    val intermedDirsRegex: String = copySourceDirPath.getFileName.toString + "|" +
      CopyConfig.intermedDirsRegexArray.mkString(sep = "|")
    val dicomFileRegex: String = CopyConfig.dicomFilenameRegexArray.mkString("|")
    val seriesDescriptionRegex: String = CopyConfig.seriesDescriptionRegexArray.mkString("|")
    logger.info(s"intermedDirsRegex=${intermedDirsRegex}")
    logger.info(s"dicomFileRegex=${dicomFileRegex}")
    logger.info(s"seriesDescriptionRegex=${seriesDescriptionRegex}")

    // Capture Deidentify configs
    val deidentifySourceDirPath: Path = copySourceDirPath

    // Capture Zip configs
    val zipSourceDirPath: Path = Paths.get(ZipConfig.sourceDirPathStr)
    val zipTargetDirPath: Path = Paths.get(ZipConfig.targetDirPathStr)
    val zipDepth: Int = ZipConfig.zipDepth

    // Create source and target DirNode trees
    val sourceDirNode = DirNode(copySourceDirPath, 0, intermedDirsRegex, dicomFileRegex)
    logger.info(s"sourceDirNode=${sourceDirNode.dirPath.toString}, ${sourceDirNode.countSubNodes()} nodes")
    val targetDirNode = DirNode(copyTargetDirPath, 0, intermedDirsRegex, dicomFileRegex)
    logger.info(s"targetDirNode=${targetDirNode.dirPath.toString}, ${targetDirNode.countSubNodes()} nodes")

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
      .substituteRootNodeName(targetDirNode.dirPath.getFileName.toString, sourceDirNode.dirPath.getFileName.toString)
    logger.info(s"targetDirNodeWithSourceRoot, ${targetDirNodeWithSourceRoot.countSubNodes()} nodes")

    // Filter source DirNode tree based for files that do NOT already exist in target DirNode tree
    val sourceDirNodeFilteredMinusTargetNodeWithSourceRoot = sourceDirNodeFiltered
      .filterNotChildFileNodesWith(childFileNodeExistsIn(targetDirNodeWithSourceRoot))
      .filterChildDirNodesWith(nonemptyDirNodesFilter)
    logger.info(s"sourceDirNodeFilteredMinusTargetNodeWithSourceRoot, " +
      s"${sourceDirNodeFilteredMinusTargetNodeWithSourceRoot.countSubNodes()} nodes")

    // Copy directories and files
    // TODO: Create config and/or command-line options for controlling file overwriting and attribute-copying
    val copyOptions: Seq[CopyOption] = Seq(REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS)
    val copier = new Copier(copySourceDirPath, copyTargetDirPath, copyOptions)
    sourceDirNodeFilteredMinusTargetNodeWithSourceRoot.copyNode(copier)
    logger.info("sourceDirNodeFilteredMinusTargetNodeWithSourceRoot copied to disk")

    val sourceDirNodeFilteredMinusTargetNodeWithSourceRootWithTargetRoot: DirNode =
      sourceDirNodeFilteredMinusTargetNodeWithSourceRoot
        .substituteRootNodeName(
          sourceDirNode.dirPath.getFileName.toString,
          targetDirNode.dirPath.getFileName.toString
        )

    // Deidentify files
    // val fileDeidentifier = new Deidentifier(deidentifySourceDirPath, copyOptions)
    val deidentifier = new Deidentifier(deidentifySourceDirPath)
    sourceDirNodeFilteredMinusTargetNodeWithSourceRootWithTargetRoot.deidentifyNode(deidentifier)
    logger.info("sourceDirNodeFilteredMinusTargetNodeWithSourceRoot deidentified")

    // Zip directories
    val dirNodeDepth = sourceDirNodeFilteredMinusTargetNodeWithSourceRootWithTargetRoot.depth
    val zipper = new Zipper(zipSourceDirPath, zipTargetDirPath, dirNodeDepth, zipDepth)
    sourceDirNodeFilteredMinusTargetNodeWithSourceRootWithTargetRoot.zipNode(zipper)
    logger.info("sourceDirNodeFilteredMinusTargetNodeWithSourceRoot zipped")

    0
  }
}

object CopyDeidentifyZip {
  val logger: Logger = LoggerFactory.getLogger(classOf[CopyDeidentifyZip])

  def main(args: Array[String]): Unit = {
    val exitCode: Int = new CommandLine(new CopyDeidentifyZip()).execute(args: _*)
    System.exit(exitCode)
  }
}