# org.eag.clie
This is all because I don't want to spend a lot of time on my
command line interface, but I want it to be nice. I want documentation,
I want command grouping and sub-commands, I want a configuration file and
the ability to save commandline options for easy reuse.
I also don't want to spend time doing these same mundane things every time 
I create a new applicaton.

This is an extension to Clojure tools cli, to make command line options easier.
Creating good commandline options and parsing them should be easy 
and painless. And the help should be automatic.

This is simply an extension and wrapper around [parse-opts from org.clojure/tools.cli](https://github.com/clojure/tools.cli)
Tools.cli is doing all the work, and you'll need to understand it
to some extent.

Parse-opts does a great job of parsing. But it leaves a lot of
work to be done when creating even a simple command line interface.
Things like --help, --version and help on error should be
a part of the simplist command line interface. Sub commands should
also be easy to create and use. Errors, help and version should be 
taken care of at a central point.

Loading and saving config files which can overlap with command line options 
should also be an easy thing to do. Clie lets you save a snapshot of the configuration 
file combined with the parse commandline options tree. There is a default configuration file,
with the ability to create and use other configurations. 

Clie does all of this and lets you determine the behavior.

There is a minimal set of things needed to create a CLI with this extension.

* Define some global options as needed.
* Define some sub-command options as needed.
* Define a sub-command map using the sub-command options and any action arguments.
* Create a CLI definition
 * Add the global options
 * Add the sub-command map
 * Add the config file options to fit your CLI definitions.
* Parse the arguments.



## ToDo
Help should be prettier.  Sometimes the order of things on the commandline causes it to fail.

## Usage

Lein: `[org.eag.clie "0.1.2"]`

Create some parse-opts options as usual.

    (def server-options
      ;; An option with a required argument
      [["-p" "--port PORT" "Port number"
        :default 80
        :parse-fn #(Integer/parseInt %)
        :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
       ;; A non-idempotent option
       ["-v" nil "Verbosity level"
        :id :verbosity
        :default 0
        :assoc-fn (fn [m k _] (update-in m [k] inc))]
        ;; A boolean option defaulting to nil
        ["-h" "--help"]])

    (def s3-options
      [["-b" "--bucket S3-Bucket" "Bucket to get a file from."]
       ["-a" "--access-key ACCESS-KEY" "S3 access key"]
       ["-s" "--secret-key SECRET-KEY" "S3 secret key"]
       ["-f" "--file-key FILE-KEY" "File-key to retrieve from bucket"]
       ["-h" "--help"]])

    (def file-options
      [["-f" "--filename PATH" "File path to get a file from."]
       ["-h" "--help"]])

###Create a list of subcommands as needed, 
Actions are specified here as well. They require no options definition.

    (def sub-commands-map:
      "define a map of sub commands, their options and a usage function."
      {:server  [server-options "setup a server"]
       :s3    [s3-options "get an s3 file"]
       :file  [file-options "get a local file"]
       
       ;; Actions have no options specification.
       :start [nil "Start a new server"]
       :stop  [nil "Stop an existing server"]
       :restart [nil "Restart a Server"]})

###Create the CLI, add the options and sub-commands and parse the arguments.

     (def mycli
        (-> (clie/new-cli "mytest" "1.0.0" "My test program does nothing.")
            (clie/set-sub-commands sub-commands-map)
            (clie/parse-all argv)))

### That is mostly it. Use the parsed options to do what you want.

    (:s3 (:parsed-sub-commands mycli))
    (:server (:parsed-sub-commands mycli))
    (:file   (:parsed-sub-commands mycli))
    ;; actions can be found in actions or in the parsed-sub-commands
    (:start (:actions mycli))
    ;; global options, if you have any, can be found in parsed-global-options.
    (:parsed-global-options mycli)

## --help, --version, and usage.
These options are taken care of. The default global options are --help and --version. If
there is an error, or --help or --version. The appropriate text is generated and the system exits.
Sub-command usage is displayed when --help is given after a sub-command.

## config files
There is by default a config file. You can set the name, but by default it is `current-config`.
If you add the config file parser to your main parser group you get *--snapshot*, *--replace* 
and *file* which has the file sub-options to get files locally, from github, S3, or sftp.

If you use *--replace* the new file will make a timestamped snapshot of itself _new-config-<timestamp>.edn_ 
and replace the current-config.

When replacing the current configuration with a new one, the unmerged, original configuration is saved.

Taking a snapshot will create a config file named _current-config-snapshot-<timestamp>.edn_ which is the 
configuration after merging the configuration file with the CLI options.

The config file can have anything you like to have in it. When read, the commandline options will be
merged into the configuration tree. When taking snapshots it is the merged version of the configuration 
that is saved.

## Adding or changing the global options.
The default global options are -h --help, and -V --version. More global options can be added
with 

    (clie/add-global-options mycli global-option-specs)

## Usage functions.
There are two usage functions for help and errors. ```:usage``` and ```:sub-command-usage```.
They do reasonable things but can of course be changed to do whatever you want them to.
    
## License

Copyright © 2014 Eric Gebhart

Distributed under the Eclipse Public License, the same as Clojure.


