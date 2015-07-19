#! /bin/bash
# Backups JDK variant specific build artifacts.
JDK_VARIANT=$1
ARTIFACT_VARIANT_DIR="$CIRCLE_ARTIFACTS/greenmail-$JDK_VARIANT"
echo $ARTIFACT_VARIANT_DIR
mkdir $ARTIFACT_VARIANT_DIR && \
find . -name \*.log -exec cp -r --parent {} $ARTIFACT_VARIANT_DIR/ \;
# Copies everything but takes too long when circlei backups artifacts
#find . -type d -name target -exec cp -r --parent {} $ARTIFACT_VARIANT_DIR/ \;
