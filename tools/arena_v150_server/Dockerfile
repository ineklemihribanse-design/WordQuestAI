FROM node:22-bookworm-slim
WORKDIR /app
COPY tools/arena_v150_server/package.json ./arena_v150_server/package.json
RUN cd arena_v150_server && npm install --omit=dev --ignore-scripts --no-audit --no-fund
COPY tools/v132_engine/*.mjs ./v132_engine/
COPY tools/arena_v150_server/*.mjs ./arena_v150_server/
USER node
ENV NODE_ENV=production
EXPOSE 8080
CMD ["node","arena_v150_server/arena_runtime.mjs"]
