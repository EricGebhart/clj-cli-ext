(ns clj-cli-ext.core-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clj-cli-ext.core :as clie]
            [clj-cli-ext.example :as ex]
            [clj-cli-ext.example2 :as ex2]
            [clojure.string :as string]
            [clojure.pprint :as pprint]))

;;; this needs to be turned into a real test...
(def args1 ["import" "sftp" "--port" "8080" "-l" "eric" "-p" "foo"])
(def args2 ["import" "s3" "-b" "mybucket" "-a" "myaccesskey" "-s" "mySecret" "-f" "filekey"])
(def args3 ["import" "file" "-f" "somefile/path.edn"])
(def logging-args ["-l" "-v" "3"])
(def server-args ["server" "--port" "8080" "start"])
(def bad-args ["-u" "foobar"])
(def bad-args2 ["foobar" "-u"])
(def help-args ["--help"])
(def version-help-args ["--version"])
(def sub-help-args ["import" "s3" "--help"])
(def too-many-valid-args (concat args1 args2 args3 server-args))
(def invalid-args-last (concat valid-args bad-args))
(def invalid-args-first (concat bad-args valid-args))

(defn test-parse [pname args except]
  (let [except (or except :none)]
    (-> (clie/new-cli pname "1.0.0" "My test program does nothing.")
        (clie/new-main-parser nil ex2/global-parse-groups)
        (clie/on-exception except)
        (clie/parse args)
        (:parsed-options))))


(defn test-throw [pname args]
  (try (test-parse pname args :throw)
       (catch clojure.lang.ExceptionInfo e
           (.getMessage e))))


;;; Basic tests for some internals.

