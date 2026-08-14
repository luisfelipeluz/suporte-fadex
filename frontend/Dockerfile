# =============================================================================
# Frontend — React + Vite servido por nginx
# =============================================================================

# --- Estágio 1: build --------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Estágio 2: servidor estático -------------------------------------------
FROM nginx:1.27-alpine

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
