# DICOM-Deidentify


## About

This app helps process DICOM files for deidentification. A source directory tree containing DICOM files is filtered for specific directories (by name and number of files within) and DICOM files (by name and `SeriesDescription` attribute value). That source directory tree is then copied to a target directory, the target tree DICOM files are deidentified, and finally the target tree is zipped at a specific tree depth. To minimize file I/O, source directories and files are only copied if they do not already exist in the target directory tree. 

There is one class for performing all three steps (copy, deidentify, zip): `edu.umich.med.alzheimers.dicom.CopyDeidentifyZip`. There are also classes in subpackages of `edu.umich.med.alzheimers.dicom` for performing each of the three steps in isolation: `...dicom.copy.Copy`, `...dicom.deidentify.Deidentify`, `...dicom.zip.Zip`. 

A firm grasp of regular expressions is required to configure and use this app effectively. ([RegexOne](https://regexone.com/) offers a good tutorial on regular expressions.)


## Getting Started

### Prerequisites

1. If you haven't already, install Java JDK 8.

2. To recompile/repackage the JARs or run the tests, install [sbt](https://www.scala-sbt.org/index.html). You can install sbt from [here](https://www.scala-sbt.org/release/docs/Setup.html).

### Installing

1. Navigate to a directory that will be the parent directory of this app.

2. Clone this repository with HTTPS or SSH. HTTPS will probably be much easier.

   * HTTPS Clone
   
   ```shell script
   git clone https://git.umms.med.umich.edu/michiganadc/dicom-deidentify.git
   ```

   * SSH Clone

   ```shell script
   git clone git@git.umms.med.umich.edu:michiganadc/dicom-deidentify.git
   ```

3. `cd` into the newly created directory:

   ```shell script
   cd ./dicom-deidentify
   ```
   
4. Using sbt, make sure the app compiles:

   ```shell script
   sbt compile
   ```


## Running the Tests

Run `sbt test` from the command line: 

```sbtshell
sbt test
```

**Note**: This will return failed tests until the app is properly configured.

## Usage

### Configuration

There are three config files, one for each step in the deidentification process: copy, deidentify, and zip. All three can be found at `src/main/resources/config/`.

##### 1. Configuring `copy.conf`

`sourceDirPathStr`: Path string of the source directory that contains all the DICOM files you want to copy.

`targetDirPathStr`: Path string of the target directory that the DICOM files and their directory tree will be copied to.

`intermedDirsRegexArray`: Array of regular expression strings for directory names that will be included in the source and target directory trees.

`dicomFilenameRegexArray`: Array of regular expression strings for acceptable DICOM filenames to copy.

`seriesDescriptionRegexArray`: Array of regular expression strings for acceptable DICOM file `SeriesDescription` attribute values to copy.

##### 2. Configuring `deidentify.conf`

`sourceDirPathStr`: Path string of the target directory that the DICOM files and their directory tree will be copied to.

`dicomAttributesToReplaceWithZero`: Array of strings for DICOM attributes whose values will be set to empty strings.

##### 3. Configuring `zip.conf`

`sourceDirPathStr`: Path string of the source directory that contains the directories or DICOM files that will be zipped.

`targetDirPathStr`: Path string of the target directory that the zipped directories or zipped DICOM files will be placed in. Note that the directory tree above any zipped directory or DICOM file is preserved.

### Package JAR with sbt

Now that you've got the app configured, package it into a JAR file using sbt:

```shell script
sbt package
```

This will build a JAR file, `target/scala-2.13/dicom-deidentify_2.13-0.1.jar`.

### Running sbt-packaged JAR

To run the sbt package of the app JAR from the command line, you will need to define the path of some dependency JARs in your shell. Then you can pass the dependency JAR paths to the Java classpath option.

First, define the path of the dependency JARs:

   ```shell script
   SCALA="lib/scala-library-2.13.3.jar"         && \
     PIXELMED="lib/pixelmed.jar"                && \
     LB_CORE="lib/logback-core-1.2.3.jar"       && \
     LB_CLASSIC="lib/logback-classic-1.2.3.jar" && \
     SLF4J="lib/slf4j-api-1.7.30.jar"           && \
     PICOCLI="lib/picocli-4.2.0.jar"            && \
     CONFIG="lib/config-1.3.0.jar"              && \
     ZIP="lib/zt-zip-1.14.jar"                  && \
     APP="target/scala-2.13/dicom-deidenitfy_2.13-0.1.jar"
   ```

#### Copy + Deidentify + Zip

Run the sbt-packaged JAR (defined at `APP="target/scala-2.13/dicom-deidentify_2.13-0.1.jar` above),  passing both the dependency JARs to the classpath option, and the class that contains the main method for doing all three steps: `CopyDeidentifyZip`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$ZIP:$APP \
     edu.umich.med.alzheimers.dicom.CopyDeidentifyZip
   ```

#### Copy (only)

Run the sbt-packaged JAR, passing both the dependency JARS to the classpath option, and the class that contains the main method for copying only: `Copy`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$ZIP:$APP \
     edu.umich.med.alzheimers.dicom.copy.Copy
   ```

Note that the `Copy` class is in the subpackage `copy` of the `edu.umich.med.alzheimers.dicom` package.

#### Deidentify (only)

Same as Copy above, but pass the class that contains the main method for deidentifying only: `Deidentify`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$ZIP:$APP \
     edu.umich.med.alzheimers.dicom.deidentify.Deidentify
   ```

#### Zip (only)

Same as Copy above, but pass the class that contains the main method for zipping only: `Zip`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$ZIP:$APP \
     edu.umich.med.alzheimers.dicom.zip.Zip
   ```


## Built Using

* [Scala 2.13](https://www.scala-lang.org/)
* [PixelMed Java DICOM Toolkit](https://www.pixelmed.com/dicomtoolkit.html)
* [Logback](http://logback.qos.ch/)
* [picocli](https://picocli.info/)
* [Zeroturnaround Zip](https://github.com/zeroturnaround/zt-zip)


## Authors

[@LDNicolasMay](https://github.com/ldnicolasmay) - Idea and work.


## Acknowledgments

Thanks to Dr. David A. Clunie for his [PixelMed Java DICOM Toolkit](https://www.pixelmed.com/dicomtoolkit.html).

Thanks also to Saravanan Subramainian for his [DICOM Tutorials](https://saravanansubramanian.com/dicomtutorials/).
