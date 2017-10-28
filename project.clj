(defproject org.eag.clie "0.1.2"
  :description "CLI-Ext: Extensions to tools.cli to make commandline argument creation and parsing better and easier."
  :url "http://github/EricGebhart/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.eag.datetime-data "0.1.0"]
                 [org.eag.file-access "0.1.1"]]



  :profiles {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.1"]]}

             :dev {:dependencies [[midje "1.5.1"]]
                   :plugins [[lein-midje "3.0.1"]]
                   :prep-tasks ["javac"]
                   :source-paths ["src"]}}

  :repositories {"local" ~(str (.toURI (java.io.File. "/home/eric/.m2/repository")))})
