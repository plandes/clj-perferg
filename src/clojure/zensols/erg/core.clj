(ns zensols.erg.core
  (:require [zensols.actioncli.log4j2 :as lu]
            [zensols.actioncli.parse :as cli])
  (:require [zensols.erg.version :as ver])
  (:gen-class :main true))

(defn- version-info []
  (println (format "%s (%s)" ver/version ver/gitref)))

(defn- create-action-context []
  (cli/single-action-context
   '(zensols.erg.perfpro convert-command)
   :version-option (cli/version-option version-info)))

(defn -main [& args]
  (lu/configure "erg-log4j2.xml")
  (cli/set-program-name "pp2erg")
  (-> (create-action-context)
      (cli/process-arguments args)))
