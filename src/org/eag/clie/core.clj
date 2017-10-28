(ns org.eag.clie.core
  "An extension of clojure.tool.cli to simplify command line parsing."
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [org.eag.file-access.core :as fa]
            [org.eag.datetime-data.core :as dt]))


;; Summarize would have been great here, except it requires
;; compiled specs which is a private function. Parse-opts works.
(defn summary [option-spec]
  (let [{:keys [summary]} (parse-opts nil option-spec)]
    summary))

(declare help-tree-group)

;; create a tree from the parse tree specifically for creating help.
(defn help-tree-entry
  "create a help tree node from a parse tree node."
  [name parse-entry]
  (let [options (if (:options parse-entry) "[options]" "")
        summary (if (:options parse-entry) (summary (:options parse-entry)))
        description (:description parse-entry)
        type (:type (:parse-groups parse-entry))
        sub-options (if (not (empty? (:parse-groups parse-entry)))
                        (help-tree-group (:parse-groups parse-entry)) )]
    { :name name :options summary :description description type sub-options }))

(defn help-tree-group
  "process the entries in a parse group to create help nodes."
  [parse-group]
  (let [type (:type parse-group)
        parsers (:parsers parse-group)
        name (first (keys parsers))
        parse-entry  (name parsers)
        rest-parsers (into {} (rest parsers))
        text (help-tree-entry name parse-entry)]
    (if (not (empty? rest-parsers))
      (cons text (help-tree-group (assoc parse-group :parsers rest-parsers)) )
      [text])))


;; ;; this isn't quite right, but not sure I really need it to work this way.
;; (defn get-help-tree [cli key-vector]
;;   (help-tree-entry (last key-vector)
;;                    (get-in cli
;;                              (into [:parsers] key-vector)) ))

(defn get-help-tree [cli group-name parser]
  (help-tree-entry group-name parser))

(declare summarize-group)
(declare summarize-group2)

(defn summarize-entry [entry]
  (let [options (if (:options entry) "[options]")
        name (:name entry)
        commands (:sub-command entry)
        actions (:actions entry)
        group (:group entry)
        sub-tree (or commands actions group)
        type (cond commands :sub-command
                   actions :action
                   group :group
                   )
        summary [name options]
        ]
    (if (not (empty? sub-tree))
      (merge summary [type  (summarize-group sub-tree)])
      summary)))

(defn summarize-entry2 [parent entry]
  (let [options (if (:options entry) "[options]")
        pname (if (not (nil? parent)) (name (:name entry)))
        commands (:sub-command entry)
        actions (:actions entry)
        group (:group entry)
        sub-tree (or commands actions group)
        type (cond commands :commands
                   actions :actions)
        summary [(str/join " " (if (nil?  options) [(str pname)] [pname options]))]]
    (if (not (empty? sub-tree))
      (let [group-summary (summarize-group2 type sub-tree)]
        (if (or (= type  :commands) (= type :actions))
          (merge summary [(str "<" (str/join \newline (flatten group-summary)) ">\n")])
          (merge summary [type (summarize-group2 type sub-tree)])))
      summary)))


(defn summarize-entry3 [parent entry]
  (let [options (if (:options entry) "[options]")
        pname (if (not (nil? parent)) (name (:name entry)))
        commands (:sub-command entry)
        actions (:actions entry)
        group (:group entry)
        sub-tree (or commands actions group)
        type (cond commands :commands
                   actions :actions)
        summary (str  pname " " options)]
    (if (not (empty? sub-tree))
      (str summary "< " type " " (summarize-group2 type sub-tree))
      (str summary "\n"))))

;(keep identity (flatten  (clie/summarize-entry2 nil (clie/get-help-tree mycli))))

(defn summarize-group [group]
  (let [entry (first group)
        summary (summarize-entry entry)]
    (if (not (empty? (rest group)))
      (cons summary (summarize-group (rest group)))
      [summary])))

(defn summarize-group2 [parent group]
  (let [entry (first group)
        summary (summarize-entry2 parent entry)]
    (if (not (empty? (rest group)))
      (cons summary (summarize-group2 parent (rest group)))
      [summary])))

