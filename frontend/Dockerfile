# Estágio 1: Build do aplicativo React
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# Estágio 2: Servir a aplicação com Nginx
FROM nginx:stable-alpine
# Copia os arquivos estáticos construídos do estágio anterior
COPY --from=build /app/dist /usr/share/nginx/html
# Expõe a porta 80 do Nginx
EXPOSE 80
# Comando para iniciar o Nginx quando o container iniciar
CMD ["nginx", "-g", "daemon off;"]