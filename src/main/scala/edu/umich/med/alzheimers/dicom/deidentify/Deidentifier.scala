package edu.umich.med.alzheimers.dicom.deidentify

import java.io.{BufferedOutputStream, FileOutputStream, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult._
import java.nio.file.{FileVisitResult, FileVisitor, Path}

import edu.umich.med.alzheimers.dicom.PackageConfig
import com.pixelmed.dicom.{Attribute, AttributeList, AttributeTag, DicomDictionary, DicomException, TagFromName}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.matching.Regex

/**
 * DICOM file deidentifier implementing Java `FileVisitor`
 *
 * @param sourceDirPath `Path` of source directory
 */
class Deidentifier(val sourceDirPath: Path) extends FileVisitor[Path] {

  /** Logger */
  private def logger: Logger = Deidentifier.logger

  /**
   * Performs actions before visiting a directory
   *
   * @param dir  `Path` of directory to act on before visit
   * @param attr `BasicFileAttributes` of `dir`
   * @return `FileVisitResult` to `CONTINUE` or `SKIP_SUBTREE`
   */
  override def preVisitDirectory(dir: Path, attr: BasicFileAttributes): FileVisitResult = {
    CONTINUE
  }

  /**
   * Performs actions after visiting a directory
   *
   * @param dir `Path` of directory to act on after visit
   * @param e   `IOException` thrown from directory visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
    if (e != null) {
      logger.error(s"Error during deidentification within ${dir.toString}")
    }

    CONTINUE
  }

  /**
   * Performs actions when visiting a file
   *
   * @param file `Path` of file to visit
   * @param attr `BasicFileAttributes` of file
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def visitFile(file: Path, attr: BasicFileAttributes): FileVisitResult = {
    Deidentifier.deidentifyDicomFile(file, attr)
    logger.info(s"Deidentify ${file.toString}")

    CONTINUE
  }

  /**
   * Performs actions when visiting a file fails
   *
   * @param file `File` whose visit failed
   * @param e    `IOException` thrown by failed file visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def visitFileFailed(file: Path, e: IOException): FileVisitResult = {
    logger.error(s"visitFileFailed=${file.toString}")

    CONTINUE
  }
}

/**
 * Companion object for Deidentifier class
 */
object Deidentifier {

  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[Deidentifier])

  /**
   * Deidentify `AttributeList` of DICOM file via helper functions
   *
   * @param sourceDicomFile `Path` of source DICOM file
   * @param attr            `BasicFileAttributes` of source DICOM file
   */
  def deidentifyDicomFile(
                           sourceDicomFile: Path,
                           attr: BasicFileAttributes): Unit = {
    // 1 Get DICOM targetFile AttributeList
    val attrList = Deidentifier.getAttributeListFromPath(sourceDicomFile)

    // 2 Reformat PatientID string: hlp17umm01234 => UM00001234
    Deidentifier.reformatPatientId(sourceDicomFile, attrList)

    // 3 Replace/remove private DICOM elements/attributes
    Deidentifier.replacePrivateDicomElements(sourceDicomFile, attrList)
    Deidentifier.removePrivateElements(sourceDicomFile, attrList)

    // 4 Write deidentified AttributeList object to file
    Deidentifier.writeAttributeListToFile(sourceDicomFile, attrList)
  }

  /**
   * Extracts DICOM `AttributeList` from a file `Path` object
   *
   * @param dicomFile `Path` of DICOM file
   * @return `String` of DICOM sequence series description
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
   * @param dicomFile `Path` of DICOM file
   * @param attrList  `AttributeList` from DICOM file
   */
  private def reformatPatientId(dicomFile: Path, attrList: AttributeList): Unit = {
    val idPrefixStrings: List[String] = PackageConfig.idPrefixStringArray.toList
    val idPrefixRegexes: List[Regex] = idPrefixStrings.map(new Regex(_))
    val idPrefixRegex = new Regex(idPrefixRegexes.mkString("|"))
    val idStrings = idPrefixRegexes.map(_.toString + "\\d{5}$")
    val idString = idStrings.mkString("|")
    val patientIdBefore: String =
      Attribute.getDelimitedStringValuesOrEmptyString(attrList, TagFromName.PatientID)

    if (patientIdBefore.matches(idString)) {
      val patientIdAfter: String = idPrefixRegex.replaceFirstIn(patientIdBefore, "UM000")
      attrList.replaceWithValueIfPresent(TagFromName.PatientID, patientIdAfter)
    } else {
      throw new Exception(s"PatientID $patientIdBefore in ${dicomFile.toString} does not match expected format")
    }
  }

  /**
   * Retrieves `AttributeTag` from DICOM attribute name string
   *
   * @param tagString DICOM attribute name
   * @return `AttributeTag` corresponding to `tagString`
   */
  private def fromStringToAttributeTag(tagString: String): AttributeTag = {
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
   * @param attrList  `AttributeList` object from DICOM file
   */
  private def replacePrivateDicomElements(dicomFile: Path, attrList: AttributeList): Unit = {
    val dicomAttributeTagsToReplaceWithZero: List[AttributeTag] =
      DeidentifyConfig.dicomAttributesToReplaceWithZero.map(fromStringToAttributeTag)

    dicomAttributeTagsToReplaceWithZero.foreach(attrList.replaceWithZeroLengthIfPresent)
  }

  /**
   * Remove all private attributes/elements from `AttributeList` object
   *
   * @param dicomFile `Path` of copied DICOM file that needs to be deidentified
   * @param attrList  `AttributeList` object from DICOM file
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
   * Write `AttributeList` object to `Path` object
   *
   * @param dicomFile `Path` of DICOM file
   * @param attrList  `AttributeList` object from source DICOM file
   */
  private def writeAttributeListToFile(
                                        dicomFile: Path,
                                        attrList: AttributeList): Unit = {
    // 1 Write attrList to DicomOutputStream
    val transferSyntaxUID: String = attrList
      .get(TagFromName.TransferSyntaxUID)
      .getDelimitedStringValuesOrEmptyString // https://www.dicomlibrary.com/dicom/transfer-syntax/
    val bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(dicomFile.toString))

    // 2 Write edited AttributeList object (attrList) to BufferedFileOutputStream object (bufferedOutputStream)
    try {
      // https://www.dclunie.com/pixelmed/software/javadoc/com/pixelmed/dicom/ +
      //   AttributeList.html#write-java.io.OutputStream-java.lang.String-boolean-boolean-boolean-
      attrList.write(bufferedOutputStream, transferSyntaxUID, true, true, true)
    }
    catch {
      case e: DicomException =>
        logger.error(s"writeAttributeListToFile(${dicomFile.toString}) DicomExc: $e")
      case e: IOException =>
        logger.error(s"writeAttributeListToFile(${dicomFile.toString}) IOExc: $e")
    }
    finally {
      if (bufferedOutputStream != null)
        bufferedOutputStream.close()
      if (attrList != null)
        attrList.clear()
    }
  }

}