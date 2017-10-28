(ns org.eag.clie.example
  (:require [org.eag.clie.config :as conf]))

(def cli-options
  ;; An option with a required argument
  [["-s" "--source File Source" "Local, S3, SFTP."
    :default "local"
    :validate [#(< (key %) (:local :S3 :SFTP)) "Must be local, S3 or SFTP."]]
  ["-f" "--file File Path" "Path to the stack definition file."
    :default "foo.edn"]
  ["-i" "--id user id" "User id for SFTP or S3"]
  ["-p" "--password user pasword" "User password for SFTP or S3"]
  ["-s" "--server file server" "Server for SFTP."]
  ["-b" "--bucket S3 bucket" "S3 bucket to retrieve file from."]

   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])


(def sftp-options
  [[nil "--port NUMBER" "access this port"
    :default 22
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number."]]
   [nil "--host HOST" "Connect to this hostname."
    :default-desc "localhost"
    ;:default (InetAddress/getByName "localhost")
    ;:parse-fn #(InetAddress/getByName %)
    ]
   ["-l" "--login ID" "Use this login id."]
   ["-p" "--password PASSWORD" "Use this password."]
   ["-h" "--help"]])

(def s3-options
  [["-b" "--bucket S3-Bucket" "Bucket to get file from."]
   ["-a" "--access-key ACCESS-KEY" "S3 access key."]
   ["-s" "--secret-key SECRET-KEY" "S3 secret key."]
   ["-f" "--file-key FILE-KEY" "File-key to retrieve from bucket."]
   ["-h" "--help"]])

(def file-options
  [["-f" "--filename PATH" "File path to get file from."]
   ["-h" "--help"]])

(def github-file-options
  [["-r" "--repository Repository" "GitHub repository name." :required "Repository"]
   ["-u" "--user UserName" "GitHub user name." :required "User Name"]
   ["-p" "--path Path" "Path of file to get." :required "File Path"]
   ["-b" "--branch Branch" "Repository branch to query." :default "master"]
   ["-a" "--auth Name:Password" "Authorization for repository."]
   ["-h" "--help"]])

(def import-stack-options
  [["-h" "--help"]
   ["-d" "--delimiter DELIMITER" "Field separator."]
   ["-r" "--record-separator SEPARATOR" "String to split records on."]])

(def import-sub-options
 [["-h" "--help"]
   ["-l" "--limit recordLimit" "Number of records to import."
    :parse-fn #(bigint %)]
   ["-s" "--set DataSetName" "Name of dataset to import."]
   ["-n" "--none" "Do not execute imports"]   ;;; I don't really like these. Need a discussion about behavior.
   ["-a" "--all" "Execute imports"]
   ;; ["-S" "--scope ImportScope" "Import scope, all or none."
   ;;  :default :none
   ;;  :parse-fn #(keyword %)
   ;;  :validate [#(contains? #{:all :none} %) "All or none."]]
   ])

(def process-sub-options
 [["-h" "--help"]
   ["-d" "--dataset" "Run dataset process modules."]
   ["-S" "--scope ProcessScope" "The scope of the processes to run."
   :default  :all
   :parse-fn #(keyword %)
   :validate [#(contains? #{:project :dataset :all :none} % ) "Must be project, dataSet, all or none."]
   ]
   ["-n" "--name ProcessName" "Name of processing module to run."]
   ["-s" "--set DataSetName" "Dataset to run process on. Implies -d."]
   ["-p" "--pail Pail Name" "Dataset pail to run process on. If not set process will run against all datasets."]
   ["-a" "--all" "Run all global process modules."]])

(def sub-options-map
  "define a map of sub commands, their options and a usage function."
  {:sftp  [sftp-options "Get a file from an sftp server."]
   :s3    [s3-options "Get an s3 file."]
   :file  [file-options "Get a local file."]})

(def system-options
  [["-r" "--root SYSTEMROOT" "change the root of the system. Mostly for testing."]
   ["-i" "--do-import" "run the imports."]
   ["-p" "--do-process" "run the processes."]])

(def log-options
  [["-l" "--logging" "Turn logging on"]
   ["-v" "--verbosity LEVEL" "Set the level of verbosity for logging."]
   ["-f" "--file FILE-PATH" "Set the log file path." :default "/var/log/foo.log"]])

(def import-sub-commands
  "define a map of sub commands, their options and a usage function."
  {:type :sub-command
   :parsers {:sftp  {:options sftp-options :description "Get a file from an sftp server."}
             :s3    {:options s3-options :description "Get an s3 file."}
             :github {:options github-file-options :description "Get a file from a github repository."}
             :file  {:options file-options :description "Get a local file."}}})

; :type could be sub-command or group. Groups organized options in help, but no keyword to parse.
(def sub-parsers
  "define a vector map of sub command parsers"
  {:type :sub-command ; :group ;; we can have more than one of these...
   :parsers {
             :stack {:options import-stack-options :description "Settings for importing a stack definition file."
                      :parse-groups import-sub-commands}
             :import {:options import-sub-options :description "Run import for one or all set definitions."}
             :process {:options process-sub-options :description "Run specific processes."}

             }})

(def stack-parser
  "Options for stack initialization."
  {:type :sub-command ; :group ;; we can have more than one of these...
   :parsers {:stack {:options import-stack-options :description "Settings for importing a stack definition file."
                      :parse-groups import-sub-commands}}})

(def import-parser
  "Options for the import engine"
  {:type :sub-command ; :group ;; we can have more than one of these...
   :parsers {:import {:options import-sub-options :description "Run import for one or all set definitions."}}})

(def process-parser
  "Options for the process engine"
  {:type :sub-command ; :group ;; we can have more than one of these...
   :parsers {:process {:options process-sub-options :description "Run specific processes."}}})

(def main-parse-group
  "Define global option groups. Groups organize options but do not provide a keyword for parsing."
  {:type :group
   :parsers (merge {:system {:options system-options :description "options for system control"}
                    :logging {:options log-options :description "Configure and control logging."}
                                        ;:commands {:parse-groups sub-parsers :description "Sub Commands."}
                    :stack {:parse-groups stack-parser :description "Stack command options group."}
                    :import {:parse-groups import-parser :description "Import command options group."}
                    :process {:parse-groups process-parser :description "Process command options group."}}
                   conf/config-parser-group-entry)})

(def main-parse-group-mutually-exclusive
  "Define global option groups. Commands are mutually exclusive."
  {:type :sub-command
   :parsers (merge conf/config-parser-group-entry
                   {:system {:options system-options :description "options for system control"}
                    :logging {:options log-options :description "Configure and control logging."}
                    :commands {:parse-groups sub-parsers :description "Sub Commands."}})})
