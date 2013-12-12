# parseapp-cljs

An idiomatic Clojure library for building Parse Cloud Code apps.

## Quick Start

For best results, pair with the parseapp lein template:

```
lein new parseapp a-great-name-for-your-new-app
```

## Usage

### Compiilation

You should use the :nodejs target and configure an empty preamble to avoid the
normal node.js hashbang:

```cljs
  :cljsbuild {
    :builds {
      :test {:compiler {;; other options here elided for clarity
                        :target :nodejs
                        :preamble ["parse_preamble.js"]}}}}
```

```sh
$ cat parse_preamble.js
$
```


## License

Copyright © 2013 Travis Vachon

Distributed under the Eclipse Public License, the same as Clojure.
