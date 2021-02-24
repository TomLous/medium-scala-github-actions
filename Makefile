#########################################
# Environment variables that need to be set in the Github Secrets settings 

# CODECOV_TOKEN for upload-codecov target
# REGISTRY_USERNAME & REGISTRY_PASSWORD credentials for Github Container Registry for pushing & pulling images
# SLACK_WEBHOOK_URL for slack integration

#########################################

# Note:
# - Many make targets are dependant on sbt config & actions
# - Defining values to be deferred (if not needed or sbt not present they should not be expanded).
#   However when they are expanded they should be only expanded once (lazy evaluation)
#   The way to do this is: OUTPUT = $(eval OUTPUT := $$(shell some-comand))$(OUTPUT) due to the ltr expansion and recursive nature
#   How http://make.mad-scientist.net/deferred-simple-variable-expansion/

# Hardcoded values

# The remote registry for Docker containers
CONTAINER_REGISTRY := docker.pkg.github.com
CHART_REGISTRY := ghcr.io

# Helm version
HELM_VERSION := v3.4.1

# The path where artifacts are created
OUTPUT_PATH := ./output

# Dynamic values:

# Use Bash instead of sh
SHELL := /bin/bash

SBT_COMMAND := sbt --error 'set showSuccess := false' -Dsbt.log.noformat=true

# Name of the team, often also name of the image namespace
REPO_NAME = $(eval REPO_NAME := $$(shell $(SBT_COMMAND) showTeam))$(REPO_NAME)
IMAGE_NAMESPACE = $(REPO_NAME)

# The version of the current release
VERSION = $(eval VERSION := $$(shell $(SBT_COMMAND) showVersion))$(VERSION)

# Allow to pass the module name as command line arg
MODULE = $(shell arg="$(filter-out $@,$(MAKECMDGOALS))" && echo $${arg:-${1}})

# Feature name
FEATURE = $(shell echo $(MODULE) | sed -e 's/[^a-zA-Z0-9]/-/g' | tr '[:upper:]' '[:lower:]' )

# Available modules to build
MODULES = $(eval MODULES :=  $$(shell $(SBT_COMMAND) listModules))$(MODULES) #Weird bug adding some escape char to all output

# Get the image namespace/repo
IMAGE_NAME = $(eval IMAGE_NAME := $$(shell $(SBT_COMMAND) $(MODULE)/showImageName))$(IMAGE_NAME)

# Get the chart name
CHART_NAME = $(eval CHART_NAME := $$(shell $(SBT_COMMAND) $(MODULE)/showChartName))$(CHART_NAME)

# Docker output
DOCKER_IMAGE_INFO_FILE := $(OUTPUT_PATH)/image.sh

# Load optionally generated shell file (used by Github Actions)
-include $(DOCKER_IMAGE_INFO_FILE)



####

define check_module
	@$(if $(MODULE), $(info Using module: $(MODULE)), $(error Module is not set in command (make [action] [module]). Use one of the following modules as argument: $(MODULES)))
endef


.PHONY: \
list-modules list-modules-json test-coverage upload-codecov \
set-github-config git-push check-changes create-hotfix-branch \
bump-snapshot bump-release bump-patch bump-snapshot-and-push bump-release-and-push bump-patch-and-push \
docker-build docker-push-registry docker-image-clean docker-images-clean docker-images-purge docker-push-minikube \
install-helm helm-concat helm-combine helm-push-registry helm-push-registry helm-minikube-deploy \
registry-docker-push-login registry-helm-push-login registry-helm-push-login registry-list-charts registry-list-images registry-repository-tags \
minikube-setup minikube-delete minikube-mount minikube-add-secret

.DEFAULT_GOAL := list-modules

# SBT Commands general
list-modules:
	@modules=($(MODULES)); for module in "$${modules[@]}"; do echo "$${module}"; done

list-modules-json:
	@echo $(MODULES) |  jq -R -c 'split(" ")'

echo-modules:
	@echo "'$(MODULES)'"

lint:
	$(SBT_COMMAND) scalastyle

test:
	$(SBT_COMMAND) test

test-coverage:
	$(SBT_COMMAND) -DcacheToDisk=1 coverage test coverageReport coverageAggregate

version:
	@echo $(VERSION)

