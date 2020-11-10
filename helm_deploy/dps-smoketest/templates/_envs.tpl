    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ENDPOINT_URL }}"

  - name: PRISONAPI_ENDPOINT_URL
    value: "{{ .Values.env.PRISONAPI_ENDPOINT_URL }}"

  - name: COMMUNITY_ENDPOINT_URL
    value: "{{ .Values.env.COMMUNITY_ENDPOINT_URL }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: APPLICATION_INSIGHTS_IKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

{{- end -}}
