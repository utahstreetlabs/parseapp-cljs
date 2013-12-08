(defproject parseapp-cljs "0.5.0-SNAPSHOT"
  :description "clojurescript to parse cloud code"
  :url "https://github.com/utahstreetlabs/parseapp-cljs"
  :repositories {
    "sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"
  }
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojurescript "0.0-2080"]
                 [com.cemerick/url "0.1.0"]
                 [com.tvachon/core.async "0.1.0"]]
  :plugins [[lein-cljsbuild "1.0.0"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:optimizations :whitespace
                   :pretty-print true}
        :jar true}

      :test {:source-paths ["src/cljs" "test/cljs"]
             :compiler {:output-to "test_app/cloud/cljs.js"
                        :optimizations :whitespace
                        :pretty-print true}}}})
