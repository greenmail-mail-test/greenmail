FROM node:lts-alpine

ENV NODE_ENV=production \
    NPM_CONFIG_PRODUCTION=true

WORKDIR /nodemailer
RUN addgroup nodemailer && \
    adduser -S nodemailer nodemailer && \
    chown -R nodemailer:nodemailer /nodemailer
USER nodemailer:nodemailer
COPY --chown=nodemailer server.js /nodemailer/server.js
RUN npm install --save nodemailer

ENTRYPOINT [ "node","/nodemailer/server.js" ]
