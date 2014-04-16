(ns clj-cli-ext.core
  "An extension of clojure.tool.cli to simplify command line parsing."
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defrecord Cli [name version description])

(declare get-actions-text)
(declare get-subcommands-text)

(defn usage [cli summary]
  (let [name (:name cli )
        description (:description cli )
        version (:version cli )
        actions (doall (string/join \newline (get-actions-text cli )))
        subcommands (doall (string/join \newline (get-subcommands-text cli )))]
    (->> [name "  Version: " version
          ""
          description
          ""
          (string/join " " [ "Usage:" name "[options]"
                             (if subcommands (string/join " " ["<subcommand>" "[options]"]))
                             (if (not (nil? actions)) "action")])
          ""
          (if (not (nil? subcommands))
            (string/join \newline ["Sub Commands:" subcommands]))
          ""
          "Options:"
          summary
          ""
          (if (not (nil? actions))
            (string/join \newline
                         ["Actions:"
                          actions
                          ""
                          "See" name "<command> --help for more information on each command"]))]
         (string/join \newline))))

(defn sub-command-usage [cli subcommand summary]
  (let [name (:name cli )
        description (:description cli )
        version (:version cli )
        actions (doall (string/join \newline (get-actions-text cli )))]
    (->> [name "  Version: " version
          ""
          description
          ""
          (string/join " " ["Usage:" name "[options]" subcommand "[options]"
                             (if (not (nil? actions)) "action")])
          ""
          "Options:"
          summary
          ""
          (if (not (nil? actions))
            (string/join \newline
                         ["Actions:"
                           actions
                           ""]))]
         (string/join \newline))))

(defn new-cli [name version description]
  (let [version (if (nil? version) "1.0.0" version)
        description (if (nil? description) "This is a great program" description)]

    (-> (->Cli name version description)
        (assoc :global-options [["-h" "--help"] ["-V" "--version"]])
        (assoc :usage 'usage)
        (assoc :subcommands {})
        (assoc :parsed-sub-commands{})
        (assoc :parsed-global-options {})
        (assoc :actions {})
        (assoc :sub-command-usage 'sub-command-usage)
        (assoc :error-message
          "The following errors occurred while parsing your command:\n\n"))))


(defn add-global-options [cli options]
  (let [global-options (:global-options cli )]
       (assoc cli :global-options
              (merge global-options options))))

(defn add-sub-command [cli name-key options description]
  (let [sub-commands (:subcommands cli )]
       (assoc cli :subcommands
              (merge sub-commands
                     {name-key [options description]}))))

(defn set-sub-commands
  "Take a sub options map and add it directly.
  The map should be of the forme {:option-name [parse-opts-definition description] ...} "
  [cli sub-options-map]
  (assoc cli :subcommands sub-options-map))

(defn change-usage [cli usage-function]
  (assoc cli :usage usage-function))

(defn error-msg [cli errors]
  (str (:error-message cli )
       (string/join \newline errors)))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn get-actions
   "Get all the possible actions from the sub-options-map"
   [opt-map]
   (filter identity
           (map #(when (nil? (first (% opt-map))) %)
                        (keys opt-map))))

(defn get-actions-text
  "For Help text. Returns all actions with their descriptions"
  [cli ]
  (let [opt-map (:subcommands cli )]
    (map #(string/join " - " [(name %) (last (% opt-map))]) (get-actions opt-map))))

(defn get-subcommands
   "Get all the possible sub commands from the sub-options-map"
   [opt-map]
   (filter identity
           (map #(when (not (nil? (first (% opt-map)))) %)
                (keys opt-map))))

(defn get-subcommands-text
  "For Help text. Returns all actions with their descriptions"
  [cli ]
  (let [opt-map (:subcommands cli )]
    (map #(string/join " - " [(name %) (last (% opt-map))]) (get-subcommands opt-map))))

 (defn find-actions
   "Find the actions in the parsed options."
   [cli ]
   (select-keys (:parsed-sub-commands cli ) (get-actions (:subcommands cli ))))

(defn assoc-actions
  "Set the actions in the cli."
  [cli]
  (assoc cli :actions (find-actions cli)))

 (defn get-subopts
  "Get the appropriate sub command options return map of all sub command options."
  [cli args sub-options]
  (let [subcommand (keyword (first args))
        sub-options-map (if sub-options
                          (first (subcommand sub-options))
                          nil)
        {:keys [options arguments summary errors]}
        (if sub-options-map (parse-opts (rest args) sub-options-map :in-order true)
            {:options true :arguments (rest args) :summary nil :errors nil})]
    (cond
     errors (exit 1 (error-msg errors))
     (:help options) (exit 0 ((:sub-command-usage cli ) cli (name subcommand) summary ))
     true (let [options-map {subcommand options}]
            (if (not (empty? arguments))
                (merge options-map (get-subopts cli arguments sub-options))
                options-map)))))

(defn parse-globals
  "Parse the global options from the command line and set them in the record.
   Return the rest of the arguments and the cli."
  [cli argv]
  (let [{:keys [options arguments summary errors]}
        (parse-opts argv (:global-options cli ) :in-order true)]
    (cond
      (:help options) (exit 0 ((:usage cli ) summary))
      (:version options) (exit 0 (:version cli ))
      errors (exit 1 (error-msg errors))
      :else {:arguments arguments
             :cli (assoc cli :parsed-global-options options)})))

(defn parse-subcommands
  "Parse out the subcommands and set the action list"
  [cli argv]
  (let [subcommands (:subcommands cli )
        sub-options (get-subopts cli argv subcommands)]
        (-> cli
            (assoc :parsed-sub-commands sub-options)
            (assoc-actions))))

(defn parse-all [cli argv]
  (let [{:keys [arguments cli]} (parse-globals cli argv)]
    (parse-subcommands cli arguments)))
