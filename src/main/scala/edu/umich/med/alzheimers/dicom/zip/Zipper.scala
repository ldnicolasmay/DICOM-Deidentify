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
   * @param dir   `Path` of directory to act on before visit
   * @param attrs `BasicFileAttributes` of `dir`
   * @return `FileVisitResult` to `CONTINUE`, `SKIP_SUBTREE`, or `SKIP_SIBLINGS`
   */
  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
    val target: Path = targetDirPath.resolve(sourceDirPath.relativize(dir))
    val targetExists: Boolean = Files.exists(target)

    if (nodeDepth < zipDepth) {
      if (!targetExists) {
        logger.info(s"Create directory ${target.toString} with nodeDepth=${nodeDepth}")
        Files.createDirectory(target)
      }
      CONTINUE
    }
    else if (nodeDepth == zipDepth) {
      logger.info(s"Zip ${dir.toString} to ${target.toString} with nodeDepth=${nodeDepth}, zipDepth=${zipDepth}")
      Zipper.zipDirectory(dir, attrs, target)
      SKIP_SUBTREE
    }
    else {
      SKIP_SIBLINGS
    }
  }

  /**
   * Performs actions after visiting a directory
   *
   * @param dir `Path` of directory to act on after visit
   * @param exc `IOException` thrown from directory visit
   * @return `FileVisitResult` to `CONTINUE`
   */
  override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
    if (exc != null) {
      logger.error(s"Error during zip of ${dir.toString}")
    }

    CONTINUE
  }

  /**
   * Performs actions when visiting a file
   *
   * @param file  `Path` of file to visit
   * @param attrs `BasicFileAttributes` of file
   * @return `FileVisitResult` to `CONTINUE`, `SKIP_SUBTREE`, or `SKIP_SIBLINGS`
   */
  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    val target = targetDirPath.resolve(sourceDirPath.relativize(file))

    if (nodeDepth < zipDepth) {
      Files.createDirectory(target)
      CONTINUE
    }
    else if (nodeDepth == zipDepth) {
      logger.info(s"Zip ${file.toString} to ${target.toString} with depth ${nodeDepth} @ depth ${zipDepth}")
      Zipper.zipFile(file, attrs, target)
      SKIP_SUBTREE
    }
    else {
      SKIP_SIBLINGS
    }
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
      logger.error(s"visitFileFailed=${file.toString}")
    }

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
   * @param attrs     `BasicAttributeList` of directory to zip
   * @param targetDir `Path` of target directory to place zipped directories
   */
  def zipDirectory(
                    sourceDir: Path,
                    attrs: BasicFileAttributes,
                    targetDir: Path): Unit = {
    val targetZipPath: Path = Paths.get(targetDir.toString + ".zip")

    ZipUtil.pack(
      sourceDir.toFile,
      targetZipPath.toFile,
      new NameMapper() {
        override def map(name: String): String = sourceDir.getFileName.toString + "/" + name
      }
    )
  }

  /**
   * Zip file
   *
   * @param sourceFile `Path` of source file to zip
   * @param attrs      `BasicAttributeList` of file to zip
   * @param targetFile `Path` of target file to place zipped files
   */

  def zipFile(
               sourceFile: Path,
               attrs: BasicFileAttributes,
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