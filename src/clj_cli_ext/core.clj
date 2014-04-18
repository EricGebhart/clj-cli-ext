(ns clj-cli-ext.core
  "An extension of clojure.tool.cli to simplify command line parsing."
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts summarize]])
  (:gen-class))

(declare get-actions-text)
(declare get-subcommands-text)

(defn help-header
  "Build the program's help header."
  [cli]
  (let  [name (:name cli )
         description (:description cli )
         version (:version cli )
         actions (doall (string/join \newline (get-actions-text cli )))]
    (->> [(string/join " " [name "Version:" version])
          ""
          description
          ""]
         (string/join \newline))))

(defn help-actions
  "Get the action command summary for help"
  [cli]
  (let [actions (get-actions-text cli)]
    (if (not (empty? actions))
      (string/join \newline ["" "Actions:"
                             (string/join \newline actions) ""])
      nil)))

(defn help-subcommands
  "Get the subcommand summary for help."
  [cli]
  (let [subcommands  (get-subcommands-text cli)]
    (if (not (empty? subcommands))
      (doall (string/join \newline ["" "Sub Commands:"
                                    (string/join \newline subcommands) ""]))
      nil)))

(defn usage
  "Basic help for the program."
  [cli summary]
  (let [name (:name cli)
        actions (help-actions cli)
        subcommands (help-subcommands cli)]
    (->> [(help-header cli)
          (string/join " " [ "Usage:" name "[options]"
                             (if (not (nil? subcommands))
                               (string/join " " ["<subcommand>" "[options]"]))
                             (if (not (nil? actions))
                               "action")])
          ""
          "Options:"
          summary
          (if (not (nil? subcommands))
            subcommands)
          (if (not (nil? actions))
            actions)
          (if (not (nil? subcommands))
            (string/join " " [(:name cli) "<command> --help for more information on each command"]))]
         (string/join \newline))))


(defn sub-command-usage
  "Basic help for any sub-command."
  [cli subcommand summary]
  (let [name (:name cli)
        actions (help-actions cli)]
    (->> [(help-header cli)
          (string/join " " ["Usage:" name "[options]" subcommand "[options]"
                            (if (not (nil? actions)) "action")])
          ""
          "Options:"
          summary
          (if (not (nil? actions))
            actions)]
         (string/join \newline))))

(defn new-cli
  "Initialize the command line interface map."
  [name version description]
  (let [version (if (nil? version) "1.0.0" version)
        description (if (nil? description) "This is a great program" description)]

    (-> {:name name :version version :description description}
        (assoc :global-options [["-h" "--help"] ["-V" "--version"]])
        (assoc :error-message
            "The following errors occurred while parsing your command:\n\n")
        (assoc :usage usage)
        (assoc :sub-command-usage sub-command-usage)
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

(defn add-global-options
  "Add on your own global options."
  [cli options]
  (let [global-options (:global-options cli )]
       (assoc cli :global-options
              (merge global-options options))))

(defn add-sub-command
  "Incrementally add sub-command options one at a time."
  [cli name-key options description]
  (let [sub-commands (:subcommands cli )]
       (assoc cli :subcommands
              (merge sub-commands
                     {name-key [options description]}))))

(defn set-sub-commands
  "Take a sub options map and add it directly.
  The map should be of the forme {:option-name [parse-opts-definition description] ...} "
  [cli sub-options-map]
  (assoc cli :subcommands sub-options-map))

(defn change-usage
  "Set your own usage function if you really want to."
  [cli usage-function]
  (assoc cli :usage usage-function))

(defn error-msg
  "Build a nice error message."
  [cli errors]
  (str (:error-message cli )
       (string/join \newline
                    (concat errors [(str (:name cli) " --help, for a summary of options." )]))))

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
                      :message ((:usage cli) cli summary)})))

  ([cli subcommand summary]
     (throw (ex-info (str (:name cli) " " subcommand " Sub-Command help summary")
                     {:type :usage :status 0 :cli cli
                      :message ((:sub-command-usage cli) cli subcommand summary)}))))

(defn unrecognized-options
 "When we have leftovers."
 [cli argv]
 (throw (ex-info (error-msg (string/join " " ["Unrecognized options" argv]))
                {:type :usage :status 0 :cli cli
                 :message ((:usage cli) cli (summarize (:global-options cli)))})))

