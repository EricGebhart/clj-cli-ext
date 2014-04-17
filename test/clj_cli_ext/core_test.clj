(ns clj-cli-ext.core-test
  (:require [clojure.test :refer :all]
            [clj-cli-ext.core :as clie]
            [clojure.string :as string]))

;;; this needs to be turned into a real test...

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(def sftp-options
  [[nil "--port NUMBER" "access this port"
    :default 22
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number"]]
   [nil "--host HOST" "Connect to this hostname"
    :default-desc "localhost"
    ;:default (InetAddress/getByName "localhost")
    ;:parse-fn #(InetAddress/getByName %)
    ]
   ["-l" "--login ID" "Use this login id"]
   ["-p" "--password PASSWORD" "Use this password"]
   ["-h" "--help"]])

(def s3-options
  [["-b" "--bucket S3-Bucket" "Bucket to get yeti stack definition."]
   ["-a" "--access-key ACCESS-KEY" "S3 access key"]
   ["-s" "--secret-key SECRET-KEY" "S3 secret key"]
   ["-f" "--file-key FILE-KEY" "File-key to retrieve from bucket"]
   ["-h" "--help"]])

(def file-options
  [["-f" "--file PATH" "File path to get Yeti stack definition."]
   ["-h" "--help"]])


(def sub-options-map
  "define a map of sub commands, their options and a usage function."
  {:sftp  [sftp-options "get a file from an sftp server"]
   :s3    [s3-options "get an s3 file"]
   :file  [file-options "get a local file"]
   ;; Actions have no options spec.
   :start [nil "Start a new server"]
   :stop  [nil "Stop an existing server"]
   :restart [nil "Restart a Server"]
   })


(defn tests
  []
  (def args1 ["sftp" "--port" "8080" "-l" "eric" "-p" "foo"])
  (def args2 ["s3" "-b" "mybucket" "-a" "myaccesskey" "-s" "mySecret" "-f" "filekey"])
  (def args3 ["file" "-f" "somefile/path.edn"])
  (def action-args ["start" "stop" "restart"])
  (def invalid-args ["foobar"])
  (def help-args ["--help"])
  (def help-args2 ["s3" "--help"])

  (def valid-args (concat args1 args2 args3 action-args))


  ;(println (:subcommands mycli ))

  ;; ;; add them one at a time.
  ;; (def mycli
  ;;   (-> (new-cli "mytest" "1.0.0" "My test program does nothing")
  ;;       (add-sub-command :sftp sftp-options "Get a file from an sftp server")
  ;;       (add-sub-command :s3 s3-options "Get a file from an s3 bucket")
  ;;       (add-sub-command :file file-options "Get a local file")
  ;;       (add-sub-command :start nil "Start the server")
  ;;       (add-sub-command :stop nil "Stop the server")
  ;;       (add-sub-command :restart nil "Restart the server")
  ;;       (parse-all valid-args)
  ;;       ))

  ;; an empty command line, --help and --version only.
  ;; Add them all at once.
  (def mycli
    (-> (clie/new-cli "mytest" "1.0.0" "My test program does nothing.")
        (clie/set-sub-commands sub-options-map)
        (clie/parse-all valid-args)))

  (def allopts (:parsed-sub-commands mycli ))

  (println (string/join \newline allopts))
  (println "------S3--------")
  (println (string/join \newline  (:s3 allopts)))
  (println "------SFTP--------")
  (println (string/join \newline (:sftp allopts)))
  (println "------Start--------")
  (println (:start allopts))
  (println "------Actions--------")
  (println (:actions mycli))
  (println "------Actions-Text-------")
  (println (clie/get-actions-text mycli ))
  (println "-------total possible actions:----- ")
  (println (clie/get-actions (:subcommands mycli)))
  (println "------subcommands-Text-------")
  (println (clie/get-subcommands-text mycli ))
  (println "-------total possible subcommands:----- ")
  (println (clie/get-subcommands (:subcommands mycli)))
  (println "--------Usage-------------")
  (println ((:usage mycli) mycli "this is a stupid summary"))
  (println "--------Sub-command-Usage-------------")
  (println ((:sub-command-usage mycli) mycli "s3" "this is another stupid summary"))

  (def mycli
    (clie/new-cli "mytest" "1.0.0" "My test program does nothing."))

  (println "--------Simple-Program-Usage-------------")
  (println ((:usage mycli) mycli "this is a stupid summary"))

  ;; ;; this will fail, print the usage and exit..
  ;; (def mycli
  ;;   (-> (clie/new-cli "mytest" "1.0.0" "My test program does nothing.")
  ;;       (clie/parse-all valid-args)))

  )
