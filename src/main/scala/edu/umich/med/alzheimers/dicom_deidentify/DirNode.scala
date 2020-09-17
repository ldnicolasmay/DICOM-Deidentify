package edu.umich.med.alzheimers.dicom_deidentify

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{DirectoryStream, Files, Path}
import scala.util.{Failure, Success, Try, Using}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import org.slf4j.{Logger, LoggerFactory}
// import org.zeroturnaround.zip.{NameMapper, ZipUtil}


/**
 * Representation of directories in file system, a hierarchical tree structure
 *
 * @param dirPath           `Path` of directory
 * @param depth             `Int` depth of `DirNode` object within its `DirNode` tree
 * @param childDirNodes     List of child DirNode objects
 * @param childFileNodes    List of child FileNode objects
 * @param intermedDirsRegex String regex of intermediate directories leading to or containing DICOM files
 * @param dicomFileRegex    String regex of DICOM file names
 */
case class DirNode(
                    dirPath: Path,
                    depth: Int,
                    childDirNodes: List[DirNode],
                    childFileNodes: List[FileNode],
                    intermedDirsRegex: String,
                    dicomFileRegex: String)
  extends Node {

  /** Logger */
  private def logger: Logger = DirNode.logger

  /**
   * Returns hierarchical representation of this DirNode tree as string
   *
   * @return String of DirNode tree represented hierarchically
   */
  override def toString: String = {
    s"$depth ${dirPath.toString}" + "\n" +
      childDirNodes.map(_.toString) + "\n" +
      childFileNodes.map(_.toString) + "\n"
  }

  /* Methods that return primitives */

  /**
   * Counts number of child Node objects beneath this DirNode object
   *
   * @return Int of Node count
   */
  def countSubNodes(): Int = {
    val nodeCountInChildDirs = childDirNodes.map(_.countSubNodes()).sum

    childDirNodes.length + childFileNodes.length + nodeCountInChildDirs
  }

  /**
   * Gets length of a this DirNode's path iterator, effectively a count of the directories in this path
   *
   * For example, DirNode("/foo/bar").getPathLength returns 2
   *
   * @return Int length of this DirNode object's iterator
   */
  override def getPathLength: Int = dirPath.iterator().asScala.length

  /**
   * Gets path index of the directory or file name String passed to method
   *
   * For example, DirNode("/foo/bar").getSubpathIndexOf("foo") returns 0
   *
   * @param name String name of directory or file to get the path index of
   * @return Int index of directory or file
   */
  override def getSubpathIndexOf(name: String): Int = {
    val dirNodeFileNameSeq: Seq[String] = nodePathSeq(dirPath)

    dirNodeFileNameSeq.indexOf(name)
  }

  /* Methods that return DirNode */

  /**
   * Filters the child DirNode objects of this DirNode object based on passed predicate
   *
   * @param predicate Function that accepts a DirNode object and returns a Boolean
   * @return DirNode object filtered
   */
  def filterChildDirNodesWith(predicate: DirNode => Boolean): DirNode = {
    val filteredChildDirs = childDirNodes
      .map(_.filterChildDirNodesWith(predicate))
      .filter(predicate)

    this.copy(dirPath, depth, filteredChildDirs, childFileNodes)
  }

  /**
   * Not-filters the child DirNode objects of this DirNode object based on passed predicate
   *
   * @param predicate Function that accepts a DirNode object and returns a Boolean
   * @return DirNode object filtered
   */
  def filterNotChildDirNodesWith(predicate: DirNode => Boolean): DirNode = {
    val filteredChildDirs = childDirNodes
      .map(_.filterChildDirNodesWith(predicate))
      .filterNot(predicate)

    this.copy(dirPath, depth, filteredChildDirs, childFileNodes)
  }

  /**
   * Filters the child FileNode objects of this DirNode object based on passed predicate
   *
   * @param predicate Function that accepts a FileNode object and returns a Boolean
   * @return DirNode object filtered
   */
  def filterChildFileNodesWith(predicate: FileNode => Boolean): DirNode = {
    val filteredChildFiles = childFileNodes.filter(predicate)
    val filteredChildDirs = childDirNodes.map(_.filterChildFileNodesWith(predicate))

    this.copy(dirPath, depth, filteredChildDirs, filteredChildFiles)
  }

  /**
   * Not-filters the child FileNode objects of this DirNode object based on passed predicate
   *
   * @param predicate Function that accepts a FileNode object and returns a Boolean
   * @return DirNode object filtered
   */
  def filterNotChildFileNodesWith(predicate: FileNode => Boolean): DirNode = {
    val filteredChildFiles = childFileNodes.filterNot(predicate)
    val filteredChildDirs = childDirNodes.map(_.filterNotChildFileNodesWith(predicate))

    this.copy(dirPath, depth, filteredChildDirs, filteredChildFiles)
  }

  /**
   * Substitutes a path string for this DirNode object's path string
   *
   * For example,
   * DirNode("/target/path/file.txt").substituteRootNodeName("/target/path/file.txt", "/source/path/file.txt")
   * returns DirNode("/source/path/file.txt")
   *
   * @param oldName Old path String to substitute
   * @param newName New path String to use
   * @return DirNode object with new substituted path
   */
  override def substituteRootNodeName(oldName: String, newName: String): DirNode = {
    val nameIndex: Int = getSubpathIndexOf(oldName)
    val pathLength: Int = getPathLength

    val newPath: Path =
      if (pathLength <= nameIndex + 1) {
        dirPath.getRoot.resolve(
          dirPath
            .subpath(0, nameIndex)
            .resolve(newName)
        )
      } else {
        dirPath.getRoot.resolve(
          dirPath
            .subpath(0, nameIndex)
            .resolve(newName)
            .resolve(dirPath.subpath(nameIndex + 1, pathLength))
        )
      }

    val newChildFileNodes: List[FileNode] =
      childFileNodes.map(_.substituteRootNodeName(oldName, newName))
    val newChildDirNodes: List[DirNode] =
      childDirNodes.map(_.substituteRootNodeName(oldName, newName))

    DirNode(newPath, depth, newChildDirNodes, newChildFileNodes, intermedDirsRegex, dicomFileRegex)
  }

  /** Methods that return Option-type objects */

  /**
   * Returns child DirNode of this DirNode whose path string matches passed `dirPathString`
   *
   * @param dirPathString String path to match DirNode object in this node tree
   * @return DirNode object of interest
   */
  def findDirNode(dirPathString: String): Option[DirNode] = {
    childDirNodes.find(_.dirPath.toString == dirPathString)
  }

  /**
   * Returns child FileNode of this DirNode whose path string matches passed `filePathString`
   *
   * @param filePathString String path to match FileNode object in this DirNode tree
   * @return FileNode object of interest
   */
  def findFileNode(filePathString: String): Option[FileNode] = {
    childFileNodes.find(_.filePath.toString == filePathString)
  }

  /* Methods that return Unit */

  /**
   * Prints hierarchical representation of this DirNode tree
   */
  override def printNode(): Unit = {
    println(s"${"  " * depth}$depth ${dirPath.toString}")

    childDirNodes.foreach(_.printNode())
    childFileNodes.foreach(_.printNode())
  }

  /**
   * Copies this DirNode tree's directories and files using passed FileCopier object
   *
   * @param fileCopier FileCopier object with requisite source path, target path, copy options, verbose flag
   */
  override def copyNode(fileCopier: FileCopier): Unit = {
    val file = dirPath
    val attr = Files.readAttributes(file, classOf[BasicFileAttributes])
    var exc: IOException = null

    try {
      fileCopier.preVisitDirectory(file, attr)
    }
    catch {
      case e: IOException =>
        exc = e
        logger.error(e.toString)
    }

    childDirNodes.foreach(_.copyNode(fileCopier))
    childFileNodes.foreach(_.copyNode(fileCopier))

    fileCopier.postVisitDirectory(file, exc)
  }

  //  /**
  //   * Zip directories at user-defined depth of this DirNode tree
  //   *
  //   * @param zipDepth Depth in this DirNode tree to zip directories
  //   * @param verbose  Boolean flag for verbose printing
  //   */
  //  def zipNodesAtDepth(zipDepth: Int, verbose: Boolean): Unit = {
  //
  //    if (this.depth <= zipDepth) {
  //      if (this.depth < zipDepth) {
  //        // recurse down
  //        childDirNodes.foreach(_.zipNodesAtDepth(zipDepth, verbose))
  //      }
  //      else {
  //        // zip this dir
  //        if (verbose) println(s"Zipping ${dirPath.toString} @ depth $depth")
  //        val zipPath: Path = Paths.get(dirPath.toString + ".zip")
  //        ZipUtil.pack(
  //          dirPath.toFile,
  //          zipPath.toFile,
  //          new NameMapper() {
  //            override def map(name: String): String = dirPath.getFileName.toString + "/" + name
  //          }
  //        )
  //      }
  //    }
  //    else ()
  //  }

}

