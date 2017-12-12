# Prism Long Term Storage

![From One Conveyor To Another](move-the-data.gif)

## Local Development

- `eval $(minikube docker-env)` to change docker contexts to minikube
- `docker build . -f DockerfileCurl -t bash-curl` to build the docker image for the job
- Setup AWS secrets, with `kubectl create secret generic aws-s3-creds --from-file ~/.aws/credentials`
- `kubectl expose deployment prism-lts --type=ClusterIP`
- `kubectl apply -f ./local/deployment.yml --validate=false`
- `kubectl apply -f ./local/submit-kafka-connect-s3-job-cm.yml --validate=false`
- `kubectl apply -f ./local/submit-kafka-connect-s3-job-job.yml --validate=false`
- `./tail-logs` to `tail -f` the kafka conenct container

### To Send Data to the REST Proxy
- `kubectl port-forward $(kubectl get po -o name -l app=prism-lts --sort-by='.metadata.creationTimestamp' | cut -d \/ -f 2 | tail -n 1) 8082:8082`
- `./post-message` to send a message into the kafka bus 

## Must Haves to Meet Customer Requirements

- [ ] Fix `health-metrics`  schema issues in dev
- [ ] Deploy in Production

## Must haves to be a "Real" Production Service

- [ ] Automate recovery of Connect Jobs (turn k8s job into k8s cron job?)
- [ ] Monitoring of Kafka Connect Jobs
- [ ] Alerting on N failed jobs
- [ ] Circle CI
