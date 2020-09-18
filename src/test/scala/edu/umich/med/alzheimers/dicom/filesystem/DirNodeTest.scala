package edu.umich.med.alzheimers.dicom.filesystem

import java.nio.file.{Path, Paths}

import edu.umich.med.alzheimers.dicom.filesystem.{DirNode, FileNode}
import edu.umich.med.alzheimers.dicom._
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.{Logger, LoggerFactory}

/**
 * Test DICOM files publicly available at http://www.pcir.org/researchers/downloads_available.html
 * Download at http://www.pcir.org/researchers/98890234_20030505_MR.html
 */

class DirNodeTest extends AnyFunSuite {

  /** Logger */
  private def logger: Logger = LoggerFactory.getLogger(classOf[DirNodeTest])

  val origDirStr: String = "/Users/ldmay/IdeaProjects/UmmapMriDeidentify/dicom/orig"
  val oneEmptyDirStr: String = "/Users/ldmay/IdeaProjects/UmmapMriDeidentify/dicom/one_empty"
  val origDirPath: Path = Paths.get(origDirStr)
  val oneEmptyPath: Path = Paths.get(oneEmptyDirStr)
  logger.info(s"origDirStr=${origDirStr}")
  logger.info(s"oneEmptyDirStr=${oneEmptyDirStr}")

  /** *****************************
   * Methods that return primitives
   */

  // countSubNode
  test("MR1 directory should have 3 subnodes") {
    val sourceDirStr: String = origDirStr + "/98890234_20030505_MR/98890234/20030505/MR/MR1"
    val sourceDirPath: Path = Paths.get(sourceDirStr)
    val sourceDirNode: DirNode = DirNode.apply(sourceDirPath, 0, """^MR\d{1,3}$""", """^\d{4,5}$""")
    // assert
    assert(sourceDirNode.countSubNodes() === 3)
  }
  test("MR2 directory should have 7 subnodes") {
    val sourceDirStr: String = origDirStr + "/98890234_20030505_MR/98890234/20030505/MR/MR2"
    val sourceDirPath: Path = Paths.get(sourceDirStr)
    val sourceDirNode: DirNode = DirNode.apply(sourceDirPath, 0, """^MR\d{1,3}$""", """^\d{4,5}$""")
    // assert
    assert(sourceDirNode.countSubNodes() === 7)
  }