/**
 * Companion object for DirNode class
 */
object DirNode {

  /** Logger */
  private val logger: Logger = LoggerFactory.getLogger(classOf[DirNode])

  /**
   * Apply method to build DirNode object from partial parameters
   *
   * @param dirPath           Path of directory
   * @param depth             Int depth of DirNode object in Node tree containing it
   * @param intermedDirsRegex String regex of intermediate directories leading to or containing DICOM files
   * @param dicomFileRegex    String regex of DICOM file names
   * @return DirNode object
   */
  def apply(dirPath: Path, depth: Int, intermedDirsRegex: String, dicomFileRegex: String ): DirNode = {

    /** Implement DirectoryStream.Filter interface; Facilitates filtering of directories and DICOM files */
    class DirectoryDicomFileFilter extends DirectoryStream.Filter[Path] {
      override def accept(entry: Path): Boolean = {
        try {
          (Files.isDirectory(entry) && entry.getFileName.toString.matches(intermedDirsRegex)) ||
            (Files.isRegularFile(entry) && entry.getFileName.toString.matches(dicomFileRegex))
        }
        catch {
          case e: IOException =>
            logger.error(e.toString)
            false
        }
      }
    }

    // val directoryDicomFileFilter = new DirectoryDicomFileFilter()

    val children: Try[(List[DirNode], List[FileNode])] =
      Using.Manager {
        use =>
          // Use one Java Directory Stream
          val directoryDicomFileFilter = new DirectoryDicomFileFilter()
          val dirStream: DirectoryStream[Path] = use(Files.newDirectoryStream(dirPath, directoryDicomFileFilter))

          // Partition DirectoryStream into tuple of Path Iterators
          val partitionedDirStream: (Iterator[Path], Iterator[Path]) = dirStream
            .iterator()
            .asScala
            .partition(Files.isDirectory(_))

          // Get the child directory Path Iterator from the tuple; Map each directory Path into a DirNode
          val childDirNodesIt: Iterator[DirNode] = partitionedDirStream._1
            .map(DirNode(_, depth + 1, intermedDirsRegex, dicomFileRegex))

          // Get the child file Path iterator from the tuple; Map each file Path into a FileNode
          val childFileNodesIt: Iterator[FileNode] = partitionedDirStream._2
            .map(FileNode(_, depth + 1))

          // Convert children iterators to List at last possible moment
          (childDirNodesIt.toList, childFileNodesIt.toList)
      }

    val derivedChildDirNodes: List[DirNode] = children match {
      case Success(c) => c._1
      case Failure(_) => List()
    }

    val derivedChildFileNodes: List[FileNode] = children match {
      case Success(c) => c._2
      case Failure(_) => List()
    }

    this (dirPath, depth, derivedChildDirNodes, derivedChildFileNodes, intermedDirsRegex, dicomFileRegex)
  }

}