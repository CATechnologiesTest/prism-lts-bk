## Get the Application Running

`helm install -n v0.1.0 . --set tags.lts-local-values=true`

## Debugging this Chart

- `helm lint` to verify that you follow best practices
- `helm install --dry-run --debug` render the templates and see what's what
- `helm get manifest` to see what k8s objects are running on the cluster

## Ideas

- Make the Kafka Connect job a helper template 
