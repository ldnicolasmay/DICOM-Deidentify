# DICOM-Deidentify


## About

This app processes DICOM files for deidentification. The process includes (1) copying target DICOM files, (2) deidentifying the copied DICOM files, (3) zipping the deidentified DICOM files at a particular directory level, and (4) uploading the zip files to an Amazon S3 bucket.

There are classes in subpackages of `edu.umich.med.alzheimers.dicom` for performing each of the four steps in isolation: 

1. `...copy.Copy`
2. `...deidentify.Deidentify`
3. `...zip.Zip`
4. `...upload.Upload`

There is also one class for performing all four steps (copy, deidentify, zip, upload): `edu.umich.med.alzheimers.dicom.CopyDeidentifyZipUpload`. 

Whether you use the single- or multi-step process, the DICOMs are procssed the same:

1. **Copy**: The source directory tree containing DICOM files is filtered for specific directories (by name and number of files within) and DICOM files (by name and `SeriesDescription` attribute value). That source directory tree is then copied to a target directory. 
2. **Deidentify**: The DICOM files in the target directory tree are deidentified.
3. **Zip**: The directories containing DICOM files (or DICOM files themselves) in the target directory tree are zipped at a specific tree depth. The resulting zip files are written to another directory.
4. **Upload**: The zip files are uploaded to an S3 bucket. 

To minimize file I/O, source directories and files are only copied if they do not already exist in the target directory tree. 

