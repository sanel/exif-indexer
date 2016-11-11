(ns exif-indexer.processor
  "Facility for fetching, extracting and storing metadata. Handles reading S3 storage via simple
http requests."
  (:require [exif-processor.core :as e]
            [exif-indexer.db :as db]
            [exif-indexer.helpers :refer [print-exception]]
            [clj-http.client :as http]
            [clojure.xml :as xml]
            [clojure.java.jdbc :as sql]))

;; For reading images from S3, I'm not using AWS S3 libraries, to keep things simple.
;; Instead, application will read S3 XML bucket metadata, extract <Key> and collect associated metadata.
;; Value from <Key> tag is appended to base S3 public bucket url.

;; For extracting EXIF data, exif-indexer will not store image on disk, but figure out image url
;; and use exif-processor/exif-for-url to actually do heavy lifting, returning EXIF metadata.

(defn- percent
  "Calculate percentage."
  [cur tot]
  (if (> tot 0)
    (long (* 100 (/ cur tot)))
    0))

(defn- xml-parse-string
  "Convinient function for parsing XML strings."
  [^String in]
  (-> in .getBytes java.io.ByteArrayInputStream. xml/parse))

(defn- extract-tag
  "Get all elements that matches this tag."
  [mp tag]
  (filter #(= tag (get % :tag)) mp))

(defn- content-tag-to-metadata
  "Convert interesting details from <Content> to our metadata."
  [base-url mp]
  (let [data    (:content mp)
        key     (-> data (extract-tag :Key) first :content first)
        lastmod (-> data (extract-tag :LastModified) first :content first)]
    ;; for easier insertion, this map looks like 'images' schema in db.clj
    {:url      (format "%s/%s" base-url key)
     :name     key
     :lastmod  lastmod}))

(defn parse-bucket-xml
  "Fetch bucket url and parse interesting parts. Keys are resolved too. Returns
list of maps in form {:url ..., :file ..., :lastmod ...}."
  [url]
  (printf "* Fetching details for %s...\n" url)
  (flush)
  (try
    (let [data (-> url http/get :body xml-parse-string)
          data (map (partial content-tag-to-metadata url)
                    (-> data :content (extract-tag :Contents)))]
      data)
    (catch Exception e
      (print-exception e (str "Failed to get data from " url)))))

(defn- img-exists?
  "Check if given image exists in database."
  [img]
  (boolean
   (seq
    (sql/query db/*db* ["select 1 from images where name = ?" img]))))

(defn index-image-metadata 
  "Accept map in format returned by 'parse-bucket-xml', but also fetch actual image data.
Store it in database."
  [mp]
  (let [id (db/extract-val
            (sql/insert! db/*db* "images" mp))
        url (-> mp :url)]
    (try
      (when-let [exif (e/exif-for-url url)]
        (doseq [[k v] exif]
          ; (printf "Storing '%s' = '%s' for %d\n" k v id)
          (sql/insert! db/*db* "exif_data" {:key k :value v :image_id id})))
      (catch Exception e
        (print-exception e (str "Failed to get EXIF data for " url))))))

(defn index-all
  "Index all images."
  []
  (let [data (parse-bucket-xml "http://s3.amazonaws.com/waldo-recruiting")
        sz   (count data)]
    ;; this part can be easily parallelised to speed up indexing, but for matter of simplicity,
    ;; only running thread is used
    (loop [data data, i 0]
      (when-let [mp (first data)]
        (printf "* [%d%%] Indexing %s...\n" (inc (percent i sz)) (:url mp))
        (flush)
        (if (img-exists? (:name mp))
          (println "** Already present, skipping")
          (index-image-metadata mp))
        (recur (rest data) (inc i))))))

(defn find-details
  "Get details for given image name and print it out.
Returns sorted map."
  [name]
  (let [data (sql/query db/*db* [(str "SELECT * FROM images AS i "
                                      "JOIN exif_data AS d "
                                      "ON i.id = d.image_id "
                                      "WHERE i.name = ?")
                                 name])]
    (into (sorted-map)
          (reduce (fn [coll d]
                    (assoc coll
                      (:key d) (:value d)
                      ;; not pretty, but can be further optimized
                      "Lastmod" (:lastmod d)
                      "Url"     (:url d)))
                  {}
                  data))))

(defn print-details
  "Pretty print details to output."
  [name]
  (let [result (find-details name)]
    (if (seq result)
      (doseq [[k v] (find-details name)]
        (printf "%-40s %-40s\n" k v)
        (flush))
      (println "No metadata found"))))
