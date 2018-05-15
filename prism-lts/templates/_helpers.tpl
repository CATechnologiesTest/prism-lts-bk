{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "prism-lts.serviceHost" -}}
{{- $service := default .Chart.Name .Values.s3ConnectorJob.config.name -}}
{{- printf "%s.%s.svc.cluster.local" $service .Release.Namespace -}}
{{- end -}}

{{- define "prism-lts.stamp" -}}
{{- $stamp := now | quote | sha256sum | trunc 6 -}}
{{- printf "%s-%s" . $stamp -}}
{{- end -}}

{{- define "prism-lts.checksum" -}}
{{- $root := index . 0 -}}
{{- $file := index . 1 -}}
{{- include (print $root.Template.BasePath $file) $root | sha256sum }}
{{- end -}}

{{- define "prism-lts.config"}}
{{- range $key, $val := . }}
- name: {{ $key }}
  value: {{ $val | quote }}
{{- end}}
{{- end}}