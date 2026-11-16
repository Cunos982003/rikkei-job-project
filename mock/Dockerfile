FROM node:22-alpine
WORKDIR /app
COPY mock/package.json mock/package-lock.json ./
RUN npm ci --omit=dev
COPY mock/ ./
ENV NODE_ENV=production
EXPOSE 9000
USER node
CMD ["node", "server.js"]
