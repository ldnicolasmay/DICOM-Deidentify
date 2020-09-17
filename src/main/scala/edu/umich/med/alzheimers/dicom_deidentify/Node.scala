package edu.umich.med.alzheimers.dicom_deidentify

import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Implemented by `DirNode` and `FileNode`
 */
abstract class Node {

  /**
   * Print hierarchical representation of Node tree
   */
  def printNode(): Unit

  /**
   * Copy Node tree's directories and files using passed FileCopier object; side effects only
   *
   * @param fileCopier FileCopier object with requisite source path, target path, copy options, verbose flag
   */
  def copyNode(fileCopier: FileCopier): Unit

  /**
   * Get path index of the directory or file name String passed to method
   *
   * @param name String name of directory or file to get the path index of
   * @return Int index of directory or file
   */
  def getSubpathIndexOf(name: String): Int

  /**
   * Get length of a Node's path iterator, effectively a count of the directories (and file) in this path
   *
   * @return Int length of Node object's iterator
   */
  def getPathLength: Int

  /**
   * Substitute a path string for this Node object's path string
   *
   * @param oldName Old path String to substitute
   * @param newName New path String to use
   * @return Node object with new substituted path
   */
  def substituteRootNodeName(oldName: String, newName: String): Node

  /**
   * Returns the path to a DirNode or FileNode as a sequence of strings;
   * DirNode path example: /path/to/dir => Seq("path", "to", "dir");
   * FileNode path eample: /path/to/file.txt => Seq("path", "to", "file.txt")
   *
   * @param nodePath Path to node
   * @return Path as a sequence of strings
   */
  def nodePathSeq(nodePath: Path): Seq[String] = nodePath
    .iterator()
    .asScala
    .map(_.getFileName.toString)
    .toSeq
}