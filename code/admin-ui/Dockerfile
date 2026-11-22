FROM node:22-alpine AS build
WORKDIR /app
ARG VITE_KEYCLOAK_URL=http://localhost:8091
ARG VITE_KEYCLOAK_REALM=gateway
ARG VITE_KEYCLOAK_CLIENT=ac-gateway-admin
ARG VITE_GRAFANA_URL=
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM VITE_KEYCLOAK_CLIENT=$VITE_KEYCLOAK_CLIENT VITE_GRAFANA_URL=$VITE_GRAFANA_URL
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
