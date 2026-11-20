# ============================================================
# Dockerfile for Nhu Villas Frontend (Next.js standalone)
# ============================================================

# Build stage (Node 22 = the version CI type-checks and builds with)
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# NEXT_PUBLIC_* values are inlined at build time.
ARG NEXT_PUBLIC_API_URL=http://backend:8080/api/v1
ARG NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=
# Demo one-click login: staging/local only (R-13). Never enable for production images.
ARG NEXT_PUBLIC_ENABLE_DEMO_LOGIN=false
ARG NEXT_PUBLIC_DEMO_LOGIN_PASSWORD=
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL} \
    NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=${NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY} \
    NEXT_PUBLIC_ENABLE_DEMO_LOGIN=${NEXT_PUBLIC_ENABLE_DEMO_LOGIN} \
    NEXT_PUBLIC_DEMO_LOGIN_PASSWORD=${NEXT_PUBLIC_DEMO_LOGIN_PASSWORD} \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# Runtime stage
FROM node:22-alpine AS runner

WORKDIR /app

RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000 \
    HOSTNAME="0.0.0.0" \
    NEXT_TELEMETRY_DISABLED=1

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD node -e "fetch('http://127.0.0.1:3000/').then(r=>process.exit(r.status<500?0:1)).catch(()=>process.exit(1))"

CMD ["node", "server.js"]
