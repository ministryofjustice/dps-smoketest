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

  - name: OAUTH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: OAUTH_CLIENT_ID

  - name: OAUTH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: OAUTH_CLIENT_SECRET

  - name: PRISONAPI_ENDPOINT_URL
    value: "{{ .Values.env.PRISONAPI_ENDPOINT_URL }}"

  - name: COMMUNITY_ENDPOINT_URL
    value: "{{ .Values.env.COMMUNITY_ENDPOINT_URL }}"

  - name: PROBATIONOFFENDERSEARCH_ENDPOINT_URL
    value: "{{ .Values.env.PROBATIONOFFENDERSEARCH_ENDPOINT_URL }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: APPLICATIONINSIGHTS_CONFIGURATION_FILE
    value: "{{ .Values.env.APPLICATIONINSIGHTS_CONFIGURATION_FILE }}"

  - name: HMPPS_SQS_QUEUES_HMPPSEVENTQUEUE_QUEUE_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: dps-smoketest-queue-secret
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_HMPPSEVENTQUEUE_QUEUE_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: dps-smoketest-queue-secret
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_HMPPSEVENTQUEUE_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: dps-smoketest-queue-secret
        key: sqs_queue_name

{{- end -}}
