(defproject com.zensols.tools/erg "0.1.0-SNAPSHOT"
  :description "Convert PerfProStudio files to ERG mode files, which is used for trainers like the WahooKickr."
  :url "https://github.com/plandes/clj-perferg"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.10.1"]
            [org.clojars.cvillecsteele/lein-git-version "1.2.7"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Convert PerfProStudio files to ERG mode files."}
          :output-path "target/doc/codox"
          :source-uri "https://github.com/plandes/clj-perferg/blob/v{version}/{filepath}#L{line}"}
  :git-version {:root-ns "zensols.erg"
                :path "src/clojure/zensols/erg"
                :version-cmd "git describe --match v*.* --abbrev=4 --dirty=-dirty"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :jar-exclusions [#".gitignore"]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; logging for core
                 [org.apache.logging.log4j/log4j-core "2.7"]

                 ;; json parse
                 [org.clojure/data.json "0.2.6"]

                 ;; spreadsheet
                 [com.zensols.tools/misc "0.0.4"]

                 ;; command line
                 [com.zensols.tools/actioncli "0.0.15"]]
  :pom-plugins [[org.codehaus.mojo/appassembler-maven-plugin "1.6"
                 {:configuration ([:programs
                                   [:program
                                    ([:mainClass "zensols.erg.core"]
                                     [:id "pp2erg"])]]
                                  [:environmentSetupFileName "setupenv"])}]]
  :profiles {:uberjar {:aot [zensols.erg.core]}
             :appassem {:aot :all}
             :snapshot {:git-version {:version-cmd "echo -snapshot"}}
             :dev
             {:jvm-opts ["-Dlog4j.configurationFile=test-resources/log4j2.xml" "-Xms4g" "-Xmx12g" "-XX:+UseConcMarkSweepGC"]
              :exclusions [org.slf4j/slf4j-log4j12
                           log4j/log4j
                           ch.qos.logback/logback-classic]
              :dependencies [[org.apache.logging.log4j/log4j-slf4j-impl "2.7"]
                             [org.apache.logging.log4j/log4j-1.2-api "2.7"]
                             [org.apache.logging.log4j/log4j-jcl "2.7"]
                             [com.zensols/clj-append "1.0.5"]]}}
  :main zensols.erg.core)
