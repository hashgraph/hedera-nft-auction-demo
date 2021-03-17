FROM node:14.9 AS build

COPY ./package.json /srv/hedera-nft-auction-demo-javascript-client/package.json
COPY ./yarn.lock /srv/hedera-nft-auction-demo-javascript-client/yarn.lock
COPY ./.browserslistrc /srv/hedera-nft-auction-demo-javascript-client/.browserslistrc
COPY ./.eslintrc.js /srv/hedera-nft-auction-demo-javascript-client/.eslintrc.js
COPY ./babel.config.js /srv/hedera-nft-auction-demo-javascript-client/babel.config.js
COPY ./vue.config.js /srv/hedera-nft-auction-demo-javascript-client/vue.config.js
COPY ./.env /srv/hedera-nft-auction-demo-javascript-client/.env
#COPY ./.env /srv/hedera-nft-auction-demo-javascript-client/.env.production
ADD ./src /srv/hedera-nft-auction-demo-javascript-client/src
ADD ./public /srv/hedera-nft-auction-demo-javascript-client/public

RUN cd /srv/hedera-nft-auction-demo-javascript-client && yarn install
RUN cd /srv/hedera-nft-auction-demo-javascript-client && yarn build

FROM nginx:alpine

COPY ./nginx.conf /etc/nginx/conf.d/default.conf
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /srv/hedera-nft-auction-demo-javascript-client/dist /usr/share/nginx/html
COPY --from=build /srv/hedera-nft-auction-demo-javascript-client/.env /usr/share/nginx/html/.env
COPY --from=build /srv/hedera-nft-auction-demo-javascript-client/.env /usr/share/nginx/html/.env.production

CMD ["nginx", "-g", "daemon off;"]
