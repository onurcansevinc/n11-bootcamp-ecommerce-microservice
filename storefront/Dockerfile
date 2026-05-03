FROM node:22-alpine AS build

ARG VITE_API_BASE_URL=http://localhost:8760
ARG VITE_OIDC_AUTHORITY=http://keycloak.localhost:8080/realms/ecommerce
ARG VITE_OIDC_CLIENT_ID=storefront-web

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_OIDC_AUTHORITY=${VITE_OIDC_AUTHORITY}
ENV VITE_OIDC_CLIENT_ID=${VITE_OIDC_CLIENT_ID}

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
