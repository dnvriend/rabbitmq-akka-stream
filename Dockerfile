FROM java:8

ADD target/dist /appl

RUN chown 1000:1000 /appl/bin/start

WORKDIR /appl/bin
CMD [ "./start" ]