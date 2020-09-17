package edu.umich.med.alzheimers.dicom_deidentify

import java.io.{BufferedOutputStream, FileOutputStream, IOException}
import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.nio.file.FileVisitResult._
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.nio.file.{CopyOption, DirectoryNotEmptyException, FileAlreadyExistsException}
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}

import com.pixelmed.dicom.{Attribute, AttributeList, AttributeTag, DicomDictionary, DicomException, TagFromName}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Implementation of Java `FileVisitor`
 *
 * @param sourceDirPath `Path` of source directory
 * @param targetDirPath `Path` of target directory
 * @param copyOptions   `Seq` of possible `REPLACE_EXISTING`, `COPY_ATTRIBUTES`, `NOFOLLOW_LINK`
 * @param verbose       Boolean flag for verbose printing
 */
class FileCopier(
                  val sourceDirPath: Path,
                  val targetDirPath: Path,
                  val copyOptions: Seq[CopyOption],
                  val verbose: Boolean)
  extends FileVisitor[Path] {

  /** Logger */
  private def logger: Logger = FileCopier.logger

  /**
   * Performs actions before visiting a directory
   *
   * @param dir  `Path` of directory to act on before visit
   * @param attr `BasicFileAttributes` of `dir`
   * @return `FileVisitResult` to `CONTINUE` or `SKIP_SUBTREE`
   */
  override def preVisitDirectory(dir: Path, attr: BasicFileAttributes): FileVisitResult = {
    val target: Path = targetDirPath.resolve(sourceDirPath.relativize(dir))

    try {
      Files.copy(dir, target, copyOptions: _*)
      logger.info(s"Copy ${dir.toString} to ${target.toString}")
    }
    catch {
      case e: DirectoryNotEmptyException =>
        logger.error(s"Unable to copy: preVisitDirectory(${dir.toString}): $e")
        SKIP_SUBTREE
      case e: IOException =>
        logger.error(s"Unable to copy: preVisitDirectory(${dir.toString}): $e")
        SKIP_SUBTREE
    }

    CONTINUE
  }

  /**
   * Performs actions after visiting a directory
   *
   * @param dir Path of directory to act on after visit
   * @param e   IOException thrown from directory visit
   * @return FileVisitResult to CONTINUE
   */
  override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
    if (e == null) {
      val target = targetDirPath.resolve(sourceDirPath.relativize(dir))

      try {
        val basicAttrs: BasicFileAttributes = Files.readAttributes(dir, classOf[BasicFileAttributes])
        Files.setAttribute(target, "lastModifiedTime", basicAttrs.lastModifiedTime(): FileTime)
        Files.setAttribute(target, "creationTime", basicAttrs.creationTime(): FileTime)
        Files.setAttribute(target, "lastAccessTime", basicAttrs.lastAccessTime(): FileTime)
      }
      catch {
        case e: IOException =>
          logger.error(s"Unable to copy attributes: postVisitDirectory(${dir.toString}): $e")
      }
    }

    CONTINUE
  }

  /**
   * Performs actions when visiting a Java File object
   *
   * @param file File to visit
   * @param attr BasicFileAttributes of `file`
   * @return FileVisitResult to CONTINUE
   */
  override def visitFile(file: Path, attr: BasicFileAttributes): FileVisitResult = {
    val target = targetDirPath.resolve(sourceDirPath.relativize(file))

    FileCopier.copyFile(file, target, copyOptions, verbose)
    logger.info(s"Copy ${file.toString} to ${target.toString}")

    FileCopier.deidentifyDicomFile(file, attr, target, copyOptions)
    logger.info(s"Deidentify ${target.toString}")

    CONTINUE
  }

  /**
   * Performs actions when visiting a Java File object fails
   *
   * @param file File whose visit failed
   * @param e    IOException thrown by failed file visit
   * @return FileVisitResult to CONTINUE
   */
  override def visitFileFailed(file: Path, e: IOException): FileVisitResult = {
    logger.error(s"Unable to copy: visitFileFailed(${file.toString}): $e")

    CONTINUE
  }
}

/**
 * Companion object for FileCopier class
 */
