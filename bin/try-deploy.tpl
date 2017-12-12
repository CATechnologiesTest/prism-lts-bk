#!/bin/bash -es

function getLogs {
    pods=$(kubectl get pods -l sha=${GIT_SHA} -o jsonpath={.items[*].metadata.name} -n ${NAMESPACE})
    for pod in $pods
    do
        failingContainers=$(kubectl get pod $pod -n ${NAMESPACE} -o 'jsonpath={.status.containerStatuses[?(@.ready==false)].name}')
        for container in $failingContainers
        do
            printf "\n"
            echo "XXXXXXXXXXXXXXXXXXXXXXXX FAILING CONTAINER LOGS XXXXXXXXXXXXXXXXXXXXXXXXX"
            echo "Container $container is not ready for deployment in pod $pod"
            kubectl logs $pod $container -n ${NAMESPACE}
            printf "\n\n\n"
        done
    done
}


function tryRollout {
    (kubectl rollout status deployment/$1 -n ${NAMESPACE}) & pid=$!
    for i in {1..40}; do
         sleep 2
         if ! ps | grep $pid | grep -v grep > /dev/null; then
             echo 'Rollout success!!'
             break
         else
             if [ $i -eq 40 ]; then
                 echo 'Your Docker image could not start or healthchecks did not pass.  Here is you description of pods.  Look in the events section'
                 kubectl describe pod --selector='sha=${GIT_SHA}' -n ${NAMESPACE}
                 printf "\n\n\n"
                 echo "Logs for failing containers:"
                 getLogs
                 echo 'Undoing deployment, it failed'
                 kubectl rollout undo deployment/$1 -n ${NAMESPACE}
                 if [[ "${ENVIRONMENT}" == "prod" ]]; then
                    exit 1
                 else
                    echo "Build failed in dev, could be due to unstable environment. Check prod build."
                    exit 0
                 fi
             fi
             echo 'Rollout still going'
         fi
      done
}

kubectl apply -f deployment.yaml

tryRollout ${SERVICE_NAME}

kubectl get deployments
