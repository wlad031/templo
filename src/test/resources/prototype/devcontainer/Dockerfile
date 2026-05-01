FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

ARG USERNAME=vscode

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER ${USERNAME}
