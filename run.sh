#!/usr/bin/env bash

mvn package dependency:copy-dependencies

java -cp target/*:target/dependency/* org.mbari.vars.avfoundation.AVFImageCapture "$@"