upload-codecov: guard-CODECOV_TOKEN
	bash <(curl -s https://codecov.io/bash) -t $(CODECOV_TOKEN)

# GIT Commands
set-github-config: guard-GITHUB_ACTOR
	git config --global user.name "$(GITHUB_ACTOR)"
	git config --global user.email "$(GITHUB_ACTOR)@users.noreply.github.com"

git-push:
	git push
	git push --tags

check-changes: guard-SHA_OLD guard-SHA_NEW
	@if [ $(SHA_OLD) != "0000000000000000000000000000000000000000" ]; then\
		git diff --name-only $(SHA_OLD) $(SHA_NEW) | (grep -v version.sbt || true) | wc -l | tr -d ' '; \
	else\
		echo 1; \
	fi

create-hotfix-branch:
	git fetch
	git branch -d hotfix || true
	git checkout -b hotfix $$(git describe --tags --abbrev=0 | grep -E "^v[0-9]+\.[0-9]+\.[0-9]+$$")

create-feature-branch:
	@git checkout main && git fetch && git pull
	git checkout -b feature/$(FEATURE)



# SBT Version bumping
bump-snapshot:
	$(SBT_COMMAND) bumpSnapshot

bump-release:
	$(SBT_COMMAND) bumpRelease

bump-patch:
	$(SBT_COMMAND) bumpPatch

bump-snapshot-and-push: set-github-config bump-snapshot git-push
bump-release-and-push: set-github-config bump-release git-push
bump-patch-and-push: set-github-config bump-patch git-push

# Docker Commands
docker-build:
	$(call check_module) $(SBT_COMMAND) $(MODULE)/docker

docker-push-registry: guard-REGISTRY_OWNER
	$(call check_module)
	@docker tag $(IMAGE_NAME):$(VERSION) $(CONTAINER_REGISTRY)/$(REGISTRY_OWNER)/$(IMAGE_NAME):$(VERSION)
	@docker tag $(IMAGE_NAME):$(VERSION) $(CONTAINER_REGISTRY)/$(REGISTRY_OWNER)/$(IMAGE_NAME):latest
	docker push $(CONTAINER_REGISTRY)/$(REGISTRY_OWNER)/$(IMAGE_NAME):$(VERSION)
	docker push $(CONTAINER_REGISTRY)/$(REGISTRY_OWNER)/$(IMAGE_NAME):latest

docker-image-clean:
	@$(call check_module)
	$(info Deleting these images:)
	@docker images  -f "reference=$(IMAGE_NAME):*" --format "{{.Repository}}:{{.Tag}}"
	@docker rmi $$(docker images -f "reference=$(IMAGE_NAME):*" --format "{{.Repository}}:{{.Tag}}")

docker-images-clean:
	$(info Deleting these images:)
	@docker images  -f "reference=$(IMAGE_NAMESPACE)/*:*" --format "{{.Repository}}:{{.Tag}}"
	@docker rmi $$(docker images -f "reference=$(IMAGE_NAMESPACE)/*:*" --format "{{.Repository}}:{{.Tag}}")

docker-images-purge:
	@-docker rmi $$(docker images -f "dangling=true" -q)
	@-docker system prune

docker-push-minikube:
	@$(call check_module)
	$(info Forwarding localhost:5000 to minikube registry)
	@eval $$(minikube docker-env -u); \
	docker rm -f minikube_registry_link || true; \
	docker run -d --rm -it --name=minikube_registry_link --network=host alpine ash -c "apk add socat && socat TCP-LISTEN:5000,reuseaddr,fork TCP:$$(minikube ip):5000"
	@sleep 3 # enough time to get socat running if image is present
	docker tag $(IMAGE_NAME):$(VERSION) localhost:5000/$(IMAGE_NAME):$(VERSION)
	docker tag $(IMAGE_NAME):$(VERSION) localhost:5000/$(IMAGE_NAME):latest
	docker push localhost:5000/$(IMAGE_NAME):$(VERSION)
	docker push localhost:5000/$(IMAGE_NAME):latest
	@eval $$(minikube docker-env -u); \
	docker stop minikube_registry_link || true

# Helm commands
install-helm:
	curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
	curl -L https://git.io/get_helm.sh | bash -s -- --version $(HELM_VERSION)

helm-concat: guard-ENVIRONMENT
	@$(call check_module)
	cat $(MODULE)/helm-vars/values-$(ENVIRONMENT).yaml >> helm/values.yaml

helm-push-registry: export HELM_EXPERIMENTAL_OCI=1
helm-push-registry: guard-ENVIRONMENT guard-CHART_REGISTRY guard-REGISTRY_OWNER
	@$(call check_module)
	sed "s/imageRegistry:.*/imageRegistry: $(CHART_REGISTRY)/" -i helm/values.yaml
	cat helm/Chart.yaml
	cat helm/values.yaml
	helm chart save helm $(CHART_REGISTRY)/$(REGISTRY_OWNER)/$(CHART_NAME):$(VERSION)-$(ENVIRONMENT)
	helm chart push $(CHART_REGISTRY)/$(REGISTRY_OWNER)/$(CHART_NAME):$(VERSION)-$(ENVIRONMENT)


helm-minikube-deploy:
	@$(call check_module)
	helm upgrade --install $(CHART_NAME) ./helm -f ./$(MODULE)/helm-vars/values-minikube.yaml --namespace=spark-apps


# Github Container Registry Commands
registry-docker-push-login: guard-REGISTRY_PASSWORD guard-CONTAINER_REGISTRY guard-REGISTRY_USERNAME
	@echo $(REGISTRY_PASSWORD) | docker login $(CONTAINER_REGISTRY) --username $(REGISTRY_USERNAME) --password-stdin

registry-helm-push-login: export HELM_EXPERIMENTAL_OCI=1
registry-helm-push-login: guard-REGISTRY_PASSWORD guard-CHART_REGISTRY guard-REGISTRY_USERNAME
	@echo $(REGISTRY_PASSWORD) | helm registry login $(CHART_REGISTRY) --username $(REGISTRY_USERNAME) --password-stdin

registry-list-charts: guard-REGISTRY_PASSWORD guard-CHART_REGISTRY guard-REGISTRY_USERNAME guard-REPO_NAME
	@curl -s -u $(REGISTRY_USERNAME):$(REGISTRY_PASSWORD) -X GET https://$(CHART_REGISTRY)/v2/_catalog?n=2000 | jq '.[] | .[] | select( startswith ("$(REPO_NAME)/charts/"))'

registry-list-images: guard-REGISTRY_PASSWORD guard-CHART_REGISTRY guard-REGISTRY_USERNAME guard-REPO_NAME
	@curl -s -u $(REGISTRY_USERNAME):$(REGISTRY_PASSWORD) -X GET https://$(CHART_REGISTRY)/v2/_catalog?n=2000 | jq '.[] | .[] | select( startswith ("$(REPO_NAME)/")  and (contains("/charts/") | not))'

registry-repository-tags: guard-REGISTRY_PASSWORD guard-CHART_REGISTRY guard-REGISTRY_USERNAME guard-ENV_REPOSITORY
	curl -s -u $(REGISTRY_USERNAME):$(REGISTRY_PASSWORD) -X GET https://$(CHART_REGISTRY)/v2/$(ENV_REPOSITORY)/tags/list | jq '.[]'


# Minikube commands
minikube-setup:
	minikube start --driver=hyperkit --bootstrapper=kubeadm --cpus 4 --memory 8192 --insecure-registry=192.168.0.0/16
	minikube addons enable registry
	minikube addons enable dashboard
	@eval $$(minikube docker-env -u); \
	docker run -d --rm -it --name=minikube_registry_link --network=host alpine ash -c "apk add socat && socat TCP-LISTEN:5000,reuseaddr,fork TCP:$$(minikube ip):5000"
	kubectx minikube
	-helm plugin install https://github.com/chartmuseum/helm-push
	@eval $$(minikube docker-env); \
	docker run -d --name chartmuseum --restart=always -p 8080:8080 -e DEBUG=true -e STORAGE=local -e STORAGE_LOCAL_ROOTDIR=/home/chartmuseum/charts chartmuseum/chartmuseum:v0.12.0
	kubectl create namespace spark-operator
	kubectl create namespace spark-apps
	kubectl create serviceaccount spark --namespace=spark-apps
	kubectl create clusterrolebinding spark-operator-role --clusterrole=edit --serviceaccount=spark-apps:spark --namespace=spark-apps
	kubectl config set-context --current --namespace=spark-apps
	helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
 	helm repo add incubator https://charts.helm.sh/incubator
	helm repo update
	helm install spark incubator/sparkoperator --namespace spark-operator --set enableWebhook=true,sparkJobNamespace=spark-apps,logLevel=3 --skip-crds
	helm repo add chartmuseum http://$$(minikube ip):8080
	@echo "Check Cluster"
	kubectl cluster-info
	@echo "Check Registry"
	curl -s $$(minikube ip):5000/v2/_catalog | jq
	@echo "Check Chart Museum"
	curl $$(minikube ip):8080/index.yaml
	@echo "Check Spark Operator"
	kubectl get all -n spark-operator
	helm list -n spark-operator

minikube-delete:
	minikube stop
	minikube delete

minikube-start:
	minikube start
	kubectl config set-context --current --namespace=spark-apps

minikube-mount: guard-LOCAL_DATA_DIR
	minikube mount $(LOCAL_DATA_DIR):/mounted-local-data &

minikube-add-secret:
	@$(call check_module)
	$(info Transforming secrets in $(MODULE)/helm-vars/secrets into minikube-$(MODULE).secret)
	@kubectl delete secret minikube-$(MODULE).secret -n spark-apps || true;
	@CMD="kubectl create secret generic minikube-$(MODULE).secret  -n spark-apps "; while read secret; do CMD="$$CMD --from-literal=$${secret%:*}=$${secret#*:}"; done < $(MODULE)/helm-vars/secrets; $$CMD
	@kubectl get secret minikube-$(MODULE).secret  -n spark-apps -o yaml


# Guard to check ENV vars
guard-%:
	@ if [ -z '${${*}}' ]; then echo 'Environment variable $* not set.' && exit 1; fi

# Catch all for module name arguments
%:
	@:
