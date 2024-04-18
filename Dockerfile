##### Stage 1
FROM node:14.17.0 as node
LABEL author="Sunil K"

ARG PROFILE
WORKDIR /app
COPY package.json package.json
RUN npm install --force
COPY . .
#RUN echo "docker command = npm run build --configuration=$PROFILE"
RUN npm run build -- --configuration=$PROFILE

##### Stage 2
FROM nginx:alpine
ADD docker/harden.sh /
RUN chmod 700 /harden.sh \
	&& sh -c "/harden.sh" \
	&& rm -rf /harden.sh
VOLUME /var/cache/nginx
COPY --from=node /app/dist /usr/share/nginx/html
COPY ./config/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY ./deploy/cert.pem /etc/nginx/conf.d/cert.pem
COPY ./deploy/key.pem /etc/nginx/conf.d/key.pem
#EXPOSE 443
#EXPOSE 8080
#docker build -t weather .
#docker run -p 80:80 weather
