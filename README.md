# https_terminator.clj

Simple HTTP->HTTPS proxy server which accepts non-trusted (eg. self-signed) certificates.

## Usage

```bash
https_terminator.clj
    
Spawns a simple HTTP server which proxies all incoming HTTP requests to remote HTTPS server, preserving relative path, headers and url params.

  -t, --target-url TARGETURL  Url of remote HTTPS server. Should not end with /.
  -p, --port PORT             localhost port for newly spawned HTTP server
  -v, --verbose               Print verbose logging messages (eg. content of request/response)
  -h, --help                  Shows this usage information.


example: bb https_terminator.clj -t https://some.remoteserver.com -p 1234
```

## Requirements

- babashka