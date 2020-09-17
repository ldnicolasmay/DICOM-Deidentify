package edu.umich.med.alzheimers.dicom_deidentify

import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.{COPY_ATTRIBUTES, REPLACE_EXISTING}
import java.nio.file.{CopyOption, Path, Paths}
import java.util.concurrent.Callable

import picocli.CommandLine
import picocli.CommandLine.{Command, Option}
import org.slf4j.{Logger, LoggerFactory}

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
class Main extends Callable[Int] {

  /** Set up logger for Main class */
  private def logger: Logger = Main.logger

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

    // Capture source and target directory paths from config
    val sourceDirPath: Path = Paths.get(DicomConfig.sourceDirPathStr)
    val targetDirPath: Path = Paths.get(DicomConfig.targetDirPathStr)
    logger.info(s"sourceDirPath=${sourceDirPath.getFileName.toString}")
    logger.info(s"targetDirPath=${targetDirPath.getFileName.toString}")

    // Capture regexes from config
    val intermedDirsRegex: String = sourceDirPath.getFileName.toString + "|" +
      DicomConfig.intermedDirsRegexArray.mkString(sep = "|")
    val dicomFileRegex: String = DicomConfig.dicomFilenameRegexArray.mkString("|")
    val seriesDescriptionRegex: String = DicomConfig.seriesDescriptionRegexArray.mkString("|")
    logger.info(s"intermedDirsRegex=${intermedDirsRegex}")
    logger.info(s"dicomFileRegex=${dicomFileRegex}")
    logger.info(s"seriesDescriptionRegex=${seriesDescriptionRegex}")

    // Create source and target DirNode trees
    val sourceDirNode = DirNode(sourceDirPath, 0, intermedDirsRegex, dicomFileRegex)
    logger.info(s"sourceDirNode=${sourceDirNode.dirPath.toString}, ${sourceDirNode.countSubNodes()} nodes")
    val targetDirNode = DirNode(targetDirPath, 0, intermedDirsRegex, dicomFileRegex)
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

    // Create FileCopier object for copying directories and files
    // TODO: Create config and/or command-line options for controlling file overwriting and attribute-copying
    val copyOptions: Seq[CopyOption] = Seq(REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS)
    val fileCopier = new FileCopier(sourceDirPath, targetDirPath, copyOptions, verbose)
    logger.info("fileCopier created")

    // Copy directories and files in filtered source DirNode tree
    sourceDirNodeFilteredMinusTargetNodeWithSourceRoot.copyNode(fileCopier)
    logger.info("sourceDirNodeFilteredMinusTargetNodeWithSourceRoot copied to disk")

    0
  }
}

// Main class companion object
object Main extends App {
  private val logger: Logger = LoggerFactory.getLogger(classOf[Main])
  val exitCode: Int = new CommandLine(new Main()).execute(args: _*)
  System.exit(exitCode)
}
