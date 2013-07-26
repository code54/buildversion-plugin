# Release Notes - buildversion-plugin

## v1.0.3

* [issue #6] Update conch dependency version. Hopefully breaking dependency on clojars.org repo at runtime

## v1.0.2

* [issue #4] Ignore newline from git --format's output

* Improve release scripts

## v1.0.1 

* New gitCmd option let's you specify location of 'git' command to use

* Does not depend on bash anymore

* Better error handling and logging when invoking git

* [issue #2] Use ${basedir} instead of "." as location for git repo.
  This resolves problem when calling maven from within Eclipse.


## v1.0.0 First release


