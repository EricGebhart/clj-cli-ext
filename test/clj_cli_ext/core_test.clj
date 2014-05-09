(ns clj-cli-ext.core-test
  (:require [clojure.test :refer :all]
            [clj-cli-ext.core :as clie]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            ))

;;; this needs to be turned into a real test...
(def args1 ["import" "sftp" "--port" "8080" "-l" "eric" "-p" "foo"])
(def args2 ["import" "s3" "-b" "mybucket" "-a" "myaccesskey" "-s" "mySecret" "-f" "filekey"])
(def args3 ["import" "file" "-f" "somefile/path.edn"])
(def args4 ["-l" "-v" "3"])
(def server-args ["server" "--port" "8080" "start"])
(def bad-args ["-u" "foobar"])
(def bad-args2 ["foobar" "-u"])
(def help-args ["--help"])
(def help-args3 ["--version"])
(def help-args2 ["import" "s3" "--help"])
(def valid-args (concat args1 args2 args3 server-args))
(def invalid-args-last (concat valid-args bad-args))
(def invalid-args-first (concat bad-args valid-args))

(def server-options
  [["-P" "--port NUMBER" "access this port"
    :default 22
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number"]]
   [nil "--host HOST" "Connect to this hostname"
    :default-desc "localhost"
    ;:default (InetAddress/getByName "localhost")
    ;:parse-fn #(InetAddress/getByName %)
    ]
   ["-h" "--help"]])

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
  [["-b" "--bucket S3-Bucket" "Bucket to get file from."]
   ["-a" "--access-key ACCESS-KEY" "S3 access key"]
   ["-s" "--secret-key SECRET-KEY" "S3 secret key"]
   ["-f" "--file-key FILE-KEY" "File-key to retrieve from bucket"]
   ["-h" "--help"]])

(def file-options
  [["-f" "--file PATH" "File path to get file from."]
   ["-h" "--help"]])

(def import-options
  [["-h" "--help"]
   ["-d" "--delimiter DELIMITER" "Field separator."]
   ["-r" "--record-separator SEPARATOR" "String to split records on."]
   ]
  )

(def sub-options-map
  "define a map of sub commands, their options and a usage function."
  {:sftp  [sftp-options "get a file from an sftp server"]
   :s3    [s3-options "get an s3 file"]
   :file  [file-options "get a local file"]
   ;; Actions have no options spec.
   :start [nil "Start a new server"]
   :stop  [nil "Stop an existing server"]
   :restart [nil "Restart a Server"]})

(def log-options
  [["-l" "--logging" "Turn logging on"]
   ["-v" "--verbosity LEVEL" "Set the level of verbosity for logging."]
   ["-f" "--file FILE-PATH" "Set the log file path" :default "/var/log/foo.log"]])

