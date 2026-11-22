FROM node:26-alpine AS build
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_AI_CAREER_BASE_URL=/ai-career
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_AI_CAREER_BASE_URL=${VITE_AI_CAREER_BASE_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY infra/docker/frontend.conf /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 8080
