(ns org.eag.clie.config)

(def file-options
  [["-f" "--filename PATH" "File path to get file from."]
   ["-h" "--help"]])

(def sftp-options
  [["-P" "--port NUMBER" "access this port"
    :default 22
    :parse-fn #(Integer/parseInt %)
    :assert [#(< 0 % 0x10000) "%s is not a valid port number."]]
   ["-H" "--host HOST" "Connect to this hostname."
    :default-desc "localhost"]
   ["-l" "--login ID" "Use this login id."]
   ["-p" "--password PASSWORD" "Use this password."]
   ["-h" "--help"]])

(def s3-options
  [["-b" "--bucket S3-Bucket" "Bucket to get file from."]
   ["-a" "--access-key ACCESS-KEY" "S3 access key."]
   ["-s" "--secret-key SECRET-KEY" "S3 secret key."]
   ["-f" "--file-key FILE-KEY" "File-key to retrieve from bucket."]
   ["-h" "--help"]])

(def github-file-options
  [["-r" "--repository Repository" "GitHub repository name." :required "Repository"]
   ["-u" "--user UserName" "GitHub user name." :required "UserName"]
   ["-p" "--path Path" "Path of file to get." :required "Path"]
   ["-b" "--branch Branch" "Repository branch to query." :default "master"]
   ["-a" "--auth Name:Password" "Authorization for repository."]
   ["-h" "--help"]])

(def file-sub-commands
  "define a map of sub commands, their options and a usage function."
  {:type :sub-command
   :parsers {:sftp  {:options sftp-options :description "Get a file from an sftp server."}
             :s3    {:options s3-options :description "Get an s3 file."}
             :github {:options github-file-options :description "Get a file from a github repository."}
             :file  {:options file-options :description "Get a local file."}}})

(def config-file-options
  [["-s" "--snapshot" "Save a snapshot of the current merged configuration."]
   ["-r" "--replace"
    "Replace the current config with the current merge of the configuration and given commandline options."]
   ["-n" "--no-execute"
    "Do not execute anything, only process the configuration file and command line."]
   ["-h" "--help"]])

(def config-parser
  "define configuration file commands."
  {:type :sub-command
   :parsers {:config {:options config-file-options
                      :description "use and create config files."
                      :parse-groups file-sub-commands}}})

(def config-parser-group-entry
  "A config-file parser entry for easy addition to the base config group."
  {:config {:parse-groups config-parser
            :description "use and create config files."}})

(def config-parser-entry
  "A config-file parser entry for easy addition to the base config group."
  {:config {:options config-file-options
            :description "use and create config files."
            :parse-groups file-sub-commands}})