(def test-options
  [["-P" "--port NUMBER" "access this port"
    :default 22
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number"]]
   ["-H" "--host HOST" "Connect to this hostname"
    :default-desc "localhost"
    ;:default (InetAddress/getByName "localhost")
    ;:parse-fn #(InetAddress/getByName %)
    ]
   ["-l" "--login ID" "Use this login id"]
   ["-p" "--password PASSWORD" "Use this password"]
   ["-h" "--help"]
   ["-l" "--logging"] "Turn logging on"
   ["-v" "--verbosity LEVEL" "Set the level of verbosity for logging."]
   ["-f" "--file FILE-PATH" "Set the log file path" :default "/var/log/foo.log"]
   ])

(def server-sub-commands
  "server actions."
  {:type :sub-command
   :parsers {:start   {:description "Start a new server"}
             :stop    {:description "Stop the server"}
             :restart {:description "Restart the server"}}})

(def import-sub-commands
  "define a map of sub commands, their options and a usage function."
  {:type :sub-command
   :parsers {:sftp  {:options sftp-options :description "get a file from an sftp server"}
             :s3    {:options s3-options :description "get an s3 file"}
             :file  {:options file-options :description "get a local file"}}})

; :type could be sub-command or group. Groups organized options in help, but no keyword to parse.
(def sub-parsers
  "define a vector map of sub-parsers"
  {:type :sub-command
   :parsers {:server {:options server-options :description "Configure and control the server"
                      :parse-groups server-sub-commands}

             :import {:options import-options :description "Settings for importing a file"
                      :parse-groups import-sub-commands}}})

(def global-parse-groups
  "Define global option groups. Groups organize options but do not provide a keyword for parsing."
  {:type :group
   :parsers {:logging {:options log-options :description "Configure and control logging."}
             :commands {:parse-groups sub-parsers :description "Sub Commands."}
             }
   }
  )

;; Look in :options, then :parse-groups then finally go to the :sub-parsers as the next node down.


(defn test-parse [pname args except]
  (let [except (or except :none)]
    (-> (clie/new-cli pname "1.0.0" "My test program does nothing.")
        (clie/new-main-parser nil global-parse-groups)
        (clie/on-exception except)
        (clie/parse args)
        (:parsed-options))))

(defn test-options-print [pname args options-vector except]
  (println (str "---------" (name (last options-vector)) "-----------"))
  (println (string/join \newline (get-in (test-parse pname args except) options-vector)))
  )

(defn test-throw [pname args]
  (try (test-parse pname args :throw)
       (catch clojure.lang.ExceptionInfo e
           (.getMessage e))))

(deftest nooptions
  (testing "no arguments passed"
    (is (= (test-parse "noargs" [] :none)
           {:main {:logging {:file "/var/log/foo.log"}}}))))

(deftest server-options
  (testing "Sever arguments"
    (is (= (test-parse "Server-args" server-args :none)
           {:main {:commands {:server {:start true, :port 8080}},
                   :logging {:file "/var/log/foo.log"}}}))))

(deftest logging-options
  (testing "logging args - global options as a sub parse group."
    (is (= (test-parse "logging-args" args4 :none)
           {:main {:logging {:verbosity "3", :logging true, :file "/var/log/foo.log"}}}))))

(deftest import-options
  (testing "import sub-commands sftp s3 and file"
    (testing "sftp command and options"
      (is (= (test-parse "sftp-args"  args1 :none)
             {:main {:commands
                     {:import {:sftp
                               {:password "foo", :login "eric", :port 8080 }}},
                     :logging {:file "/var/log/foo.log"}}})))

    (testing "s3 command and options"
      (is (= (test-parse "s3args"  args2 :none)
             {:main {:commands
                     {:import {:s3
                               {:file-key "filekey", :secret-key "mySecret",
                                :access-key "myaccesskey", :bucket "mybucket"}}},
                     :logging {:file "/var/log/foo.log"}}})))

    (testing "file command and options"
      (is (= (test-parse "fileargs"  args3 :none)
             {:main {:commands
                     {:import {:file
                               {:file "somefile/path.edn"}}},
                     :logging {:file "/var/log/foo.log"}}})))))

;; (deftest bad-options
;;   (testing "invalid options given with throw exception set."
;;     ( is (thrown? clojure.lang.ExceptionInfo
;;                   (test-options-print "bad-test" invalid-args-first [:main] :throw)))))

(deftest bad-options
  (testing "invalid options given with throw exception set."
    (testing  "invalid arguments at the front of the argument list."
      ( is (= (test-throw "bad-test" invalid-args-first)
              "The following errors occurred while parsing your command:\n\nUnrecognized options\n[\"-u foobar import sftp --port 8080 -l eric -p foo import s3 -b mybucket -a myaccesskey -s mySecret -f filekey import file -f somefile/path.edn server --port 8080 start\"]\n\n\nbad-test --help, for a summary of options.")))

   (testing "invalid arguments at the end of the argument list."
      ( is (= (test-throw "bad-test2" invalid-args-last)
             "The following errors occurred while parsing your command:\n\nUnrecognized options\n[\"import s3 -b mybucket -a myaccesskey -s mySecret -f filekey import file -f somefile/path.edn server --port 8080 start -u foobar\"]\n\n\nbad-test2 --help, for a summary of options.")))

   (testing "invalid arguments at the end of the argument list."
     ( is (= (test-throw "bad-test3" bad-args)
            "The following errors occurred while parsing your command:\n\nUnrecognized options\n[\"-u foobar\"]\n\n\nbad-test3 --help, for a summary of options.")))

  (testing "invalid arguments at the end of the argument list."
     ( is (= (test-throw "bad-test4" bad-args2)
             "The following errors occurred while parsing your command:\n\nUnrecognized options\n[\"foobar -u\"]\n\n\nbad-test4 --help, for a summary of options.")))

  (testing "Too many valid arguments - conflicting commands."
     ( is (= (test-throw "toomany" valid-args)
             "The following errors occurred while parsing your command:\n\nUnrecognized options\n[\"import s3 -b mybucket -a myaccesskey -s mySecret -f filekey import file -f somefile/path.edn server --port 8080 start\"]\n\n\ntoomany --help, for a summary of options."))) ))

(deftest help-options
  (testing "help, version, and sub-command help"
    (testing "top level help"
      (is (= (test-throw "basic-help" help-args)
            "basic-help :main Sub-Command help summary"))
      )
    (testing "version"
      (is (= (test-throw "version-help" help-args3)
            "version-help Version:"
            ))
      )
   (testing "sub-command help"
      (is (= (test-throw "s3-command-help" help-args2)
            "s3-command-help :s3 Sub-Command help summary"
            ))
      )
    )
  )


(defn tests
  []

  ;; an empty command line, --help and --version only.
  ;; Add them all at once.
  (def mycli
    (-> (clie/new-cli "mytest" "1.0.0" "My test program does nothing.")
        (clie/new-main-parser nil global-parse-groups)
        (clie/on-exception :none)
        (clie/parse args1)))

  (println (:parsed-options mycli))

  (println (clie/help-entry (clie/get-help-tree mycli :main (->> mycli :parsers :main))))

  (println (clie/summarize-entry (clie/get-help-tree mycli :main (->> mycli :parsers :main))))
  (println (clie/help-summary mycli))
  (println (clie/usage mycli "some summary" :main (->> mycli :parsers :main)))

  (clie/get-help-tree mycli :main (->> mycli :parsers :main))

  (test-options-print "MyTest" args2 [:main :commands :import :s3] :none)
  (test-options-print "MyTest" args3 [:main :commands :import :file] :none)
  (test-options-print "MyTest" server-args [:main :commands :server] :none)

  ;;; set to throw exception instead of exit on error,help or version.
  ;;; wrap with try/catch.
  (try
    (test-options-print "bad-test" invalid-args-first [:main] :throw)
    (catch clojure.lang.ExceptionInfo e
      (println "caught the exception")))
  (try
    (test-options-print "help-test" help-args [:main] :throw)
    (catch clojure.lang.ExceptionInfo e
      (println "caught the exception")))

  (try
    (test-options-print "sub-help-test" help-args2 [:main] :throw)
    (catch clojure.lang.ExceptionInfo e
      (println "caught the exception")
      (println (.getMessage e))
      ))



  )