A good grasp of regular expressions is required to configure and use this app effectively. ([RegexOne](https://regexone.com/) offers a good tutorial on regular expressions.)


## Getting Started

### Prerequisites

1. If you haven't already, install Java JDK 1.8 and ensure it's the default JDK.

2. In order to recompile/repackage the JARs or to run the tests, install [sbt](https://www.scala-sbt.org/index.html). You can install sbt from [here](https://www.scala-sbt.org/release/docs/Setup.html).

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

```shell script
sbt test
```

**Note**: This will return failed tests until the app is properly configured.


## Usage

### Configuration

There are five config files, one for the app overall (`package.conf`) and four for each step in the deidentification process: `copy.conf`, `deidentify.conf`, `zip.conf`, and `upload.conf`. All five are placed in `src/main/resources/config/`. (Templates `*.conf.template` have been provided).

##### 1. Configuring `package.conf`

* `appDirPathStr`: String of the directory where this app rests, likely the `dicom-deidentify` folder that was created when you cloned this repo.
* `intermedDirsRegexArray`: Regex string array of directories between the `sourceDirPathStr` in `copy.conf` (see below) and the target DICOM files.
* `dicomFilenameRegexArray`: Regex string array of DICOM file names to include for processing.
* `seriesDescriptionRegexArray`: Regex string array of Series Description element values to include for processing.
* `idPrefixStringArray`: ID prefix values in DICOM "Patient ID" element to replace.
* `expectedIdPrefixStr`: 

##### 2. Configuring `copy.conf`

* `sourceDirPathStr`: Path string of the source directory that contains all the DICOM files you want to copy.
* `targetDirPathStr`: Path string of the target directory that the DICOM files and their directory tree will be copied to.
* `intermedDirsRegexArray`: Array of regular expression strings for directory names that will be included in the source and target directory trees.
* `dicomFilenameRegexArray`: Array of regular expression strings for acceptable DICOM filenames to copy.
* `seriesDescriptionRegexArray`: Array of regular expression strings for acceptable DICOM file `SeriesDescription` attribute values to copy.

##### 3. Configuring `deidentify.conf`

* `sourceDirPathStr`: Path string of the target directory that the DICOM files and their directory tree will be copied to.
* `dicomAttributesToReplaceWithZero`: Array of strings for DICOM attributes whose values will be set to empty strings.

##### 4. Configuring `zip.conf`

* `sourceDirPathStr`: Path string of the source directory that contains the directories or DICOM files that will be zipped.
* `targetDirPathStr`: Path string of the target directory that the zipped directories or zipped DICOM files will be placed in. Note that the directory tree above any zipped directory or DICOM file is preserved.
* `zipDepth`: Depth in directory tree to zip directories containing DICOM files, or DICOM files themselves.

##### 5. Configuring `upload.conf`

* `sourceDirPathStr`: Path string of the source directory that the zipped directories or zipped DICOM files have been placed in.
* `uploadDepth`: Depth in directory tree where the zip files to be uploaded are.
* `awsAccessKeyId`: AWS access key ID.
* `awsSecretAccessKey`: AWS secret access key.
* `s3BucketStr`: S3 bucket string, e.g., "my-s3-bucket" in "s3://my-s3-bucket".
* `s3KeyPrefixStr`: S3 key prefix, .e.g, "folder1/folder2/" in "s3://my-s3-bucket/folder1/folder2/zipfile.zip".

### Package JAR with sbt

Now that you've got the app configured, package it into a JAR file using sbt:

```shell script
sbt package
```

This will build a JAR file, `target/scala-2.13/dicom-deidentify_2.13-0.1.jar`.

### Running sbt-packaged JAR

To run the sbt package of the app JAR from the command line, you will need to define the path of some dependency JARs in your shell. Then you can pass the dependency JAR paths to the Java classpath option (as in `java -cp`).

First, define the path of the dependency JARs:

   ```shell script
   SCALA="lib/scala-library-2.13.3.jar"            && \
     PIXELMED="lib/pixelmed.jar"                   && \
     LB_CORE="lib/logback-core-1.2.3.jar"          && \
     LB_CLASSIC="lib/logback-classic-1.2.3.jar"    && \
     SLF4J="lib/slf4j-api-1.7.30.jar"              && \
     PICOCLI="lib/picocli-4.2.0.jar"               && \
     CONFIG="lib/config-1.3.0.jar"                 && \
     ZIP="lib/zt-zip-1.14.jar"                     && \
     S3="lib/aws-java-sdk-1.7.4.jar"               && \
     CL="lib/commons-logging-1.1.1.jar"            && \
     CC="lib/commons-codec-1.3.jar"                && \
     HTTP_CORE="lib/httpcore-4.2.jar"              && \
     HTTP_CLIENT="lib/httpclient-4.2.jar"          && \
     JAX_CORE="lib/jackson-core-2.1.1.jar"         && \
     JAX_DATA="lib/jackson-databind-2.1.1.jar"     && \
     JAX_ANNOT="lib/jackson-annotations-2.1.1.jar" && \
     APP="target/scala-2.13/dicom-deidenitfy_2.13-0.1.jar"
   ```

#### Copy

Run the sbt-packaged JAR, passing both the dependency JARS to the classpath option, and the class that contains the main method for copying only: `Copy`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$APP \
     edu.umich.med.alzheimers.dicom.copy.Copy
   ```

Note that the `Copy` class is in the subpackage `copy` of the `edu.umich.med.alzheimers.dicom` package.

#### Deidentify

Same as Copy above, but pass the class that contains the main method for deidentifying only: `Deidentify`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$APP \
     edu.umich.med.alzheimers.dicom.deidentify.Deidentify
   ```

#### Zip

Same as Copy above, but pass the class that contains the main method for zipping only: `Zip`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$ZIP:$APP \
     edu.umich.med.alzheimers.dicom.zip.Zip
   ```

#### Upload

Same as Zip above, but pass the class that contains the main method for uploading only: `Upload`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$S3:$CC:$CL:$HTTP_CLIENT:$HTTP_CORE:$JAX_CORE:$JAX_DATA:$JAX_ANNOT:$APP \
     edu.umich.med.alzheimers.dicom.upload.Upload
   ```

#### Copy + Deidentify + Zip + Upload

Run the sbt-packaged JAR (defined at `APP="target/scala-2.13/dicom-deidentify_2.13-0.1.jar` above),  passing both the dependency JARs to the classpath option, and the class that contains the main method for doing all three steps: `CopyDeidentifyZip`.

   ```shell script
   java -cp \
     $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$ZIP:\
     $S3:$CC:$CL:$HTTP_CLIENT:$HTTP_CORE:$JAX_CORE:$JAX_DATA:$JAX_ANNOT:$APP \
     edu.umich.med.alzheimers.dicom.CopyDeidentifyZip
   ```


## Built Using

* [Scala 2.13](https://www.scala-lang.org/)
* [PixelMed Java DICOM Toolkit](https://www.pixelmed.com/dicomtoolkit.html)
* [Logback](http://logback.qos.ch/)
* [picocli](https://picocli.info/)
* [Zeroturnaround Zip](https://github.com/zeroturnaround/zt-zip)
* [AWS SDK for Java v1](https://aws.amazon.com/sdk-for-java/)


## Authors

[@LDNicolasMay](https://github.com/ldnicolasmay) - Idea and work.


## Acknowledgments

Thanks to Dr. David A. Clunie for his [PixelMed Java DICOM Toolkit](https://www.pixelmed.com/dicomtoolkit.html).

Thanks also to Saravanan Subramainian for his [DICOM Tutorials](https://saravanansubramanian.com/dicomtutorials/).
