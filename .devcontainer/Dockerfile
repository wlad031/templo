FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

ARG USERNAME=vscode

RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates gnupg2 \
    && curl -fsSL https://keyserver.ubuntu.com/pks/lookup?op=get\&search=0x99E82A75642AC823 | gpg --dearmor -o /usr/share/keyrings/sbt.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/sbt.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list \
    && echo "deb [signed-by=/usr/share/keyrings/sbt.gpg] https://repo.scala-sbt.org/scalasbt/debian /" > /etc/apt/sources.list.d/sbt_old.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends sbt \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER ${USERNAME}
