# slim

[![Clojars Project](https://img.shields.io/clojars/v/io.github.abogoyavlensky/slim.svg)](https://clojars.org/io.github.abogoyavlensky/slim)
[![cljdoc badge](https://cljdoc.org/badge/io.github.abogoyavlensky/slim)](https://cljdoc.org/jump/release/io.github.abogoyavlensky/slim)
[![CI](https://github.com/abogoyavlensky/slim/actions/workflows/snapshot.yaml/badge.svg?branch=master)](https://github.com/abogoyavlensky/slim/actions/workflows/snapshot.yaml)

The slim way to build Clojure.

## Overview

`slim` is a build tool for Clojure projects that emphasizes simplicity and minimal configuration. It helps you build uberjars for applications or build and deploy jars for libraries with zero ceremony.

## Features

- 🔄 **Versatile Building**: Can build applications (uberjar), and build and deploy libraries
- 🎯 **Minimal Configuration**: Uses `deps.edn` - no additional configuration files needed
- 🔧 **Sensible Defaults**: Works out of the box with minimal configs for most Clojure projects
- 📦 **Minimal Dependencies**: Built on top of `tools.build` and `slipset/deps-deploy`
- 📚 **Documentation**: Build up-to-date [cljdoc](https://cljdoc.org/) page for a specific SNAPSHOT version


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

## Motivation

Using bare `tools.build` and `slipset/deps-deploy` isn't particularly difficult - you only need a few lines in `build.clj` and an alias in `deps.edn`. However, **you have to write this build script** for each project repeatedly. In practice, these scripts are quite similar when building jars, yet I found myself writing them over and over.

There are other existing build tools for Clojure, such as build.edn and Clein (links below). These tools are excellent, offering flexibility and rich features. However, this flexibility comes with the cost of extensive configuration: either through a separate file (build.edn), a separate alias (Clein), or verbose configuration (both). For most projects, this level of complexity isn't necessary, as they don't require such extensive flexibility. Additionally, both tools primarily focus on building and deploying libraries, whereas I wanted a versatile tool capable of handling both applications and libraries.

The main motivation behind Slim, and the answer to "Why one more build tool?" is to make the build process for Clojure projects as effortless and minimal as possible. Most projects can be configured with just one parameter for applications and two parameters for libraries. I aimed to streamline the building/deploying process to be as smooth as possible. At its core, Slim requires just one alias and a few required options to manage essential tasks. I designed Slim to be **a very thin wrapper** around `tools.build`, adding helpful utilities in a more user-friendly way, with sensible defaults and assumptions. At the same time, it maintains the flexibility to configure virtually any aspect of the build process.

## Usage

### Build: App
Builds an uberjar file for an application.
The minimal configuration requires only the main namespace:

```clojure
{:aliases
 {:slim {:deps {io.github.abogoyavlensky/slim {:mvn/version "LATEST"}}
         :ns-default slim.app
         :exec-args {:main-ns my-app.core}}}}
 ```

#### Available commands
| Command  | Description                                                                                                                                              |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `build`  | Builds an uberjar file with all dependencies included. The uberjar file will be created at the specified location (defaults to `target/standalone.jar`). |

#### Custom configuration
You can customize the build with optional parameters. All available options are shown below:

```clojure
{...
 :exec-args {:main-ns my-app.core
             :target-dir "custom-target"
             :uber-file "my-app.jar"
             :src-dirs ["src" "resources" "custom-src"]
             :class-dir "custom-classes"}}
```

- `:main-ns` (**required**) - Main namespace to compile.
- `:target-dir` (optional) - Target directory for build artifacts (default: "target").
- `:uber-file` (optional) - Name of the output uberjar (default: "target/standalone.jar").
- `:src-dirs` (optional) - Source directories to include (default: ["src" "resources"]).
- `:class-dir` (optional) - class directory (default: "target/classes").

### Build: Library
Builds and deploys a jar file for a library.
The minimal configuration requires the library name and version. It also requires the `slipset/deps-deploy` dependency to deploy the library to Clojars:

```clojure
{:aliases
 {:slim {:deps {io.github.abogoyavlensky/slim {:mvn/version "LATEST"}
                slipset/deps-deploy {:mvn/version "0.2.2"}}
         :ns-default slim.lib
         :exec-args {:lib my-org/my-lib
                     :version "0.1.0"}}}}
```

Install locally:

```shell
clojure -T:slim install
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

> [!IMPORTANT]
> You need to have `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables set to your Clojars credentials.

#### Available commands

| Command   | Description                                                                                                       | Options                                                                                                      |
|-----------|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `build`   | Builds a jar file for the library.                                                                                | `:snapshot` (optional) - If `true`, the jar will be built as a snapshot version. Default is `false`.         |
| `install` | Builds and installs the jar to local Maven repository.                                                            | `:snapshot` (optional) - If `true`, the jar will be installed as a snapshot version. Default is `false`.     |
| `deploy`  | Builds and deploys the jar to Clojars (requires `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment variables). | `:snapshot` (optional) - If `true`, the jar will be deployed as a snapshot version. Default is `false`.      |
| `tag`     | Creates a git tag for the library version.                                                                        | `:push` - If `true` automatically pushes the newly created git tag to remote repository. Default is `false`. |

#### Extended configuration
You can customize the build with optional parameters for extended metadata information in the library's pom-file. 

```clojure
{...
 :exec-args {:lib my-org/my-lib
             :version "0.1.0"
             :url "https://github.com/my-org/my-lib"
             :description "My awesome library"
             :developer "Your Name"}}
```

- `:lib` (**required**) - Library name in org/lib format.
- `:version` (**required**) - Library version. The version will be used as a git tag as-is. If you want to customize it, please use the `:scm` option.
- `:url` (optional) - Project URL. It will also be used as the SCM URL if option `:scm-url` is not set.
- `:scm-url` (optional) - Repository URL. If not set, defaults to `:url`.
- `:description` (optional) - Project description.
- `:developer` (optional) - Developer name.
- `:license` (optional) - If not set, defaults to: `{:name "MIT License" :url "https://opensource.org/license/mit"}`.

#### Custom configuration

The options `:url`, `:description`, `:developer`, and `:license` are used to generate the pom-file for the library.
If you need to customize the pom-file, you can pass the `:pom-data` option, which will take precedence over other options.
An example of `:pom-data`:
```clojure
{...
 :exec-args {...
             :pom-data [[:description "My awesome library"]
                        [:url "https://github.com/username/lib"]
                        [:licenses
                         [:license
                          [:name "MIT License"]
                          [:url "https://opensource.org/license/mit/"]]]
                        [:developers
                         [:developer
                          [:name "Person Name"]]]]}}
```

By default, `:scm` is generated using `:url` (or `:scm-url`) and `:version` (as tag).
If `:snapshot` is `true`, the tag will be presented as latest git commit hash. This is useful for [Cljdoc](https://cljdoc.org/), which requires a unique version for each build, 
and it allows you to preview doc for each snapshot.
If you need to customize your SCM repository data, you can pass the `:scm` option with a value:

```clojure
{...
 :exec-args {...
             :scm {:url "https://github.com/username/lib"
                   :connection "scm:git:git://github.com/username/lib.git"
                   :developerConnection "scm:git:ssh://git@github.com/username/lib.git"
                   :tag "v0.1.0"}}}
```

> [!TIP]
> For other options, please consult the [spec](https://github.com/abogoyavlensky/slim/blob/2a11f2b44ee1e0d66f4175078878296608f0f800/src/slim/lib.clj#L11-L45) of the library and the definition of [clojure.tools.build.api/write-pom](https://github.com/clojure/tools.build/blob/0e68670279b4fac73ff0fc4943059b1ef03c110d/src/main/clojure/clojure/tools/build/api.clj#L369-L421) function.

## Examples

- [Slim](https://github.com/abogoyavlensky/slim/blob/cf80181d0054738e1fd74defb5a67516fda2d77c/deps.edn#L18-L25)
- [Automigrate](https://github.com/abogoyavlensky/automigrate/blob/b9d0034effa0803ac2b3b47bf8c4ed119d2358ac/deps.edn#L36-L43)

## Inspired by

- [build-clj](https://github.com/seancorfield/build-clj/)
- [application build guide](https://clojure.org/guides/tools_build#_compiled_uberjar_application_build)

## Alternatives

- https://github.com/liquidz/build.edn
- https://github.com/NoahTheDuke/clein

## License
MIT License
Copyright (c) 2025 Andrey Bogoyavlenskiy
