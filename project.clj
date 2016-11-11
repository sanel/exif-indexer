(defproject exif-indexer "0.1.0-SNAPSHOT"
  :description "Extract and index EXIF data"
  :url "https://github.com/sanel/exif-indexer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; handles metadata-extractor dependency properly
                 [social/exif-processor "0.1.2-socialsuperstore-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.h2database/h2 "1.4.191"]]
  :profiles {:uberjar {:aot :all
                       :javac-options ["-g:none"]
                       :uberjar-exclusions [#"\.clj", #"META-INF/(.+?)\.SF"]
                       :uberjar-name "exif-indexer.jar"}}
  :global-vars {*warn-on-reflection* true}
  :repl-options {:port 7888}
  :omit-source true
  :main exif-indexer.core)
