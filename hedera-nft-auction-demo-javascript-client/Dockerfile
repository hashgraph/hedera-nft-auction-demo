FROM node:14.15.0

ENV PORT 3000

# Create app directory
RUN mkdir -p ./app
WORKDIR /app

# Installing dependencies
COPY package*.json /app/
RUN npm install

# Copying source files
COPY . /app

# Building app
RUN npm run-script build
EXPOSE 3000

# Running the app
CMD "npm" "start"
