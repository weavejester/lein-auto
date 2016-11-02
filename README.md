# Lein-Auto

A [Leiningen][] plugin that watches the project source directories, and
executes a task when it detects changes to files matching a set pattern.

[Leiningen]: https://github.com/technomancy/leiningen

## Installation

Add `lein-auto` as a plugin dependency to your project or profiles.

```clojure
:plugins [[lein-auto "0.1.2"]]
```

## Usage

Add `auto` to the beginning of any command you want to be executed
when file changes are detected. For example:

```
lein auto test
```

This will run `lein test` every time it detects a change to a file.
You can stop it running with Ctrl-C.

By default only `.clj`, `.cljs`, `cljx` and `.cljc` files are watched. You can
change this by adding some extra configuration to your project file:

```clojure
:auto {:default {:file-pattern #"\.(clj|cljs|cljx|cljc|edn)$"}
```

The `:default` key will apply this option to all tasks, but you can
also apply options to a specific task:

```clojure
:auto {"test" {:file-pattern #"\.(clj|cljs|cljx|cljc|edn)$"}
```

There are currently four options available:

- `:paths` -
  list of directories scanned for files. (defaults to concatenation
  of project `:source-paths`, `:java-source-paths` and `:test-paths`).

- `:file-pattern` -
  a regular expression that determine which files to watch (defaults
  to `#"\.(clj|cljs|cljx|cljc)$"`).

- `:wait-time` -
  the time to wait in milliseconds between polling the filesystem
  (defaults to 50)

- `:log-color` -
  the color of the Lein-Auto log messages (defaults to `:magenta`).
  The following colors are allowed: black gray white red green yellow
  blue magenta cyan bright-red bright-green bright-yellow bright-blue
  bright-magenta bright-cyan bright-white.

## License

Copyright © 2015 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
