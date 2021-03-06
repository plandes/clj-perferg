(ns zensols.erg.perfpro
  (:require [clojure.java.io :as io]
            [clojure.set :refer (rename-keys)]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:require [clj-excel.core :as excel])
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :refer (with-exception)]
            [zensols.util.spreadsheet :as ss]))

(def ^:dynamic *default-percent-ftp-ranges*
  "Arary of arrays regular expressions of English sentence ranges."
  [#"(\d+)%?\s*(?:to|[\-])\s*(\d+)%"
   #"(\d+)%(?uix:ftp)"])

;; TODO: add tests for regexs
;; (re-find #"(\d+)%?\s*(?:to|[\-])\s*(\d+)%" "92% to 97%")
;; (re-find #"(\d+)%?\s*(?:to|[\-])\s*(\d+)%" "92 to 97%")

(def ^:dynamic *default-percent-ftp-descriptors*
  "Arary of arrays with each a regular expression of the description, a keyword
  identifier token and a percent to use for FTP."
  [[#"^(?ui)warm\s*up$" :warmup 0.4]
   [#"^(?ui)cool\s*down$" :sprint 0.3]
   [#"^(?ui)(easy\s*)?recovery?$" :recovery 0.6]
   [#"^(?ui)fast\s*spin$" :fast-spin 0.3]
   [#"^(?ui)easy\s*spin$" :fast-spin 0.35]
   [#"^(?ui)spin$" :spin 0.4]
   [#"^(?ui)sprint$" :sprint 0.9]
   [#"^(\d+)\s+(?ui)rpm$" :unknown 0.8]])

(def ^:dynamic *linear-interpolate-ftp-zone* 4.5)

(defn- parse-ppsmrx
  "Parse a .ppsmrx JSON file."
  [file]
  (log/debugf "parsing perfpro file: %s" file)
  (with-open [reader (io/reader file)]
    (-> (json/read-json reader)
        (select-keys [:set_fields :workoutType :sets :comments :name])
        (rename-keys {:set_fields :fields
                      :workoutType :type}))))

(defn- description-to-ftp-range
  [s]
  (->> *default-percent-ftp-ranges*
       (some (fn [pat]
               (let [[_ st en] (re-find pat s)]
                 (cond (and st en) (map #(Double/parseDouble %) [st en])
                       st (map #(Double/parseDouble %) [st st])))))))

(defn- description-to-ftp-descriptor
  "Guess the percent FTP by the description.  This is given by "
  [s]
  (->> *default-percent-ftp-descriptors*
       (some (fn [[pat desc percent]]
               (if (re-find pat s)
                 {:descriptor desc
                  :percent percent})))
       ((fn [data]
          (or data (-> (format "No known descriptor for: <%s>" s)
                       (ex-info {:descriptor s})
                       throw))))))

(defn- zone-to-percent
  "Convert from an FTP zone to an FTP percent.  **desc** is the description of
  the interval and num is the `start` field."
  [desc num]
  (log/debugf "zone to percent: %s, %s" desc num)
  (if-let [ftp-range (description-to-ftp-range desc)]
    (let [high (apply max ftp-range)
          low (apply min ftp-range)]
      (/ (+ (/ (- high low) 2.0) low) 100.0))
    (if (and (< 0 num) (> 7 num))
      (/ num *linear-interpolate-ftp-zone*)
      (or (-> (description-to-ftp-descriptor desc) :percent)
          (-> (format "Don't know how to convert percent '%s' of %d" desc num)
              (ex-info {:desc desc :num num})
              throw)))))

(defn- format-time [seconds]
  (let [minutes (/ (double seconds) 60.0)]
    (format "%2d:%02d"
            (int (Math/floor minutes))
            (int (mod seconds 60)))))

(defn- power-sets
  "Create power sets used in the ERG.  **ftp** is the athlete's FTP and
  **ppdata** is parsed by [[parse-ppsmrx]]."
  [ftp ppdata]
  (log/debugf "computing power sets on ftp=%f" ftp)
  (let [{:keys [fields sets]} ppdata
        fields (map keyword fields)]
    (->> sets
         (map (fn [row]
                (zipmap fields row)))
         (map (fn [{:keys [start seconds description mode targetcad] :as m}]
                (when (> (-> description s/trim count) 0)
                  (log/debugf "[%s, %s]: <%s>, m=%s, tc=%s"
                              start seconds description mode targetcad)
                  (let [minutes (/ (double seconds) 60.0)
                        time-desc (format-time seconds)
                        ;; bad name
                        percent (if (= mode "M")
                                  (-> (description-to-ftp-descriptor description)
                                      :percent)
                                  (zone-to-percent description start))
                        watts (* percent ftp)]
                    {:description description
                     :cadence targetcad
                     :time time-desc
                     :seconds seconds
                     :minutes minutes
                     :percent (* 100 percent)
                     :watts watts}))))
         (remove nil?)
         (reduce (fn [{:keys [rowid] :as res} {:keys [seconds] :as m}]
                   (let [st (+ (:start res) seconds)]
                    {:lst (conj (:lst res)
                                (assoc m :start st :rowid rowid))
                     :start st
                     :rowid (+ 1 rowid)}))
                 {:lst []
                  :start 0
                  :rowid 0})
         :lst)))

(defn- save-power-sets
  "Save an Excel file using power sets (**sets**).  **fields** are the keys
  returned in maps from [[power-sets]] and **file** is the output file."
  [fields tab-name file sets]
  (log/debugf "saving excel file: %s, %s, %s" file fields tab-name)
  (-> (excel/build-workbook
       (excel/workbook-hssf)
       {tab-name
        (->> sets
             (cons fields)
             ss/headerize)})
      (ss/autosize-columns)
      (excel/save file)))

(defn- save-as-excel
  "Massage data and save with [[save-power-sets]]."
  [ppdata power-sets out-file]
  (let [fields [:description :time :start :rowid :percent :watts :cadence]]
    (->> power-sets
         (map (fn [row]
                (map #(get row %) fields)))
         (save-power-sets (map #(-> % name s/capitalize) fields) (:name ppdata)
                          out-file))))

(defn- course-data
  "Create the ERG *course* data."
  [power-sets]
  (->> power-sets
       (reduce (fn [{:keys [rows time description]} {:keys [minutes watts]}]
                 (let [start (double time)
                       end (double (+ time minutes))
                       mmap {:watts watts
                             :description description}]
                   {:time end
                    :rows (concat rows [(assoc mmap :time start)
                                        (assoc mmap :time end)])}))
               {:rows []
                :time 0})
       :rows))

(defn- compose-erg
  "Print the ERG file to standard out."
  [ftp out-file power-sets ppdata]
  (let [ftp 244
        {:keys [name type]} ppdata
        course-data (course-data power-sets)]
    (println "[COURSE HEADER]")
    (->> (format "MINUTES WATTS
VERSION = 2
UNITS = ENGLISH
DESCRIPTION = %s (type: %s)
FILE NAME = %s
FTP = %d
MINUTES WATTS
[END COURSE HEADER]
[COURSE DATA]"
                 name type
                 out-file
                 ftp)
         println)
    (->> course-data
         (map (fn [{:keys [time watts]}]
                (format "%.2f\t%d" (double time) (int watts))))
         (map println)
         doall)
    (println "[END COURSE DATA]\n\n[PERFPRO DESCRIPTIONS]")
    (->> power-sets
         (map (fn [{:keys [rowid description cadence] :as m}]
                (println (format "Desc%d=%s (%d rpm)"
                                 rowid description cadence))))
         doall)
    (println "[END PERFPRO DESCRIPTIONS]\n\n[PERFPRO COMMENTS]")
    (println (str "Comments=" (:comments ppdata)))
    (println "[END PERFPRO COMMENTS]")))

(defn- file-to-name
  "Get the name portion of a file."
  [file]
  (->> file
       .getName
       (#(or (-> (re-find #"^(.*)\." %) second) %))))

(defn read-and-save
  "Read and parse the PerfPro `.ppsmrx` file, then output the ERG file.

* **ftp** is the athlete's FTP (see project docs)
* **perf-pro-file** is the `.ppsrmx` file
* **export-excel?** if non-nil create an Excel summary file as well"
  [ftp perf-pro-file export-excel?]
  (let [in-file perf-pro-file
        file-name (file-to-name in-file)
        parent-file (.getParentFile in-file)
        erg-file (->> file-name (format "%s.erg") (io/file parent-file))
        xls-file (->> file-name (format "%s.xls") (io/file parent-file))
        ppdata (parse-ppsmrx in-file)
        power-sets (power-sets ftp ppdata)]
    (with-open [writer (io/writer erg-file)]
      (binding [*out* writer]
        (compose-erg ftp erg-file power-sets ppdata)))
    (log/infof "wrote: %s" erg-file)
    (when export-excel?
      (save-as-excel ppdata power-sets xls-file)
      (log/infof "wrote: %s" xls-file))))

(def convert-command
  "CLI command to convert PerfPro .ppsmrx files."
  {:description "Convert a perfpro .ppsmrx file to .erg and optionally .xls"
   :options
   [(lu/log-level-set-option)
    ["-p" "--perfpro" "The PerfPro .ppsmrx file input file"
     :required "<file>"
     :parse-fn io/file]
    ["-e" "--excel" "If provided output an Excel summary file as well"]
    ["-f" "--ftp" "The functional threshold power"
     :required "<number>"
     :parse-fn #(Double/parseDouble %)
     :validate [#(> % 50) "Must be a positive number above 50"]]
    ["-i" "--interpolate" "zone to percent FTP interplation constant"
     :required "<number>"
     :parse-fn #(Double/parseDouble %)
     :default *linear-interpolate-ftp-zone*
     :validate [#(> % 1) "Must be a positive number above 1"]]]
   :app (fn [{:keys [perfpro ftp interpolate excel] :as opts} & args]
          (with-exception
            (binding [*linear-interpolate-ftp-zone* interpolate]
              (if (nil? perfpro) (throw (ex-info "Missing -p option" {})))
              (if (nil? ftp) (throw (ex-info "Missing -f option" {})))
              (log/infof "processing %s with ftp %s" perfpro ftp)
              (log/debugf "excel: %s" excel)
              (read-and-save ftp perfpro excel))))})
