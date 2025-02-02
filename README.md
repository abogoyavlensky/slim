# slim

The slim way to build Clojure.

## Overview

`slim` is an intentionally minimal build tool for Clojure projects that emphasizes simplicity and minimal configuration. It helps you build uberjars with zero ceremony and minimal setup. It's quite opinionated and does not aim to be a general-purpose build tool. 

## Features

- 🎯 **Minimal Configuration**: Uses standard `deps.edn` - no additional configuration files needed
- 📦 **Zero Dependencies**: Built on top of official `tools.build`
- 🔧 **Sensible Defaults**: Works out of the box for most Clojure projects

## Quick Start: Build App

Add slim to your `deps.edn`:

```clojure
{:aliases
 {:slim {:deps {io.github.abogoyavlensky/slim {:git/tag "v0.1.0" :git/sha "..."}}
         :ns-default slim.app
         :exec-args {:main-ns my-app.core}}}}
```

Run the build:
    
```shell
clojure -T:slim build
```

That's it! Your uberjar will be created at `target/standalone.jar`.

## Usage

### Build: App
Builds uberjar file for an application.
The minimal configuration requires only the main namespace:

```clojure
{...
 :ns-default slim.app
 :exec-args {:main-ns my-app.core}}
 ```

#### Custom configuration
You can customize the build also with optional parameters. All available options are shown below:

```clojure
{...
 :ns-default slim.app
 :exec-args {:main-ns my-app.core
             :target-dir "custom-target"
             :uber-file "my-app.jar"
             :src-dirs ["src" "resources" "custom-src"]
             :class-dir "custom-classes"}}
```

- `:main-ns` (**required**) - Main namespace to compile
- `:target-dir` (optional) - Target directory for build artifacts (default: "target")
- `:uber-file` (optional) - Name of the output uberjar (default: "target/standalone.jar")
- `:src-dirs` (optional) - Source directories to include (default: ["src" "resources"])
- `:class-dir` (optional) - class directory (default: "target/classes")

### Build: Library

*TODO: Add instructions*

## Inspired by

- [build-clj](https://github.com/seancorfield/build-clj/)
- [application build guide](https://clojure.org/guides/tools_build#_compiled_uberjar_application_build)

## Alternatives

- https://github.com/liquidz/build.edn
- https://github.com/NoahTheDuke/clein

## License
MIT License
Copyright (c) 2025 Andrey Bogoyavlenskiy