(defn get-summary-entry [tree]
  (let [options (:options tree)
        commands (:sub-commands tree)
        actions (:actions tree)
        sub-tree (or commands actions)
        text (->> [(if options "[options]")
                   (if commands " [commands]")
                   (if actions " [actions]")]
                  (str/join " "))]
    (if sub-tree
      (->> [text  (get-summary-entry sub-tree)] (str/join " "))
      text)))


(declare help-group)

(defn help-entry [entry]
  (let [{:keys [options sub-command actions description sub-command group]} entry
        gname (str/capitalize (name (:name entry)))
        sub-tree (or sub-command actions group)
        type (cond sub-command :commands
                   actions :actions)
        summary (->> [\newline gname "--" description \newline
                      (if options (str/join \newline ["Options:" options]))]
                     (str/join " "))]

    (if (not (empty? sub-tree))
      (let [group-help(help-group sub-tree)]
        (str/join "\n" [summary (help-group sub-tree)]))
      summary)))

(defn help-group [group]
  (let [entry (first group)
        summary (help-entry entry)]
    (if (not (empty? (rest group)))
      (str/join [summary (help-group (rest group))])
      summary)))

(defn help-summary
  "Create a summary of the command line for the program."
  [cli]
  (->> (get-help-tree cli :main (->> cli :parsers :main))
       (summarize-entry2 nil)
       (flatten)
       (into #{})
       (drop-while empty?)
       (cons (:name cli))
       (str/join \newline)))

;; TODO:
;; this should work for sub help, just pass the piece of
;; the parse tree to get-help-tree.
(defn help-complete [cli group-name parser]
  (help-entry (get-help-tree cli group-name parser)))


(defn help-header
  "Build the program's help header."
  [cli]
  (let  [name (:name cli )
         description (:description cli )
         version (:version cli )]
    (->> [(str/join " " [name "Version:" version])
          ""
          description
          ""]
         (str/join \newline))))

;; TODO: This should work for sub help too. Just need to know
;; which part of the tree to render.
(defn usage
  "Basic help for the program."
  [cli summary group-name parser]
  (str/join \newline
            [(help-header cli)
             (help-summary cli)
             (help-complete cli group-name parser)]))


(defn new-cli
  "Initialize the command line interface map."
  [name version description config-name]
  (let [version (if (nil? version) "1.0.0" version)
        description (if (nil? description) "This is a great program" description)]

    (-> {:name name :version version :description description :config-name config-name}
        (assoc :global-options [["-h" "--help"] ["-V" "--version"]])
        (assoc :error-message
               "The following errors occurred while parsing your command:\n\n")
        (assoc :usage usage)
        (assoc :sub-command-usage usage)
        (assoc :on-exception :exit)

        ;;;;;;;;;;;;;;;- these get filled in when we parse.
        (assoc :subcommands {})
        (assoc :parsed-sub-commands{})
        (assoc :parsed-global-options {})
        (assoc :actions {}))))

(defn exit-on-exception
  "Set behavior to system exit on error, help or version.
   true or nil."
  [cli setting]
  (let [setting (if setting :exit :none)]
    (assoc cli :on-exception setting)))

(defn throw-on-exception
  "Set behavior to throw exception on error, help or version.
   true  or nil."
  [cli setting]
  (let [setting (if setting :throw :none)]
    (assoc cli :throw-on-exception setting)))

(defn on-exception
  "Set behavior for exception handling. :exit :throw or :none.
   Behavior applies to --help, --version and errors."
  [cli setting]
  (assoc cli :on-exception setting))

(defn change-usage
  "Set your own usage function if you really want to."
  [cli usage-function]
  (assoc cli :usage usage-function))

(defn error-msg
  "Build a nice error message."
  [cli errors]
  (str (:error-message cli )
       (str/join \newline
                 (concat errors [(str \newline \newline (:name cli) " --help, for a summary of options.")]))))

(defn error-exit
  "Set the proper error message and throw an exception."
  [cli errors]
  (throw (ex-info (:error-message cli)
                  {:type :error :status 1 :cli cli
                   :message (error-msg cli errors)})))

(defn usage-exit
  "Set the proper usage message and throw an exception."
  ([cli]
     (throw (ex-info (str (:name cli) " Version:")
                     {:type :version :status 0 :cli cli
                      :message (:version cli)})))

  ([cli summary]
     (throw (ex-info (str (:name cli) " Help Summary")
                     {:type :usage :status 0 :cli cli
                      :message ((:usage cli) cli summary :main (get-in cli [:parsers :main]))})))

  ([cli group-name parser summary]
     (throw (ex-info (str (:name cli) " " group-name " Sub-Command help summary")
                     {:type :usage :status 0 :cli cli
                      :message ((:sub-command-usage cli) cli summary group-name parser)}))))

(defn unrecognized-options
  "When we have leftovers."
  [cli argv]
  (throw (ex-info (error-msg cli (merge ["Unrecognized options"] [(str/join " " argv)]))
                  {:type :usage :status 1 :cli cli
                   :message ((:usage cli) cli summary :main (get-in cli [:parsers :main]))})))

(defn exit-condition
  "Check for errors, --version and --help. If any of the above
   do the right thing and throw an exception"
  [cli group-name parser options errors & rest]
  (let [help-parms (merge [cli group-name parser] rest)
        unknowns (count (map #(re-matches #"Unknown.*" %) errors))]
    (cond
     (> unknowns 0) :unknown
     (not (empty? errors)) (error-exit cli errors)
     (:help options) (apply usage-exit help-parms)
     (:version options) (usage-exit cli)
     :else nil)))

(defn add-default-globals
  "add the global options (--version --help) in."
  [cli options]
  (concat options (:global-options cli)))

(defn new-main-parser
  "Create the main cli parser"
  [cli options parse-groups]
  (assoc cli :parsers {:main {:options (add-default-globals cli options)
                                :parse-groups parse-groups}}))
(defn add-parsed-options
  "Put the parsed options into the tree."
  [cli options key-vector]
  (let [parsed-opts (:parsed-options cli)
        parsed-opts (assoc-in parsed-opts key-vector options)]
    (assoc cli :parsed-options parsed-opts)))

(defn add-action
  "Put the parsed options into the tree."
  [cli key-vector]
  (let [parsed-opts (:parsed-options cli)
        parsed-opts (assoc-in parsed-opts key-vector true)]
    (assoc cli :parsed-options parsed-opts)))

(defn parse-options
  "Parse the options for the current parser."
  [cli key-vector argv options group-name parser]
  (let [{:keys [options arguments summary errors]}
        (parse-opts argv options :in-order true)]
    (let [exit (exit-condition cli group-name parser options errors summary)]
      (cond
       (nil? exit) {:arguments arguments :cli (add-parsed-options cli options key-vector)}
       (= exit :unknown) {:arguments argv :cli cli}))))

;; (assoc-in nil [:a :long :non-existant :path] "value")
;; {:a {:long {:non-existant {:path "value"}}}}

;if subcommand find match for next argument. - parse it's optons and parse groups.
;else parse each parser's options and parse groups in turn.

(declare parse-group)

(defn parse-group-entry
  "Parse argumens with an entry from parse-group for its options and recurse
   through it's parse-groups."
  [cli key-vector group-name parser arguments]
  (let [key-vector (if-not (= (last key-vector) group-name)
                     (merge key-vector group-name)
                     key-vector)
        options (:options parser)
        parse-groups (:parse-groups parser)
        {:keys [arguments cli]} (if options
                                  (parse-options cli key-vector arguments options group-name parser)
                                  {:arguments arguments :cli cli})]
    (if parse-groups
      (parse-group cli key-vector parse-groups arguments)
      (if (nil? (get-in (:parsed-options cli) key-vector))
        (let [action-vector (get-in cli key-vector)
              cli (add-action cli key-vector)]
          {:cli cli :arguments arguments})
        {:cli cli :arguments arguments}))
    ))

(defn parse-subcommand
  [cli key-vector parse-group arguments]
  (let [sub-command (keyword (first arguments))
        parsers (:parsers parse-group)
        parser (sub-command parsers)
        type (:type parse-group)]

    (if  parser
      (parse-group-entry cli
                         key-vector
                         sub-command
                         parser
                         (rest arguments))
      {:cli cli :arguments arguments})))

; (get-in (:parsers cli) (concat key-vector :help))

(defn parse-group-entries
 "loop through non sub-command group of parser entries."
 [cli key-vector parsers arguments]
 (let [group-name (first (keys parsers))
       parser-entry (group-name parsers)
       rest-parsers (into {} (rest parsers))
       {:keys [cli arguments]} (parse-group-entry cli
                                                  key-vector
                                                  group-name
                                                  parser-entry
                                                  arguments)]
   (if (not (empty? rest-parsers))
      (parse-group-entries cli key-vector rest-parsers arguments)
      {:cli cli :arguments arguments})))

(defn parse-group
  "Iterate over a parse group and parse the options and sub-parse-groups."
  [cli key-vector parse-group arguments]
  (let [type (:type parse-group)
        parsers (:parsers parse-group)]
    (if (= type :sub-command)
      (if (not (empty? arguments))
        (parse-subcommand cli key-vector parse-group arguments)
        {:cli cli :arguments arguments})
      (parse-group-entries cli key-vector parsers arguments))))

;; Look in :options, then :parse-groups, finally go to the :sub-parsers as the next node down.
;; default help and version are in options.
(defn handle-catch [cli e]
  (let [{:keys [type status message cli]} (ex-data e)]
    (println (.getMessage e))
    (println message)
    (when (= :exit (:on-exception cli))
      (System/exit 0))
    (when (= :throw (:on-exception cli))
      (throw (ex-info (.getMessage e)
                      {:type type :status status :message message :cli cli})))))

(defn parse
  "Parse the arguments and fill in the cli map. Handle --help, --version and
   parsing errors."
  [cli argv]
  (try
    (let [{:keys [arguments cli]}
          (parse-group-entry cli [] :main (get-in cli [:parsers :main]) argv)]

      (if (not (empty? arguments))
        (try (unrecognized-options cli arguments)
             (catch clojure.lang.ExceptionInfo e
               (handle-catch cli e)))
        cli))

    (catch clojure.lang.ExceptionInfo e
      (handle-catch cli e))))

;;;;;;;; Configuration file management

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))

(defn load-current-config [name]
  (let [config (fa/slurp-file
                {:source :file
                 :filename (str/join "" [name ".edn"])})]
    (when config (read-string config))))

(defn snapshot-config [name c]
  (when (get-in c [:config :snapshot])
    (let [filename (str/join "_" [name (dt/now-str) ".edn"])]
      (spit filename c)))
  c)

(defn replace-current-config
  "if --replace on the cli, make a timestamped
  snapshot and put in place of current config file."
  [name c]
  (when (get-in c [:config :replace])
    (spit (str/join "" ["new-config-" (dt/now-str) ".edn"]) c)
    (spit (str/join "" [name ".edn"]) c))
  c)

;;; We need to catch exceptions here for read-string. - a failure indicates an
;;; invalid edn file.
(defn load-new-config
  "load a new config file from the command line args."
  [options cli]
  (try
    (let [config-opt (:config options)
          config (merge (read-string
                         (fa/slurp-file-cmdline config-opt))
                        {:cli- options})]
      (replace-current-config (:config-name cli) config)) ;; if --replace
    (catch Exception e
      nil)))

(defn load-config [options cli]
  (let [config (load-new-config options cli)
        snapshot (get-in options [:config :snapshot])
        replace (get-in options [:config :replace])
        config-name (:config-name cli)
        config (if config
                 (deep-merge config options)
                 (deep-merge (load-current-config config-name) options))]
    (when snapshot
      (snapshot-config config-name config))
    (when replace
      (replace-current-config config-name config))
    config))

(defn do-parse [args parse-group
                {:keys [pname version description exception config-name]
                 :or {pname "test"
                      version "1.0.0"
                      description "This test program does nothing."
                      exception :none
                      config-name "current-config"}}]
  (let [cli (new-cli pname version description config-name)
        config (-> cli
                   (new-main-parser nil parse-group)
                   (on-exception exception)
                   (parse args)
                   (:parsed-options)
                   (:main)
                   (load-config cli))]
    (if (get-in config [:config :no-execute])
      (System/exit 0)
      config)))
