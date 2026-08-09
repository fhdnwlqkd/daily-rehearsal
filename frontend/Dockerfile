FROM node:22-alpine AS deps
WORKDIR /app
RUN npm install -g pnpm@9
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile --ignore-scripts

FROM node:22-alpine AS builder
WORKDIR /app
RUN npm install -g pnpm@9
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN pnpm build

FROM node:22-alpine AS runner
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
ENV NODE_ENV=production
ENV PORT=3000

COPY --from=builder /app/public ./public
COPY --from=builder --chown=app:app /app/.next/standalone ./
COPY --from=builder --chown=app:app /app/.next/static ./.next/static

USER app
EXPOSE 3000
ENTRYPOINT ["node", "server.js"]
