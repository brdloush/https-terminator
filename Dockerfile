FROM babashka/babashka:1.0.166-alpine
ADD https_terminator.clj /opt/
ENTRYPOINT ["/opt/https_terminator.clj"]