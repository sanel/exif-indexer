(ns exif-indexer.helpers
  "Helper functions.")

(defn print-exception
  "Helper for printing exception stacktraces."
  [^Exception e msg]
  (println "**" msg)
  (.printStackTrace e))
