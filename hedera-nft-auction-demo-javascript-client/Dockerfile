FROM node:14

ARG URL
ENV PORT 3000

# Create app directory
RUN mkdir -p ./app
WORKDIR /app

RUN echo NEXT_PUBLIC_BASE_API_URL=${URL} > .env.local

# Installing dependencies
COPY package*.json /app/
RUN npm install

# Copying source files
COPY . /app

# Building app
RUN npm run build
EXPOSE 3000

# Running the app
CMD "npm" "start"
