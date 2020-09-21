package edu.umich.med.alzheimers.dicom.zip

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult.{CONTINUE, SKIP_SIBLINGS, SKIP_SUBTREE}
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path, Paths}

import org.slf4j.{Logger, LoggerFactory}
import org.zeroturnaround.zip.{NameMapper, ZipUtil}

/**
 * Directory or DICOM file zipper implementing Java `FileVisitor`
 *
 * @param sourceDirPath `Path` of source directory
 * @param targetDirPath `Path` of target directory
 * @param nodeDepth     `Int` depth of this node in its `DirNode` tree
 * @param zipDepth      `Int` depth at which to zip files or directories
 */
class Zipper(
              val sourceDirPath: Path,
              val targetDirPath: Path,
              val nodeDepth: Int,
              val zipDepth: Int)
  extends FileVisitor[Path] {

  /** Logger */
  private def logger: Logger = Zipper.logger

  /**
   * Performs actions before visiting a directory
   *
   * @param dir  `Path` of directory to act on before visit
   * @param attr `BasicFileAttributes` of `dir`
   * @return `FileVisitResult` to `CONTINUE` or `SKIP_SUBTREE`
   */
  override def preVisitDirectory(dir: Path, attr: BasicFileAttributes): FileVisitResult = {
    val target: Path = targetDirPath.resolve(sourceDirPath.relativize(dir))

    if (nodeDepth < zipDepth) {
      logger.info(s"Create directory ${target.toString} with nodeDepth=${nodeDepth}")
      Files.createDirectory(target)
      CONTINUE
    } else if (nodeDepth == zipDepth) {
      logger.info(s"Zip ${dir.toString} to ${target.toString} with nodeDepth=${nodeDepth}, zipDepth=${zipDepth}")
      Zipper.zipDirectory(dir, attr, target)
      SKIP_SUBTREE
    } else {
      SKIP_SIBLINGS
    }
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
      logger.error(s"Error during zip of ${dir.toString}")
    }

    CONTINUE
  }

  /**
   * Performs actions when visiting a file
   *
   * @param file `Path` of file to visit
   * @param attr `BasicFileAttributes` of file
   * @return `FileVisitResult` to `CONTINUE`, `SKIP_SUBTREE`, `SKIP_SIBLINGS`
   */
  override def visitFile(file: Path, attr: BasicFileAttributes): FileVisitResult = {
    val target = targetDirPath.resolve(sourceDirPath.relativize(file))

    if (nodeDepth < zipDepth) {
      Files.createDirectory(target)
      CONTINUE
    } else if (nodeDepth == zipDepth) {
      logger.info(s"Zip ${file.toString} to ${target.toString} with depth ${nodeDepth} @ depth ${zipDepth}")
      Zipper.zipFile(file, attr, target)
      SKIP_SUBTREE
    } else {
      SKIP_SIBLINGS
    }
  }

  /**
   * Performs actions when visiting a file fails
   *
   * @param file `Path` of file whose visit failed
   * @param e    `IOException` thrown by failed file visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def visitFileFailed(file: Path, e: IOException): FileVisitResult = {
    logger.error(s"visitFileFailed=${file.toString}")

    CONTINUE
  }

}

/**
 * Companion object for Zipper class
 */
object Zipper {

  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[Zipper])

  /**
   * Zip directory
   *
   * @param sourceDir `Path` of source directory to zip
   * @param attr      `BasicAttributeList` of directory to zip
   * @param targetDir `Path` of target directory to place zipped directories
   */
  def zipDirectory(
                    sourceDir: Path,
                    attr: BasicFileAttributes,
                    targetDir: Path): Unit = {
    val targetZipPath: Path = Paths.get(targetDir.toString + ".zip")

    ZipUtil.pack(
      sourceDir.toFile,
      targetZipPath.toFile,
      new NameMapper() {
        override def map(name: String): String = targetZipPath.getFileName.toString + "/" + name
      }
    )
  }

  /**
   * Zip file
   *
   * @param sourceFile `Path` of source file to zip
   * @param attr       `BasicAttributeList` of file to zip
   * @param targetFile `Path` of target file to place zipped files
   */

  def zipFile(
               sourceFile: Path,
               attr: BasicFileAttributes,
               targetFile: Path): Unit = {
    val targetZipPath: Path = Paths.get(targetFile.toString + ".zip")

    ZipUtil.packEntry(
      sourceFile.toFile,
      targetZipPath.toFile,
      new NameMapper() {
        override def map(name: String): String = sourceFile.getFileName.toString + "/" + name
      }
    )
  }

}