(facts "there are various ways to access the cli tree."
       (let [mycli
             (-> (clie/new-cli "mytest" "1.0.0" "My test program does nothing.")
                 (clie/new-main-parser nil ex2/global-parse-groups)
                 (clie/on-exception :none)
                 (clie/parse args1))]

         (fact "check we have a cli"
               mycli =not=> nil)

         (fact "we can get the parsed options from it"
               (:parsed-options mycli)  => {:main {:commands {:import
                                                              {:sftp {:login "eric",
                                                                      :password "foo",
                                                                      :port 8080}}},
                                                   :logging {:file "/var/log/foo.log"}}})

         (fact "we can get help from it"
               (clie/help-entry
                (clie/get-help-tree
                 mycli
                 :main
                 (->> mycli :parsers :main))) =>
                 (contains "\n Main --  \n Options:\n  -h, --help\n  -V, --version" :gaps-ok))

         (fact "we can summarize an entry"
               (clie/summarize-entry (clie/get-help-tree mycli :main (->> mycli :parsers :main))) =>
                '[:main "[options]"
                 [:group ([:logging "[options]"]
                            [:commands nil [:sub-command ([:server "[options]"
                                                           [:sub-command ([:restart nil] [:start nil] [:stop nil])]]
                                                          [:import "[options]"
                                                           [:sub-command
                                                            ([:sftp "[options]"]
                                                             [:s3 "[options]"]
                                                             [:file "[options]"])]])]])]])

         (fact "we can get a full help summary"
               (clie/help-summary mycli) => (contains "mytest\n [options]\n<server [options]\n<restart\nstart\nstop>"))

         (fact "we can get usage"
               (string/split-lines (clie/usage mycli "some summary" :main (->> mycli :parsers :main))) =>
               (contains ["mytest Version: 1.0.0" ""
                          "My test program does nothing." ""
                          "mytest" " [options]"] :gaps-ok))

         (fact "there is a help tree"
               (keys (clie/get-help-tree mycli :main (->> mycli :parsers :main)))  => '(:name :options :description :group))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests using the options definitions from example2.clj
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(tabular
    (facts "example 2 argument definitions"
        (fact "The arguments become a useful tree."
              (test-parse ?pname ?args ?except) => ?result))

    ?pname         ?args         ?except      ?result
    "noargs"       []            :none        {:main {:logging {:file "/var/log/foo.log"}}}

    "Server-args"  server-args   :none        {:main {:commands {:server {:start true, :port 8080}},
                                                     :logging {:file "/var/log/foo.log"}}}

    "logging-args" logging-args  :none        {:main {:logging {:verbosity "3", :logging true,
                                                               :file "/var/log/foo.log"}}}

    "sftp-args"    args1         :none        {:main {:commands {:import {:sftp
                                                                         {:password "foo",
                                                                          :login "eric",
                                                                          :port 8080 }}},
                                                     :logging {:file "/var/log/foo.log"}}}

    "s3args"       args2         :none       {:main {:commands {:import
                                                                {:s3 {:file-key "filekey",
                                                                      :secret-key "mySecret",
                                                                      :access-key "myaccesskey",
                                                                      :bucket "mybucket"}}},
                                                     :logging {:file "/var/log/foo.log"}}}

    "fileargs"     args3         :none       {:main {:commands {:import
                                                                {:file {:file "somefile/path.edn"}}},
                                                     :logging {:file "/var/log/foo.log"}}})


(tabular
 (facts "example2: we can make cli throw exceptions and give help."
        (fact "Exceptions can be handled easily."
              (test-throw ?pname ?args) => ?result))

  ?pname       ?args                  ?result
  "bad-test"   invalid-args-first     (contains "Unrecognized options\n[\"-u foobar import sftp" :gaps-ok)

  ;; The following errors occurred while parsing your command:
  ;; Unrecognized options
  ;; [-u foobar import sftp --port 8080 -l eric -p foo
  ;; import s3 -b mybucket -a myaccesskey -s mySecret -f filekey
  ;; import file -f somefile/path.edn
  ;; server --port 8080 start]
  ;; bad-test --help, for a summary of options.


  "bad-test2"  invalid-args-last      (contains "bad-test2 --help, for a summary of options" :gaps-ok)

  ;; The following errors occurred while parsing your command:
  ;; Unrecognized options
  ;; ["import s3 -b mybucket -a myaccesskey -s mySecret -f filekey
  ;; import file -f somefile/path.edn server --port 8080 start -u foobar
  ;; bad-test2 --help, for a summary of options.


  "bad-test3"  bad-args                (contains "bad-test3 --help, for a summary of options" :gaps-ok)

  ;; The following errors occurred while parsing your command:
  ;; Unrecognized options\n[\"-u foobar\"]\n\n\nbad-test3 --help, for a summary of options.


  "bad-test4"  bad-args2               (contains "Unrecognized options\n[\"foobar -u\"]\n\n\nbad-test4 --help")

                                        ;The following errors occurred while parsing your command:
                                        ;Unrecognized options\n[\"foobar -u\"]
                                        ;bad-test4 --help, for a summary of options.


  ;;"Too many valid arguments - conflicting commands."
  "toomany"    too-many-valid-args      (contains "The following errors occurred while parsing your command:" :gaps-ok)

  ;; The following errors occurred while parsing your command:
  ;; Unrecognized options
  ;; [import s3 -b mybucket -a myaccesskey -s mySecret -f filekey
  ;;  import file -f somefile/path.edn
  ;;  server --port 8080 start]
  ;; toomany --help, for a summary of options."

  ;; "help, version, and sub-command help"
  ;; "top level help"
  "basic-help"      help-args          "basic-help :main Sub-Command help summary"

  "version-help"    version-help-args  "version-help Version:"

  "s3-command-help" sub-help-args      "s3-command-help :s3 Sub-Command help summary")







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests using the options definitions from example.clj
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parseargs [args]
  (clie/do-parse args ex/main-parse-group
                 {:pname "An example"
                  :version "1.0.1"
                  :description "this is an example."
                  :exception :none}))


(tabular
 (facts "There are a ton of arguments that can be passed on the commandline."
        (fact "Turns the arguments into a useful tree."
              (parseargs ?args) => ?result))

 ?args                                            ?result
 ["foo"]                                          nil  ;prints help.

 []                                               '{:main {:logging {:file "/var/log/foo.log"}, :system {}}}

 ["process" "-s" "Set1" "-n" "fullnames"]         '{:main {:process
                                                           {:process {:name "fullnames",
                                                                      :scope :all,
                                                                      :set "Set1"}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["process" "-S" "project" ]                      '{:main {:process
                                                           {:process
                                                            {:scope :project}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["process" "-n" "fullnames" "-s" "set1"]         '{:main {:process
                                                           {:process
                                                            {:name "fullnames",
                                                             :scope :all,
                                                             :set "set1"}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["stack" "file" "-f" "yetistack.edn"]            '{:main {:stack
                                                           {:stack {:file
                                                                    {:file "yetistack.edn"}}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["import" "-n"]                                  '{:main {:import
                                                           {:import {:none true}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["import" "-s" "set1"]                           '{:main {:import
                                                           {:import {:set "set1"}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["import" "-s" "set1" "-l" "10"]                 '{:main {:import
                                                           {:import {:limit 10N, :set "set1"}},
                                                           :logging {:file "/var/log/foo.log"}
                                                           :system {}
                                                           }}

 ["-f" "logfile" ]                                '{:main {:logging {:file "logfile"},
                                                           :system true}}

 ["-r" "test"]                                    '{:main {:logging {:file "/var/log/foo.log"},
                                                            :system {:root "test"}}}

 ["stack" "file" "-f" "foo.edn" "import" "-s" "set1" "-l" "10"]  '{:main {:import
                                                                                  {:import {:limit 10N, :set "set1"}},
                                                                                  :logging {:file "/var/log/foo.log"},
                                                                                  :stack
                                                                                  {:stack {:file {:file "foo.edn"}}},
                                                                                  :system {}}})


;; just some stuff for fun. digging into the options tree.
#_(defn test-options-print [pname args options-vector except]
  (println (str "---------" (name (last options-vector)) "-----------"))
  (println (string/join \newline (get-in (test-parse pname args except) options-vector))))

#_(defn tests
  []

  ;; an empty command line, --help and --version only.
  ;; Add them all at once.
  (def mycli
    (-> (clie/new-cli "mytest" "1.0.0" "My test program does nothing.")
        (clie/new-main-parser nil ex2/global-parse-groups)
        (clie/on-exception :none)
        (clie/parse args1)))


  (test-options-print "MyTest" args2 [:main :commands :import :s3] :none)
  (test-options-print "MyTest" args3 [:main :commands :import :file] :none)
  (test-options-print "MyTest" server-args [:main :commands :server] :none))
