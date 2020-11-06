package edu.umich.med.alzheimers.dicom.copy

import java.io.IOException
import java.nio.file.attribute.{BasicFileAttributes, FileTime}
import java.nio.file.FileVisitResult._
import java.nio.file.{CopyOption, DirectoryNotEmptyException, FileAlreadyExistsException}
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}

import org.slf4j.{Logger, LoggerFactory}

/**
 * Directory and DICOM file copier implementing Java `FileVisitor`
 *
 * @param sourceDirPath `Path` of source directory
 * @param targetDirPath `Path` of target directory
 * @param copyOptions   `Seq` of possible `REPLACE_EXISTING`, `COPY_ATTRIBUTES`, `NOFOLLOW_LINK`
 */
class Copier(
              val sourceDirPath: Path,
              val targetDirPath: Path,
              val copyOptions: Seq[CopyOption])
  extends FileVisitor[Path] {

  /** Logger */
  private def logger: Logger = Copier.logger

  /**
   * Performs actions before visiting a directory
   *
   * @param dir   `Path` of directory to act on before visit
   * @param attrs `BasicFileAttributes` of `dir`
   * @return `FileVisitResult` to `CONTINUE` or `SKIP_SUBTREE`
   */
  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
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
   * @param dir `Path` of directory to act on after visit
   * @param exc `IOException` thrown from directory visit
   * @return `FileVisitResult` to CONTINUE
   */
  override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
    if (exc == null) {
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
   * Performs actions when visiting a file
   *
   * @param file  `Path` of file to visit
   * @param attrs `BasicFileAttributes` of file
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    val target = targetDirPath.resolve(sourceDirPath.relativize(file))

    Copier.copyFile(file, target, copyOptions)
    logger.info(s"Copy ${file.toString} to ${target.toString}")

    CONTINUE
  }

  /**
   * Performs actions when visiting a file fails
   *
   * @param file `Path` of file whose visit failed
   * @param exc  `IOException` thrown by failed file visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
    if (exc != null) {
      logger.error(s"Unable to copy: visitFileFailed(${file.toString}): ${exc}")
    }

    CONTINUE
  }
}

/**
 * Companion object for `Copier` class
 */
object Copier {

  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[Copier])

  /**
   * Copies file from source to target
   *
   * @param source      `Path` of source file to copy
   * @param target      `Path` of target where source is to be copied
   * @param copyOptions `Seq` of possible `REPLACE_EXISTING`, `COPY_ATTRIBUTES`, `NOFOLLOW_LINK`
   */
  def copyFile(source: Path, target: Path, copyOptions: Seq[CopyOption]): Unit = {
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

}