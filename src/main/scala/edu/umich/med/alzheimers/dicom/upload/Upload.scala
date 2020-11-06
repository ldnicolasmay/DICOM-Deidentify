package edu.umich.med.alzheimers.dicom.upload

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
class Upload extends Callable[Int] {

  /** Logger */
  private def logger: Logger = Upload.logger

  @throws(classOf[Exception])
  override def call(): Int = {

    // Capture source directory paths from config
    val sourceDirPath: Path = Paths.get(UploadConfig.sourceDirPathStr)
    logger.info(s"sourceDirPath=${sourceDirPath.getFileName.toString}")

    // Capture Upload configs
    val uploadDepth: Int = UploadConfig.uploadDepth
    val awsAccessKeyId: String = UploadConfig.awsAccessKeyId
    val awsSecretAccessKey: String = UploadConfig.awsSecretAccessKey
    val s3BucketStr: String = UploadConfig.s3BucketStr
    val s3KeyPrefixStr: String = UploadConfig.s3KeyPrefixStr

    // Create source DirNode trees
    val sourceDirNode = DirNode(sourceDirPath, 0, ".*", ".*")
    logger.info(s"sourceDirNode=${sourceDirNode.dirPath.toString}")
    logger.info(s"sourceDirNode, ${sourceDirNode.countSubNodes()} nodes")

    val sourceDirNodeDepth = sourceDirNode.depth
    val uploader =
      new Uploader(
        sourceDirPath,
        sourceDirNodeDepth,
        uploadDepth,
        s3BucketStr,
        s3KeyPrefixStr,
        awsAccessKeyId,
        awsSecretAccessKey
      )

    sourceDirNode.uploadNode(uploader)
    val s3BucketKeyPrefixStr: String = s3BucketStr + "/" + s3KeyPrefixStr
    logger.info(s"${sourceDirNode.dirPath.toString} uploaded to ${s3BucketKeyPrefixStr}")

    0
  }
}

object Upload {

  /** Logger */
  val logger: Logger = LoggerFactory.getLogger(classOf[Upload])

  def main(args: Array[String]): Unit = {
    val exitCode: Int = new CommandLine(new Upload()).execute(args: _*)
    System.exit(exitCode)
  }
}
