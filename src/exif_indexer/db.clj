(ns exif-indexer.db
  "Facility for managing H2 database, where EXIF metadata is indexed."
  (:require [exif-indexer.helpers :refer [print-exception]]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :refer [as-file]]))

(def ^{:doc "Database filename"
       :private true}
  dbfile "./exif-indexer")

(def ^{:doc "Global database object"
       :dynamic true
       :public true}
  *db* {:classname "org.h2.Driver"
        :subprotocol "h2:file"
        :subname dbfile})

(defmacro with-default-connection
  "Run actions on already set db database object. Handle exception by
logging and printing stacktrace, returning nil from expression."
  [& body]
  `(try
     (sql/with-db-connection [db# *db*]
       ~@body)
     (catch Throwable e#
       (print-exception e# "Caught database exception"))))

(defn do-commands
  "Run SQL commands on default database."
  [commands]
  (sql/db-do-commands *db* true commands))

(defn extract-val
  "Extract row id, returned after insert."
  [ret]
  (-> ret first vals first))

(defn setup-database!
  "Create database if not present. It will be place in folder from where
indexer is started.

Note that EXIF key/values are limited to 255 chars."
  []
  (when-not (-> (str dbfile ".mv.db") as-file .exists)
    (with-default-connection
      (do-commands
       (sql/create-table-ddl "images"
        [:id      :bigint :auto_increment]
        [:name    "varchar(255)"]
        [:url     "varchar(255)"]
        [:lastmod "varchar(255)"])))

    (with-default-connection
      (do-commands
       (sql/create-table-ddl "exif_data"
        [:key    "varchar(255)"]
        [:value  "varchar(255)"]
        [:image_id :int "references images(id) on delete cascade"])))))
