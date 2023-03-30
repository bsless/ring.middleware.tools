[![Clojars Project](https://img.shields.io/clojars/v/io.github.bsless/ring.middleware.tools.svg)](https://clojars.org/io.github.bsless/ring.middleware.tools)
[![cljdoc](https://cljdoc.org/badge/io.github.bsless/ring.middleware.tools)](https://cljdoc.org/d/io.github.bsless/ring.middleware.tools)
![Test And Snapshot](https://github.com/bsless/ring.middleware.tools/actions/workflows/test-and-snapshot.yml/badge.svg)

# Ring Middleware Tools

Tools for the boilerplate and annoying bits of putting together ring middlewares.

## Usage

See this [brief tutorial](https://github.clerk.garden/bsless/ring.middleware.tools)
or [tests](./test/bsless/test/bsless/ring/middleware/tools_test.clj)

## Dependency information

Deps:

```clojure
{io.github.bsless/ring.middleware.tools {:mvn/version "..."}}
```

Leiningen:

```clojure
[io.github.bsless/ring.middleware.tools "..."]
```

## API

See [API.md](./API.md)

## Development

### Build API.md

    $ clojure -M:quickdoc

### Test

    $ clojure -T:build test

### CI

    $ clojure -T:build ci

### Install locally

    $ clojure -T:build install

### Deploy

    $ clojure -T:build deploy

## License

Copyright © 2023 Bsless

Distributed under the Eclipse Public License version 1.0.
