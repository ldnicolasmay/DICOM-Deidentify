package edu.umich.med.alzheimers.dicom_deidentify

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * Representation of file nodes, always leaves of `DirNode` trees
 *
 * @param filePath Path of file
 * @param depth    Int depth of FileNode object in Node tree containing it
 */
case class FileNode(filePath: Path, depth: Int) extends Node {

  /**
   * Print hierarchical representation of this FileNode
   */
  override def printNode(): Unit = {
    println(s"${"  " * depth}$depth : ${filePath.toString}")
  }

  /**
   * String hierarchical representation of this DirNode tree
   *
   * @return String of DirNode tree represented hierarchically
   */
  override def toString: String = {
    s"$depth ${filePath.toString}\n"
  }

  /**
   * Copy this FileNode file using passed FileCopier object; side effects only
   *
   * @param fileCopier FileCopier object with requisite source path, target path, copy options, verbose flag
   */
  override def copyNode(fileCopier: FileCopier): Unit = {
    val attr = Files.readAttributes(filePath, classOf[BasicFileAttributes])

    try {
      fileCopier.visitFile(filePath, attr)
    } catch {
      case e: IOException => fileCopier.visitFileFailed(filePath, e)
    }
  }

  /**
   * Get path index of the directory or file name String passed to method
   *
   * For example, FileNode("/foo/bar/baz.txt").getSubpathIndexOf("foo") returns 0
   *
   * @param name String name of directory or file to get the path index of
   * @return Int index of directory or file
   */
  override def getSubpathIndexOf(name: String): Int = {
    val fileNodeFileNameSeq: Seq[String] = nodePathSeq(filePath)

    fileNodeFileNameSeq.indexOf(name)
  }

  /**
   * Get length of a this FileNode's path iterator, effectively a count of the directories and file in this path
   *
   * For example, DirNode("/foo/bar/baz.txt").getPathLength returns 3
   *
   * @return Int length of this FileNode object's iterator
   */
  override def getPathLength: Int = {
    filePath.iterator().asScala.length
  }

  /**
   * Substitute a path string for this FileNode object's path string
   *
   * For example,
   * DirNode("/target/path/file.txt").substituteRootNodeName("/target/path/file.txt", "/source/path/file.txt")
   * returns DirNode("/source/path/file.txt")
   *
   * @param oldName Old path String to substitute
   * @param newName New path String to use
   * @return FileNode object with new substituted path
   */
  override def substituteRootNodeName(oldName: String, newName: String): FileNode = {
    val nameIndex: Int = getSubpathIndexOf(oldName)
    val newPath = filePath.getRoot.resolve(
      filePath.subpath(0, nameIndex).resolve(newName).resolve(
        filePath.subpath(nameIndex + 1, this.getPathLength)))

    FileNode(newPath, depth)
  }

}
