FROM ubuntu:latest
LABEL authors="armando guzman"

ENTRYPOINT ["top", "-b"]