  // getPathLength
  test("getPathLength on origDirNode should return 6") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    // assert
    assert(origDirNode.getPathLength === 6)
  }

  // getSubpathIndexOf
  test("getSubpathIndexOf(\"orig\") should return 5") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    assert(origDirNode.getSubpathIndexOf("orig") === 5)
  }

  /** *****************************
   * Methods that return DirNode
   */

  // filterChildDirNodesWith(nonemptyDirNodesFilter)
  test("nonemptyDirNodesFilter should be filter out empty directory") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val oneEmptyDirNode: DirNode =
      DirNode.apply(
        oneEmptyPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$|^empty$""",
        dicomFileRegex = """^\d{4,5}$""")
    // assert
    assert {
      origDirNode.toString ==
        oneEmptyDirNode
          .filterChildDirNodesWith(nonemptyDirNodesFilter)
          .substituteRootNodeName(
            oneEmptyDirNode.dirPath.getFileName.toString,
            origDirNode.dirPath.getFileName.toString
          )
          .toString
    }
  }

  // filterChildDirNodesWith(intermediateDirNameFilter)
  test("intermediateDirNameFilter should keep only regexed directories") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR[12]$""",
        dicomFileRegex = """^\d{4,5}$""")
    val filteredDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR1$""",
        dicomFileRegex = """^\d{4,5}$""")
    // assert
    assert {
      filteredDirNode.toString ==
        origDirNode
          .filterChildDirNodesWith(
            intermedDirNameFilter("""^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR1$""")(_)
          ).toString
    }
  }

  // filterChildDirNodesWith(numberOfFilesFilter)
  test("numberOfFilesFilter should keep only branches of tree with <= 7 files") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val filteredDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR[12]$""",
        dicomFileRegex = """^\d{4,5}$""")
    // assert
    assert {
      filteredDirNode.toString ==
        origDirNode
          .filterChildDirNodesWith(numberOfFilesFilter(7)(_))
          .toString
    }
  }

  // filterChildDirNodesWith(dicomFileFilter)
  test("dicomFileFilter should only keep branches of tree whose filename matches regex") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val filteredDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR1$""",
        dicomFileRegex = """^4919$""")
    // assert
    assert {
      filteredDirNode.toString ==
        origDirNode
          .filterChildFileNodesWith(dicomFileFilter("""^4919$"""))
          .filterChildDirNodesWith(nonemptyDirNodesFilter)
          .toString
    }
  }

  // filterChildDirNodesWith(childDirNodeExistsIn)
  test("directory MR1 should be only branch in filtered origDirNode") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val filteredDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR1$""",
        dicomFileRegex = """^\d{4,5}$""")
    // assert
    assert {
      filteredDirNode.toString ==
        origDirNode
          .filterChildDirNodesWith(childDirNodeExistsIn(filteredDirNode)(_))
          .toString
    }
  }

  // filterChildFileNodesWith(dicomFileSeriesDescripFilter)
  test("dicomFileSeriesDescripFilter should only keep branches of tree whose " +
    "SeriesDescription matches regex") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val filteredDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR6$""",
        dicomFileRegex = """^1\d{4}$""")
    // assert
    assert {
      filteredDirNode.toString ==
        origDirNode
          .filterChildFileNodesWith(dicomFileSeriesDescripFilter("""^SAG T2 FSE$"""))
          .filterChildDirNodesWith(nonemptyDirNodesFilter)
          .toString
    }
  }

  // filterChildFileNodesWith(childFileNodeExistsIn)
  test("DICOM file '4919' should be only branch in filtered origDirNode") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val filteredDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR1$""",
        dicomFileRegex = """^4919$""")
    // assert
    assert {
      filteredDirNode.toString ==
        origDirNode
          .filterChildFileNodesWith(childFileNodeExistsIn(filteredDirNode)(_))
          .filterChildDirNodesWith(nonemptyDirNodesFilter)
          .toString
    }
  }

  // substituteRootNodeName
  test("substituteRootNodeName applied to ./dicom/one_empty should yield ./dicom/orig") {
    val origDirNode: DirNode =
      DirNode.apply(
        origDirPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    val oneEmptyDirNode: DirNode =
      DirNode.apply(
        oneEmptyPath,
        depth = 0,
        intermedDirsRegex = """^98890234_20030505_MR$|^98890234$|^20030505$|^MR$|^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$""")
    // assert
    assert {
      origDirNode.toString.equals(
        oneEmptyDirNode
          .substituteRootNodeName(
            oneEmptyDirNode.dirPath.getFileName.toString,
            origDirNode.dirPath.getFileName.toString
          ).toString
      )
    }
  }

  /** *****************************
   * Methods that return Option[...]
   */

  // findDirNode
  test("findDirNode ...") {
    val mrDirStr: String =
      "/Users/ldmay/IdeaProjects/UmmapMriDeidentify/dicom/orig/98890234_20030505_MR/98890234/20030505/MR"
    val mrDirPath: Path = Paths.get(mrDirStr)
    val mrDirNode: DirNode =
      DirNode.apply(
        mrDirPath,
        depth = 0,
        intermedDirsRegex = """^MR\d{1,3}$""",
        dicomFileRegex = """^\d{4,5}$"""
      )
    val mr1DirStr: String =
      "/Users/ldmay/IdeaProjects/UmmapMriDeidentify/dicom/orig/98890234_20030505_MR/98890234/20030505/MR/MR1"
    val mr1DirPath: Path = Paths.get(mr1DirStr)
    val mr1DirNode: DirNode =
      DirNode.apply(
        mr1DirPath,
        depth = 1,
        intermedDirsRegex = "",
        dicomFileRegex = """^\d{4,5}$"""
      )
    val foundDirNode: Any = mrDirNode.findDirNode(mr1DirStr) match {
      case Some(dn) => dn
      case None => ()
    }
    // assert
    assert {
      mr1DirNode.toString == foundDirNode.toString
    }
  }

  // findFileNode
  test("findFileNode ...") {
    val mr1DirStr: String =
      "/Users/ldmay/IdeaProjects/UmmapMriDeidentify/dicom/orig/98890234_20030505_MR/98890234/20030505/MR/MR1"
    val mr1DirPath: Path = Paths.get(mr1DirStr)
    val mr1DirNode: DirNode =
      DirNode.apply(
        mr1DirPath,
        depth = 0,
        intermedDirsRegex = "",
        dicomFileRegex = """^\d{4,5}$"""
      )
    val mr1_4919_FileStr: String = mr1DirStr + "/4919"
    val mr1_4919_FilePath: Path = Paths.get(mr1_4919_FileStr)
    val mr1_4919_FileNode: FileNode =
      FileNode.apply(
        mr1_4919_FilePath,
        depth = 1
      )
    val foundFileNode: Any = mr1DirNode.findFileNode(mr1_4919_FileStr) match {
      case Some(fn) => fn
      case None => ()
    }
    // assert
    assert {
      mr1_4919_FileNode.toString == foundFileNode.toString
    }
  }

}
