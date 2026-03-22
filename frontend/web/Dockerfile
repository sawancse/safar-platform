FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --legacy-peer-deps

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_PUBLIC_API_URL=https://api.ysafar.com
ENV NEXT_PUBLIC_CDN_DOMAIN=media.ysafar.com
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

FROM node:20-alpine
WORKDIR /app
RUN addgroup -S safar && adduser -S safar -G safar

COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

USER safar
EXPOSE 3000

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
