Prerequisites:

* Kubernetes cluster and `kubectl` CLI

Initial setup binds a cluster role to the default service account in the `default` namespace. This allows the service account to interact with the token resources in the cluster.:

```
$ kubectl apply -f config/account.yaml
```

Manually fish out a token:

```
$ kubectl apply -f config/account.yaml
$ KUBERNETES_HOST=`kubectl -n kube-system get pod -l component=kube-apiserver -o=jsonpath="{.items[0].metadata.annotations.kubeadm\.kubernetes\.io/kube-apiserver\.advertise-address\.endpoint}"`
$ TOKEN=`kubectl create token -n spring-system default`
```

and authenticate it with `curl`:

```
$ curl -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -k https://127.0.0.1:43319/apis/authentication.k8s.io/v1/tokenreviews -d '{"spec":{"token":"'$TOKEN'"}}'
```

The response should be successful and contain the status indicating the token is valid:

```
{
  "kind": "TokenReview",
  "apiVersion": "authentication.k8s.io/v1",
  "metadata": {...},
  "spec": {...},
  "status": {
    "authenticated": true,
    "user": {
      "username": "system:serviceaccount:spring-system:default",
      "uid": "b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1",
      "groups": [
        "system:serviceaccounts",
        "system:serviceaccounts:spring-system",
        "system:authenticated"
      ]
    }
  }
}
```