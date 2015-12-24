# resolve

Basic service registry and dynamic proxy, loosely inspired by https://github.com/substack/seaport.

## Features

1. KV store of endpoints against [name, version] pairs
2. Selects endpoint based on exact versions or semantic version ranges
3. Provides proxy urls to mapped endpoints

## TODO

[See issues](https://github.com/organosoft/resolve/issues)

## Usage

**DON'T USE**

To play:

repl: `(require '[bss.resolve.core :as resolve]) (resolve/run <port> <redis-options>)`

source: `lein run -m bss.resolve.core <port> <redis-options>`

compiled: `SPEC=<redis-options> foreman start -p <port>`

## License

Copyright © 2015
