(defproject parseapp-cljs "0.2.0-SNAPSHOT"
  :description "clojurescript to parse cloud code"
  :url "https://github.com/utahstreetlabs/parseapp-cljs"
  :repositories {
    "sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"
  }
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [ring "1.1.8"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.3"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:optimizations :simple
                   :pretty-print true}
        :jar true}}}
  :main parseapp-cljs.server
  :ring {:handler parseapp-cljs.server/app})

