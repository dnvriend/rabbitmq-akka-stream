#!/bin/bash
sbt clean dist
docker rm -f $(docker ps -aq)
docker-compose build
docker-compose up -d
docker-compose logs
#docker-compose rm -f