(ns exif-indexer.core
  "Main driver."
  (:gen-class)
  (:require [exif-indexer.processor :as p]
            [exif-indexer.db :refer [setup-database!]]))

(defn- help []
  (println "Usage: java -jar exif-indexer.jar [options]")
  (println "Options:")
  (println "   -index          Perform indexing by fetching image data from S3.")
  (println "   -lookup <file>  Print metadata for given file.")
  (println "   -help           This help.")
  (System/exit 1))

(defn- err [msg]
  (println msg)
  (System/exit 1))

(defn -main [& args]
  (setup-database!)
  (if-not (seq args)
    (help)
    (condp = (first args)
      "-help"   (help)
      "-lookup" (if-let [a (second args)]
                  (p/print-details a)
                  (err "This argument requires a parameter."))
      "-index"  (p/index-all)
      (println "Unknown argument. Use '-help' to see options."))))