(defn exit-condition
  "Check for errors, --version and --help. If any of the above
   do the right thing and throw an exception"
  [cli options errors & rest]
  (let [help-parms (cons cli rest)]
    (cond
     errors (error-exit cli errors)
     (:help options) (apply usage-exit help-parms)
     (:version options) (usage-exit cli)
     :else nil)))

(defn get-actions
   "Get all the possible actions from the sub-options-map"
   [opt-map]
   (filter identity
           (map #(when (nil? (first (% opt-map))) %)
                        (keys opt-map))))

(defn get-actions-text
  "For Help text. Returns all actions with their descriptions"
  [cli]
  (let [opt-map (:subcommands cli )]
    (map #(string/join " - " [(name %) (last (% opt-map))])
         (get-actions opt-map))))

(defn get-subcommands
   "Get all the possible sub commands from the sub-options-map"
   [opt-map]
   (filter identity
             (map #(when (not (nil? (first (% opt-map)))) %)
                  (keys opt-map))))

(defn get-subcommands-text
  "For Help text. Returns all actions with their descriptions"
  [cli]
  (let [opt-map (:subcommands cli )]
    (map #(string/join " - " [(name %) (last (% opt-map))]) (get-subcommands opt-map))))

 (defn find-actions
   "Find the actions in the parsed options."
   [cli]
   (select-keys (:parsed-sub-commands cli ) (get-actions (:subcommands cli ))))

(defn assoc-actions
  "Set the actions in the cli."
  [cli]
  (assoc cli :actions (find-actions cli)))

(defn get-sub-command-options
  "Try to get the sub-command options-map. If the first arg is not in the sub-command map
   it is an error. If found and options are nil it is an action."
  [args sub-options]
  (let [subcommand (keyword (first args))]
    (if (subcommand sub-options)
      {:subcommand subcommand
       :sub-options-map (first (subcommand sub-options))
       :errors nil}
      {:subcommand subcommand
       :sub-options-map nil
       :errors [(string/join " " ["Invalid sub-command:" (name subcommand)])]})))

(defn parse-sub-options
  "Try to parse the sub-command options. If there is no options-map it is an action
   or an error"
  [args sub-options]
  (let  [{:keys [subcommand sub-options-map errors]} (get-sub-command-options args sub-options)]
    (if sub-options-map
      (merge (parse-opts (rest args) sub-options-map :in-order true)
             {:subcommand subcommand})
        {:options true :arguments (rest args) :summary nil :errors errors :subcommand subcommand})))

 (defn get-subopts
  "Get map of all parsed sub command options."
  [cli args sub-options]
  (let [{:keys [subcommand options arguments summary errors]}
        (parse-sub-options args sub-options)]
    (if (nil? (exit-condition cli options errors subcommand summary))
      (let [options-map {subcommand options}]
        (if (not (empty? arguments))
          (merge options-map (get-subopts cli arguments sub-options))
          options-map)))))

(defn parse-globals
  "Parse the global options from the command line and set them in the record.
   Return the rest of the arguments and the cli."
  [cli argv]
  (let [{:keys [options arguments summary errors]}
        (parse-opts argv (:global-options cli ) :in-order true)]
    (if (nil? (exit-condition cli options errors summary))
      {:arguments arguments
       :cli (assoc cli :parsed-global-options options)})))

(defn parse-subcommands
  "Parse out the subcommands and set the action list, if there are
   any arguments left it's an error."
  [cli argv]
  (when argv
    (let [subcommands (:subcommands cli )]
      (if (and (empty? subcommands) argv)
        (unrecognized-options cli argv))
      (let [sub-options (get-subopts cli argv subcommands)]
        (-> cli
            (assoc :parsed-sub-commands sub-options)
            (assoc-actions))))))

(defn parse
  "Parse the arguments and fill in the cli map. Handle --help, --version and
   parsing errors."
  [cli argv]
  (try
    (let [{:keys [arguments cli]} (parse-globals cli argv)]
      (parse-subcommands cli arguments))

    (catch clojure.lang.ExceptionInfo e
      (let [{:keys [type status message cli]} (ex-data e)]
        (println message)
        (when (= :exit (:on-exception cli))
          (System/exit 0))
        (when (= :throw (:on-exception cli))
          (throw (ex-info (.getMessage e)
                          {:type type :status status :message message :cli cli})))))))
