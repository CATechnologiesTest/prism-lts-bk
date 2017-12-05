# Prism Long Term Storage

![From One Conveyor To Another](move-the-data.gif)

## Local Development

1. Setup AWS secrets, with `kubectl create secret generic aws-s3-creds --from-file ~/.aws/credentials`
2. `kubectl apply -f ./local/deployment.yml --validate=false`
3. `kubectl apply -f ./local/submit-kafka-connect-s3-job-cm.yml --validate=false`
4. `kubectl apply -f ./local/submit-kafka-connect-s3-job-job.yml --validate=false`
5. `./tail-logs` to `tail -f` the kafka conenct container
6. `./post-message` to send a message into the kafka bus 
