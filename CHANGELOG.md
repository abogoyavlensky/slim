# Changelog

All notable changes to this project will be documented in this file.

*The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)*

## 0.3.0 - 2025-03-09

### Added

- Add ability to read version from a file using `:version-file` parameter.
- Add support for `{{git-count-revs}}` template variable in version strings to dynamically include the number of git commits.

## 0.2.2 - 2025-02-09

### Changed

- Bump tools.build version up to 0.10.7.

## 0.2.1 - 2025-02-07

### Added

- Add separate config `:scm-url` to set the SCM URL in the library's pom-file.

### Changed

- Set scm tag as latest git commit if `:snapshot` is `true`.
- Improve docs.


## 0.2.0 - 2025-02-04

### Added

- Add ability to build, install and deploy library with minimal config.


## 0.1.0 - 2025-01-30

### Added

- Add ability to build app with minimal config.
