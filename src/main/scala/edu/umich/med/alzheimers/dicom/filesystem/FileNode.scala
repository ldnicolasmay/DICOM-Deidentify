package edu.umich.med.alzheimers.dicom.filesystem

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

import edu.umich.med.alzheimers.dicom.copy.Copier
import edu.umich.med.alzheimers.dicom.deidentify.Deidentifier
import edu.umich.med.alzheimers.dicom.upload.Uploader
import edu.umich.med.alzheimers.dicom.zip.Zipper
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Representation of file nodes, always leaves of `DirNode` trees
 *
 * @param path  `Path` of file
 * @param attrs `BasicFileAttributes` object of file
 * @param depth `Int` depth of `FileNode` object in `DirNode` tree containing it
 */
case class FileNode(
                     path: Path,
                     attrs: BasicFileAttributes,
                     depth: Int
                   )
  extends Node(path, attrs, depth) {

  /** Logger */
  private def logger: Logger = FileNode.logger

  /**
   * Print hierarchical representation of this `FileNode`
   */
  override def printNode(): Unit = {
    println(s"${"  " * depth}$depth : ${path.toString}")
  }

  /**
   * String hierarchical representation of this `DirNode` tree
   *
   * @return `String` of `DirNode` tree represented hierarchically
   */
  override def toString: String = {
    s"$depth ${path.toString}\n"
  }

  /**
   * Predicate to determine if this `FileNode` tree equals passed node
   *
   * @param node `Node` that may be in this node tree
   * @return `Boolean` whether `node` exists
   */
  def hasNode(node: Node): Boolean = {
    path.toString == node.getPath.toString
  }

  /**
   * Copy this `FileNode` file using passed `Copier` object
   *
   * @param copier `Copier` object with required fields
   */
  def copyNode(copier: Copier): Unit = {
    try {
      copier.visitFile(path, attrs)
    }
    catch {
      case e: IOException => copier.visitFileFailed(path, e)
    }
  }

  /**
   * Deidentify this `FileNode` file using passed `Deidentifier` object
   *
   * @param deidentifier `Deidentifier` with required fields
   */
  def deidentifyNode(deidentifier: Deidentifier): Unit = {
    try {
      deidentifier.visitFile(path, attrs)
    }
    catch {
      case e: IOException => deidentifier.visitFileFailed(path, e)
    }
  }

  /**
   * Zip this `FileNode` file using passed `Zipper` object
   *
   * @param zipper `Zipper` with required fields
   */
  def zipNode(zipper: Zipper): Unit = {
    try {
      zipper.visitFile(path, attrs)
    }
    catch {
      case e: IOException => zipper.visitFileFailed(path, e)
    }
  }

  /**
   * Upload this `FileNode` file using passed `Zipper` object
   *
   * @param uploader `Uploader` with required fields
   */
  def uploadNode(uploader: Uploader): Unit = {
    try {
      uploader.visitFile(path, attrs)
    }
    catch {
      case e: IOException => uploader.visitFileFailed(path, e)
    }
  }

  /**
   * <p>Get path index of the directory or file name `String` passed to method</p>
   *
   * <p>For example, `FileNode("/foo/bar/baz.txt").getSubpathIndexOf("foo")` returns `0`</p>
   *
   * @param name `String` name of directory or file to get the path index of
   * @return `Int` index of directory or file
   */
  override def getSubpathIndexOf(name: String): Int = {
    val fileNodeFileNameSeq: Seq[String] = nodePathSeq(path)

    fileNodeFileNameSeq.indexOf(name)
  }

  /**
   * <p>Get length of a this `FileNode``'s path iterator,
   * effectively a count of the directories and file in this path</p>
   *
   * <p>For example, `DirNode("/foo/bar/baz.txt").getPathLength` returns `3`</p>
   *
   * @return `Int` length of this `FileNode`'s iterator
   */
  override def getPathLength: Int = {
    path.iterator().asScala.length
  }

  /**
   * <p>Substitute a path string for this `FileNode`'s path string</p>
   *
   * <p>For example,
   * `DirNode("/target/path/file.txt").substituteRootNodeName("/target/path/file.txt", "/source/path/file.txt")`
   * returns `DirNode("/source/path/file.txt")`</p>
   *
   * @param oldName `String` of old path substitute
   * @param newName `String` of new path to use
   * @return `FileNode` with new substituted path
   */
  override def substituteRootNodeName(oldName: String, newName: String): FileNode = {
    val nameIndex: Int = getSubpathIndexOf(oldName)
    val newPath = path.getRoot.resolve(
      path.subpath(0, nameIndex).resolve(newName).resolve(
        path.subpath(nameIndex + 1, this.getPathLength)
      )
    )

    FileNode(newPath, depth)
  }

}

/**
 * Companion object for `FileNode` class
 */
object FileNode {
  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[FileNode])

  /**
   * Apply method to build FileNode object from partial parameters
   *
   * @param filePath `Path` of file
   * @param depth    Depth of `FileNode` in its `DirNode` tree
   * @return
   */
  def apply(filePath: Path, depth: Int): FileNode = {
    val attrs = Files.readAttributes(filePath, classOf[BasicFileAttributes])

    this (filePath, attrs, depth)
  }
}