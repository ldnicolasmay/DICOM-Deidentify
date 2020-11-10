package edu.umich.med.alzheimers.dicom.filesystem

import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Implemented by `DirNode` and `FileNode`
 */
abstract class Node(
                     path: Path,
                     attrs: BasicFileAttributes,
                     depth: Int
                   ) {

  /**
   * Get path of the directory or file node
   *
   * @return `Path` of directory or file node
   */
  def getPath: Path = path

  /**
   * Get attributes of directory or file node
   *
   * @return `BasicFileAttributes` of node
   */
  def getAttrs: BasicFileAttributes = attrs

  /**
   * Get depth of directory or file node
   *
   * @return `Int` of node depth
   */
  def getDepth: Int = depth

  /**
   * Predicate to determine of this `Node` tree contains passed node
   *
   * @param node `Node` that may be in this node tree
   * @return `Boolean` whether `node` exists
   */
  def hasNode(node: Node): Boolean

  /**
   * Print hierarchical representation of `Node` tree
   */
  def printNode(): Unit

  /**
   * Get path index of the directory or file name `String` passed to method
   *
   * @param name `String` name of directory or file to get the path index of
   * @return `Int` index of directory or file
   */
  def getSubpathIndexOf(name: String): Int

  /**
   * Get length of a `Node`'s path iterator, effectively a count of the directories (and file) in this path
   *
   * @return `Int` length of `Node`'s iterator
   */
  def getPathLength: Int

  /**
   * Substitute a path `String` for this `Node` object's path string
   *
   * @param oldName `String` of old path to substitute
   * @param newName `String` of new path to use
   * @return `Node` object with new substituted path
   */
  def substituteRootNodeName(oldName: String, newName: String): Node

  /**
   * <p>Returns `Path` to a `Node` as a sequence of `String`s</p>
   *
   * <p>`DirNode` path example: `/path/to/dir => Seq("path", "to", "dir")`;
   * `FileNode` path example: `/path/to/file.txt => Seq("path", "to", "file.txt")`</p>
   *
   * @param path `Path` to node
   * @return `Path` as a sequence of strings
   */
  def nodePathSeq(path: Path): Seq[String] = path
    .iterator()
    .asScala
    .map(_.getFileName.toString)
    .toSeq
}