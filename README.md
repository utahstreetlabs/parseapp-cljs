# parseapp-cljs

An idiomatic Clojure library for building Parse Cloud Code apps.

*WARNING* this is all alpha: subject to API changes without major version bumps

*WARNING* currently using a custom version of ClojureScript (see `comp/clojurescript` submodule) - use at your own risk

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

### Running tests

First, make sure you've initialized and updated the compiler submodule:

```
git submodule init
git submodule update
```

Next, deploy the tests to a test project. Make a new parse app and add the keys to test_app/config/local.json:

```
{
    "applications": {
        "test": {
            "applicationId": "yourTestAppId",
            "masterKey": "yourTestAppKey"
        }
    }
}
```

And then deploy with `bin/testonce`

Finally, run the tests with

```
curl -X POST \\
  -H "X-Parse-Application-Id: yourTestAppId" \\
  -H "X-Parse-REST-API-Key: yourTestAppKey" \\
  -H "Content-Type: application/json" \\
  -d '{}' \\
  https://api.parse.com/1/functions/runTests
```

When developing tests you may want to autodeploy to Parse with `bin/testauto`

## License

Copyright © 2013 Travis Vachon

Distributed under the Eclipse Public License, the same as Clojure.
