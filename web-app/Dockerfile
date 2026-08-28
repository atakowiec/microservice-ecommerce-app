FROM node:24.19.0-alpine AS builder

WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . .

RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine3.24 AS runner

COPY --chown=nginx:nginx --from=builder /app/dist/*/browser /usr/share/nginx/html
COPY --chown=nginx:nginx runtime-env.template.js /usr/share/nginx/html/runtime-env.template.js
COPY nginx.conf /etc/nginx/nginx.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh

USER root

RUN chmod 0755 /docker-entrypoint.sh

USER nginx

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
