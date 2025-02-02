# slim

[![Clojars Project](https://img.shields.io/clojars/v/io.github.abogoyavlensky/slim.svg)](https://clojars.org/io.github.abogoyavlensky/slim)

The slim way to build Clojure.

## Overview

`slim` is a minimal build tool for Clojure projects that emphasizes simplicity and minimal configuration. It can help you build uberjar for an application, or build and deploy jar for a library with zero ceremony. It's quite opinionated and does not aim to be a general-purpose build tool. 

## Features

- 🎯 **Minimal Configuration**: Uses `deps.edn` - no additional configuration files needed
- 📦 **Minimal Dependencies**: Built on top of `tools.build` and `slipset/deps-deploy`
- 🔧 **Sensible Defaults**: Works out of the box with minimal configs for most Clojure projects

## Quick Start: Build App

Add slim to your `deps.edn`:

```clojure
{:aliases
 {:slim {:deps {io.github.abogoyavlensky/slim {:mvn/version "LATEST"}}
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
{:deps {io.github.abogoyavlensky/slim {:mvn/version "LATEST"}}
 :ns-default slim.app
 :exec-args {:main-ns my-app.core}}
 ```

#### Custom configuration
You can customize the build also with optional parameters. All available options are shown below:

```clojure
{...
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

#### Available commands

- `build` - Builds an uberjar file with all dependencies included. The jar file will be created at the specified location (defaults to `target/standalone.jar`).


### Build: Library
Builds and deploys jar file for a library.
The minimal configuration requires the library name and version:

```clojure
{:deps {io.github.abogoyavlensky/slim {:mvn/version "LATEST"}
        slipset/deps-deploy {:mvn/version "0.2.2"}}
 :ns-default slim.lib
 :exec-args {:lib my-org/my-lib
             :version "0.1.0"}}
```

Run the build and deploy snapshot version:
    
```shell
clojure -T:slim deploy :snapshot true
```

or deploy release version:

```shell
clojure -T:slim deploy
```

That's it! Your library has been built and deployed to Clojars. 

*Note: You need to have `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables set to your Clojars credentials.*

#### Extended configuration
You can customize the build with optional parameters for extended meta information in pom-file of the library. 

```clojure
{...
 :exec-args {:lib my-org/my-lib
             :version "0.1.0"
             :url "https://github.com/my-org/my-lib"
             :description "My awesome library"
             :developer "Your Name"}}
```

- `:lib` (**required**) - Library name in org/lib format
- `:version` (**required**) - Library version. Version will be used as git tag as-is. If you want to customize (add prefix or anything else)
- `:url` (optional) - Project URL. It will be used as an SCM url as well. If you have separate url please define `:scm` option
- `:description` (optional) - Project description
- `:developer` (optional) - Developer name

#### Available commands

- `build` - Builds a jar file for the library. `:snapshot true` can be added to the paa
  - `:snapshot` (optional) - If `true`, the jar will be deployed as a snapshot version
- `install` - Builds and installs the jar to local Maven repository
  - `:snapshot` (optional) - If `true`, the jar will be deployed as a snapshot version
  - builds the jar file automatically before installing it
- `deploy` - Builds and deploys the jar to Clojars (requires `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables)
  - `:snapshot` (optional) - If `true`, the jar will be deployed as a snapshot version
  - builds the jar file automatically before deploying it
- `create-tag` - Creates a git tag for the library version
- `push-tag` - Pushes the git tag for the library version to remote repository


#### Custom configuration

Options `:url`, `:description` and `:developer` are used to generate pom-file for the library.
If you need to customize the pom-file you can pass `:pom-data` option and it will have precedence over other options.
An example of `:pom-data`:
```clojure
[[:description "The slim way to build Clojure"]
 [:url "https://github.com/username/lib"]
 [:licenses
  [:license
   [:name "MIT License"]
   [:url "https://opensource.org/license/mit/"]]]
 [:developers
  [:developer
   [:name "Person Name"]]]]
```

By default license is "MIT License". You can customize it with option `:license` with value:

```clojure
{:name "Apache License 2.0"
 :url "https://www.apache.org/licenses/LICENSE-2.0"}}
```

By default `:scm` is generated by `:url` and `:version` (as tag). If you need to change your scm repository url
or tag you can pass `:scm` option with value:

```clojure
{:url "https://github.com/abogoyavlensky/slim"
 :connection "scm:git:git://github.com/abogoyavlensky/slim.git"
 :developerConnection "scm:git:ssh://git@github.com/abogoyavlensky/slim.git"
 :tag "v0.1.0}}}}}
```

*Note: for other options consult [spec](https://github.com/abogoyavlensky/slim/blob/da4f254ebdfc2c1b6987579cdd2f5ce3463a1950/src/slim/lib.clj#L28-L41) of the lib and definition of [clojure.tools.build.api/write-pom](https://github.com/clojure/tools.build/blob/0e68670279b4fac73ff0fc4943059b1ef03c110d/src/main/clojure/clojure/tools/build/api.clj#L369-L421 function.*

## Inspired by

- [build-clj](https://github.com/seancorfield/build-clj/)
- [application build guide](https://clojure.org/guides/tools_build#_compiled_uberjar_application_build)

## Alternatives

- https://github.com/liquidz/build.edn
- https://github.com/NoahTheDuke/clein

## License
MIT License
Copyright (c) 2025 Andrey Bogoyavlenskiy
