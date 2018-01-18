# Prism Long Term Storage

![From One Conveyor To Another](move-the-data.gif)

## Local Development

### Initial Setup

- `minikube start`
- Setup AWS secrets, with `kubectl create secret generic aws-s3-creds --from-file ~/.aws/credentials`
- Setup Quay credentials, go to `https://quay.io/organization/stsatlas?tab=robots`, select `stsatlas+platform_deployer` and follow the directions for Kubernetes Secret
  - Rename the secret (in the file downloaded) to be `quay-sts`
  - Run `kubectl create -f /path/to/stsatlas-platform-deployer-secret.yml`
- Install helm with `brew install kubernetes-helm`
- Run `helm init`
- Get dependencies for prism-lts by running `helm dep update prism-lts && helm dep build prism-lts` from the root of this project

### Updating the `local-kafka` chart or `local-charts` [environment specific] values

If you update any environment specific values or the local kafka chart, you will need to update and build dependencies in prism-lts again.

- Run `helm dep update`
- Run `helm dep build`

### Running Prism-lts

`<release_name>` is how you will refer to your installation of the helm chart in your local cluster.
- Your first run will use the command: `helm install --replace --name=<release name> ./prism-lts --set tags.prism-lts-local-values=true`
- After the first run, use the command: `helm upgrade --install <release_name> ./prism-lts --set tags.prism-lts-local-values=true`

### Updating the Kafka Connect Docker Image 

- `eval $(minikube docker-env)` to change docker contexts to minikube
- build the `sts-atlas/schwarz-kafka-connect` jar using `lein uberjar` in that repo
- copy the jar to the `prism-long-term-storage` repo
  - NOTE: ensure that the file is renamed to `schwartz-kafka-connect.jar`
- `docker build . -t quay.io/stsatlas/schwartz-kafka-connect:<your git SHA> -f DockerfileSchwartzKafkaConnect` to build the docker image
- `docker push quay.io/stsatlas/schwarz-kafka-connect:<your git SHA>` to push the docker image
- update `prism-lts/values.yaml`

### Updating the Curl Docker Image (used by the kafka connect jobs)

- `eval $(minikube docker-env)` to change docker contexts to minikube
- `docker build . -f DockerfileCurl -t quay.io/stsatlas/bash-curl:<your git SHA>` to build the new docker image
- `docker push quay.io/stsatlas/bash-curl:<your git SHA>` to push the docker image
- update `prism-lts/values.yaml`

### Changing metadata labels or annotations
If you change metadata labels or annotations, helm does not know that the previous release running in your minikube cluster is the same app.
Delete your old release by running `helm delete <release_name> --purge`
The `--purge` flag removes references that helm has to track your release.


### To Send Data to the REST Proxy
- `kubectl port-forward $(kubectl get po -o name -l app=prism-lts --sort-by='.metadata.creationTimestamp' | cut -d \/ -f 2 | tail -n 1) 8082:8082`
- `./prism-lts/bin/post-message` to send a message into the kafka bus

## Must Haves to Meet Customer Requirements

- [ ] Fix `health-metrics`  schema issues in dev
- [ ] Deploy in Production

## Must haves to be a "Real" Production Service

- [ ] Automate recovery of Connect Jobs (turn k8s job into k8s cron job?)
- [ ] Monitoring of Kafka Connect Jobs
- [ ] Alerting on N failed jobs
- [ ] Circle CI