object FileCopier {

  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[FileCopier])

  /**
   * Copies file from source to target
   *
   * @param source      Path of source file to copy
   * @param target      Path of target where `source` is to be copied
   * @param copyOptions Seq of possible REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINK
   * @param verbose     Boolean flag for verbose printing
   */
  def copyFile(
                source: Path,
                target: Path,
                copyOptions: Seq[CopyOption],
                verbose: Boolean): Unit = {
    try {
      Files.copy(source, target, copyOptions: _*)
    }
    catch {
      case e: FileAlreadyExistsException =>
        logger.error(s"Unable to copy: copyFile(${source.toString}): $e")
      case e: IOException =>
        logger.error(s"Unable to copy: copyFile(${source.toString}): $e")
    }
  }

  /**
   * Deidentify AttributeList of DICOM file via helper functions
   *
   * @param sourceDicomFile Source Path of DICOM file
   * @param attr            BasicFileAttributes of `sourceDicomFile`
   * @param targetDicomFile Target Path of DICOM file
   * @param copyOptions     Seq of possible REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINK
   */
  private def deidentifyDicomFile(
                                   sourceDicomFile: Path,
                                   attr: BasicFileAttributes,
                                   targetDicomFile: Path,
                                   copyOptions: Seq[CopyOption]): Unit = {
    // 1 Get DICOM targetFile AttributeList
    val attrList = FileCopier.getAttributeListFromPath(targetDicomFile)

    // 2 Reformat PatientID string: hlp17umm01234 => UM00001234
    FileCopier.reformatPatientId(targetDicomFile, attrList)

    // 3 Replace/remove private DICOM elements/attributes
    FileCopier.replacePrivateDicomElements(targetDicomFile, attrList)
    FileCopier.removePrivateElements(targetDicomFile, attrList)

    // 4 Write deidentified AttributeList object to file
    FileCopier.writeAttributeListToFile(sourceDicomFile, attrList, targetDicomFile, copyOptions)
  }

  /**
   * Extracts DICOM AttributeList from a file Path object
   *
   * @param dicomFile Path object of DICOM file
   * @return String of DICOM sequence series description
   */
  private def getAttributeListFromPath(dicomFile: Path): AttributeList = {
    val attrList = new AttributeList

    try
      attrList.read(dicomFile.toString)
    catch {
      case e: DicomException =>
        logger.error(s"getAttributeListFromPath(${dicomFile.toString}): $e")
      case e: IOException =>
        logger.error(s"getAttributeListFromPath(${dicomFile.toString}): $e")
    }

    attrList
  }

  /**
   * Reformats PatientId elements to UM-MAP ID format
   *
   * @param dicomFile Path object of DICOM file
   * @param attrList  AttributeList object from DICOM file
   */
  private def reformatPatientId(dicomFile: Path, attrList: AttributeList): Unit = {
    val idPrefix = """^hlp17umm|^bmh17umm|^hlp14umm|^17umm""".r
    val patientIdBefore: String =
      Attribute.getDelimitedStringValuesOrEmptyString(attrList, TagFromName.PatientID)

    if (patientIdBefore.matches("""^hlp17umm\d{5}$|^bmh17umm\d{5}$|^hlp14umm\d{5}$|^17umm\d{5}$""")) {
      val patientIdAfter: String = idPrefix.replaceFirstIn(patientIdBefore, "UM000")
      attrList.replaceWithValueIfPresent(TagFromName.PatientID, patientIdAfter)
    } else {
      throw new Exception(s"PatientID $patientIdBefore in ${dicomFile.toString} does not match expected format")
    }
  }

  /**
   * Retrieves AttributeTag object from DICOM attribute name string
   *
   * @param tagString DICOM attribute name
   * @return AttributeTag corresponding to tagString
   */
  def fromStringToAttributeTag(tagString: String): AttributeTag = {
    try {
      DicomDictionary.StandardDictionary.getTagFromName(tagString)
    } catch {
      case e: DicomException =>
        logger.error(s"Error getting tag ${tagString} in fomStringToAttributeTag(): ${e.toString}")
        DicomDictionary.StandardDictionary.getTagFromName("")
    }
  }

  /**
   * Replaces DICOM attributes/elements with zero-length strings
   *
   * @param dicomFile Copied DICOM file that needs to be deidentified
   * @param attrList  AttributeList object from DICOM file
   */
  private def replacePrivateDicomElements(dicomFile: Path, attrList: AttributeList): Unit = {
    val dicomAttributeTagsToReplaceWithZero: List[AttributeTag] =
      DicomConfig.dicomAttributesToReplaceWithZero.map(fromStringToAttributeTag)

    dicomAttributeTagsToReplaceWithZero.foreach(attrList.replaceWithZeroLengthIfPresent)
  }

  /**
   * Remove all private attributes/elements from AttributeList object
   *
   * @param dicomFile Copied DICOM file that needs to be deidentified
   * @param attrList  AttributeList object from DICOM file
   */
  private def removePrivateElements(dicomFile: Path, attrList: AttributeList): Unit = {
    try {
      attrList.removePrivateAttributes()
    } catch {
      case e: DicomException =>
        logger.error(s"removePrivateElements(${dicomFile.toString}, ...): $e")
    }
  }

  /**
   * Write AttributeList object to Path object
   *
   * @param sourceDicomFile Source Path of DICOM file
   * @param targetDicomFile Target Path of DICOM file
   * @param copyOptions     Seq of possible REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINK
   * @param attrList        AttributeList object from `sourceDicomFile`
   */
  private def writeAttributeListToFile(sourceDicomFile: Path,
                                       attrList: AttributeList,
                                       targetDicomFile: Path,
                                       copyOptions: Seq[CopyOption]): Unit = {
    // 1 Write attrList to DicomOutputStream
    val transferSyntaxUID: String = attrList
      .get(TagFromName.TransferSyntaxUID)
      .getDelimitedStringValuesOrEmptyString // https://www.dicomlibrary.com/dicom/transfer-syntax/
    val bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetDicomFile.toString))

    // 2 Write edited AttributeList object (attrList) to BufferedFileOutputStream object (bufferedOutputStream)
    try {
      // https://www.dclunie.com/pixelmed/software/javadoc/com/pixelmed/dicom/ +
      //   AttributeList.html#write-java.io.OutputStream-java.lang.String-boolean-boolean-boolean-
      // write(BufferedFileOutputStream, TransferSyntaxUID, useMeta, useBufferedStream, closeAfterWrite)
      attrList.write(bufferedOutputStream, transferSyntaxUID, true, true, true)
      if (copyOptions.contains(COPY_ATTRIBUTES)) {
        Files.setLastModifiedTime(targetDicomFile, Files.getLastModifiedTime(sourceDicomFile, NOFOLLOW_LINKS))
        Files.setPosixFilePermissions(targetDicomFile, Files.getPosixFilePermissions(sourceDicomFile, NOFOLLOW_LINKS))
      }
    }
    catch {
      case e: DicomException =>
        logger.error(s"writeAttributeListToFile(${targetDicomFile.toString}) DicomExc: $e")
      case e: IOException =>
        logger.error(s"writeAttributeListToFile(${targetDicomFile.toString}) IOExc: $e")
    }
    finally {
      if (bufferedOutputStream != null)
        bufferedOutputStream.close()
      if (attrList != null)
        attrList.clear()
    }
  }

}