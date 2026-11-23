FROM node:22-alpine AS build
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_AI_CAREER_BASE_URL=/ai-career
ARG VITE_LMS_BASE_URL=
ARG VITE_LMS_PORTAL_URL=
ARG VITE_MOCK_API_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_AI_CAREER_BASE_URL=${VITE_AI_CAREER_BASE_URL}
ENV VITE_LMS_BASE_URL=${VITE_LMS_BASE_URL}
ENV VITE_LMS_PORTAL_URL=${VITE_LMS_PORTAL_URL}
ENV VITE_MOCK_API_URL=${VITE_MOCK_API_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.31-alpine
COPY infra/docker/frontend.conf /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 8080
