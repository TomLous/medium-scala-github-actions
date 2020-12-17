# Medium Scala Github Actions

[![Github Actions](https://github.com/TomLous/medium-scala-github-actions/workflows/Automatic%3A%20On%20Push/badge.svg)](https://github.com/TomLous/medium-scala-github-actions/actions?query=workflow%3A%22Automatic%3A+On+Push%22)
[![codecov](https://codecov.io/gh/TomLous/medium-scala-github-actions/branch/main/graph/badge.svg?token=JP3gvCahOW)](https://codecov.io/gh/TomLous/medium-scala-github-actions)

This project is used as example for this medium article:
The project is setup as a multi-module scala template. 

Currently there are 2 modules `basic-example` and `util` to house common components for both.


## Project configuration

There are three .sbt files in the root that shape this project
- `release.sbt` contains release logic, shouldn't have to be touched.
- `version.sbt` contains the first semantic version number of your project. For, default: `1.0.0`, for pre-mvp change to `0.1.0`
 This is the only time you ever need to change the version manually. The release logic will take over from here
- `build.sbt` contains all project logic & structure. Follow all TODO's in this file to customize for your team & project


## Github Config

Make sure the following keys are set in the repostory secrets:

`CODECOV_TOKEN` token from codecov for this project
`SLACK_WEBHOOK_URL` webhook url for the CI slack channel (probably )

## Build / Run

SparkJobs should be scala objects that extend App and SparkJob. This makes them runnable from IDE, CLI and (kubernetes) cluster
eg. `object [Runner] extends App with SparkJob`

### Run from IDE (IntelliJ)
Create a Run Configuration pointing to the file. Make sure to pass VM params to point to correct config file.
Best is to copy the `application.template.conf` in the resource directory to `application.local.conf` (gitignored)
Then pass `-Dconfig.resource=application.local.conf -DLogLevel=DEBUG` to the run config
Make sure that in the class path options `Include dependencies with "Provided" scope` is checked

### Run from sbt
Just type `sbt -Dconfig.resource=application.local.conf -DLogLevel=INFO [module]/run` or something similar. 
The `runLocalSettings` in build.sbt make sure that provided dependencies are bundled

### Run from minikube
Make sure minikube is setup correctly via `make minikube-setup` optionally followed by `LOCAL_DATA_DIR=[dir] make minikube-mount` and/or `make minikube-add-secret [module]`

First build the image: `make docker-build [module]`

Then push the image: `make docker-push-minikube [module]`

Finally helm deploy: `make helm-minikube-deploy [module]`

You can check the Spark UI via `kubectl port-forward [team]-[module]-driver 4040:4040` or `kubectl logs pod/[team]-[module]-driver`


## Deploy
Deployment & versioning is done automatically via CI/CD
Just create new features on a reasonably named `feature/[name]` branch. And create a PR to `main` when satified.
Every push to remote `feature/[name]` branch will trigger linters & testers

When merged with `main` a new SNAPSHOT release will be automatically deployed to DEV (eg. image & chart)

To start a release use the manual github action to start releasing the current version on the HEAD of `main`
This will create & deploy the image to STAGING & DEV with the new semantic release version `vX.Y.Z` and a PR back to main

Merging the PR form `release` to `main` will trigger the push to PROD of this release version `vX.Y.Z`

All actions are notified in Slack for an automatic flow


## Makefile

The Makefile consists of a plethora of targets that can be used from your own machine as well as CI/CD flows

All can be run using `make [target] [options]`

### Targets:

#### list-modules
Lists all available runnable modules (created by createProjectModule in build.sbt) in this codebase.

#### lint
Runs `sbt scalastyle` for entire project

#### test
Runs `sbt test` for entire project

#### test-coverage
Also runs `sbt test` but includes generation of coverage info to be used by codecov.io

#### version
Outputs the current version (in `version.sbt`)

#### create-feature-branch [feature]
Creates a local feature branch based on head of main branch

#### create-hotfix-branch
Creates a local hotfix branch based on latest stable release. 

#### minikube-setup
Creates minkikube setup on MacOS with ample resources and registry. Creates namespaces, spark-operator, etc to make current project deployable to minikube

#### minikube-delete
Deletes the minikube setup

#### minikube-start
Starts a stopped minikube and does some config

#### minikube-mount
Requires `LOCAL_DATA_DIR` env that points to dir or file that will be mounted to `/mounted-local-data` in minikube

#### minikube-add-secret [module]
Reads the secret file in `[module]/helm-vars/secrets` and creates/overwrites a `minikube-[module].secret` in `spark-apps` namespace.
The secret file should be formatted in `key:value` lines

#### docker-build [module]
Runs the `sbt [module]/docker` command creating a local docker image called `[team]/[module]:latest` for that module.

#### docker-image-clean [module]
Delete all images locally with the name `[team]/[module]`

#### docker-images-clean
Delete all images locally starting with `[team]/**`

#### docker-images-purge
Delete all dangling images and do a system prune

#### docker-push-minikube [module]
Sets up a link between local docker and minikube registry (port 5000). Tags the `[team]/[module]:latest` image as `localhost:5000/[team]/[module]:latest` and `localhost:5000/[team]/[module]:[version]` and pushes the image to the minikube registry, making it available for helm installs in minikube.

#### helm-minikube-deploy [module]
Installs / upgrades a SparkApplication helm deployment for the current module in `spark-apps` namespace in minikube based on the settings in `/[module]/helm-vars/values-minikube.yaml` 

#### acr-docker-push-login
Needs `APP_SPN_PWD`, `APP_SPN` and `CONTAINER_REGISTRY` (in Makefile) and logs in into docker images registry

#### acr-helm-push-login
Needs `APP_SPN_PWD`, `APP_SPN` and `CONTAINER_REGISTRY` (in Makefile) and logs in into helm chart repository

#### acr-list-charts
Needs `TEAM_NAME`, `APP_SPN_PWD`, `APP_SPN` and `CONTAINER_REGISTRY` (in Makefile) and lists all charts for current team in Github Container Registry

#### acr-list-images
Needs `TEAM_NAME`, `APP_SPN_PWD`, `APP_SPN` and `CONTAINER_REGISTRY` (in Makefile) and lists all images for current team in Github Container Registry

#### acr-repository-tags
Needs `ENV_REPOSITORY`, `APP_SPN_PWD`, `APP_SPN` and `CONTAINER_REGISTRY` (in Makefile) and lists all tags for current image/chart (`ENV_REPOSITORY`) in Github Container Registry


### CI/CD only:

#### list-modules-json
Does the same as `list-modules` but outputs as json array to be used by matrix config in CI/CD

#### upload-codecov
Requires env var: `CODECOV_TOKEN`. Uploads coverage to <https://codecov.io>

#### set-github-config
Requires env var: `GITHUB_ACTOR`. Sets user config to this account to enable commits & pushes

#### git-push
Just runs `git push` && `git push --tags`

#### check-changes
Requires env var: `SHA_OLD` and `SHA_NEW`. Is used to see if anything but `versions.sbt` has changed since last deploy. 

#### bump-snapshot
Bumps snapshot version in `version.sbt` & git commits. Used in merging `feature/[branch]` with `main` branch
eg. `v1.2.0-[hash1]-SNAPSHOT` =>  `v1.2.0-[hash2]-SNAPSHOT`

#### bump-snapshot-and-push
Runs `set-github-config`, `bump-snapshot` and `git-push` consecutively 

#### bump-release
Bumps release version in `version.sbt`,  git commits and tags. Used in merging `release` with `main` branch 
The choice between minor and major release can be set in `release.sbt` (`nextReleaseBump`) and is default Minor 
eg. `v1.2.0-[hash1]-SNAPSHOT` =>  `v1.2.0`, `v1.2.0` =>  `v1.3.0`, `v1.2.1` =>  `v1.3.0`

#### bump-release-and-push
Runs `set-github-config`, `bump-release` and `git-push` consecutively

#### bump-patch
Bumps paths version in `version.sbt`,  git commits and tags. Used in merging `hotfix` with `main` branch
eg. `v1.2.0-[hash1]-SNAPSHOT` =>  `v1.2.1`, `v1.2.0` =>  `v1.2.1`, `v1.2.3` =>  `v1.2.4`

#### bump-patch-and-push
Runs `set-github-config`, `bump-patch` and `git-push` consecutively

#### docker-push-acr [module]
Pushes the locally created docker image `[team]/[module]:latest` to the `CONTAINER_REGISTRY` defined in the Makefile
`[CONTAINER_REGISTRY]/[team]/[module]:[version]` and `[CONTAINER_REGISTRY]/[team]/[module]:latest` 

#### install-helm
Installs helm and azure cli in CI/CD environment to be able to push to helm Github Container Registry

#### helm-concat [module]
Needs `ENVIRONMENT` as env variables and creates a single helm deployment in the `helm/` directory based on these env vars

#### helm-push-acr [module]
Needs `CONTAINER_REGISTRY` (from Makefile), `ENVIRONMENT` and `HELM_EXPERIMENTAL_OCI=1` as env var. Replaces the `imageRegistry` param in `values.yaml` to point to `CONTAINER_REGISTRY`
Saves the charts and pushes them to the chart repo in `CONTAINER_REGISTRY` using `[CONTAINER_REGISTRY]/[team]/charts/[ENVIRONMENT]/[chart name from module]:latest` and `[CONTAINER_REGISTRY]/[team]/charts/[ENVIRONMENT]/[chart name from module]:[version]`


