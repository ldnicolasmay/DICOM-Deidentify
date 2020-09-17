```shell script
SCALA="lib/scala-library-2.13.3.jar"         && \
  PIXELMED="lib/pixelmed.jar"                && \
  LB_CORE="lib/logback-core-1.2.3.jar"       && \
  LB_CLASSIC="lib/logback-classic-1.2.3.jar" && \
  SLF4J="lib/slf4j-api-1.7.30.jar"           && \
  PICOCLI="lib/picocli-4.2.0.jar"            && \
  CONFIG="lib/config-1.3.0.jar"              && \
  APP="target/scala-2.13/dicom-deidenitfy_2.13-0.1.jar"
```

```shell script
java -cp \
  $SCALA:$PIXELMED:$LB_CORE:$LB_CLASSIC:$SLF4J:$PICOCLI:$CONFIG:$APP \
  edu.umich.med.alzheimers.dicom_deidentify.Main$delayedInit$body
```