FROM nginx:1.27-alpine

COPY 10-select-template.sh /docker-entrypoint.d/10-select-template.sh
COPY default-http.conf.template /opt/nginx-templates/default-http.conf.template
COPY default-https.conf.template /opt/nginx-templates/default-https.conf.template

RUN chmod +x /docker-entrypoint.d/10-select-template.sh
