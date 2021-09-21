FROM node:14

ENV PORT 3000
#ARG url
# Create app directory
RUN mkdir -p ./app
WORKDIR /app

# Installing dependencies
COPY package*.json /app/

RUN npm install

# Copying source files
COPY . /app
#RUN echo NEXT_PUBLIC_BASE_API_URL=${url} > .env.local
RUN echo NEXT_PUBLIC_BASE_API_URL=/v1 > .env.local

# Building app
RUN npm run build
EXPOSE 3000

# Running the app
CMD "npm" "start"
