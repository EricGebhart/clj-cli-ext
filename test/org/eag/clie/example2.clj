(ns org.eag.clie.example2
  (:require [org.eag.clie.config :as conf]))

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
  [["-f" "--filename PATH" "File path to get file from."]
   ["-h" "--help"]])

(def import-options
  [["-h" "--help"]
   ["-d" "--delimiter DELIMITER" "Field separator."]
   ["-r" "--record-separator SEPARATOR" "String to split records on."]])

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
   ["-f" "--file FILE-PATH" "Set the log file path" :default "/var/log/foo.log"]])

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

;; a group means that all commands can be specified simultaneously.  A subcommand is exclusive,
;; only one of them can be specified.  In the subcommand parser above there is a choice. server or import.
;; here both logging and config can be specified along with server or import...
(def global-parse-groups
  "Define global option groups. Groups organize options but do not provide a keyword for parsing."
  {:type :group
   :parsers (merge {:logging {:options log-options :description "Configure and control logging."}
                    :commands {:parse-groups sub-parsers :description "Sub Commands."}}
                   conf/config-parser-entry)})
