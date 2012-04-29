# buildversion-maven-plugin

This is a maven plugin that extracts current build information from git
projects, including: the latest commit hash, timestamp, most recent tag, number
of commits since most recent tag. It also implements a "follow first parent"
flavor of `git describe` (see "About git-describe" below for details).

Similar in intent to
[buildnumber-maven-plugin](http://mojo.codehaus.org/buildnumber-maven-plugin/),
this plugin sets Maven project properties intended to be used in later phases of
the Maven lifecycle. You may use this to include build version information on
property files, manifests and generated sources.


## Usage

Simply add `buildversion-plugin` to your pom, executing the `set-properties` goal. Example:

```xml
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>com.code54.mojo</groupId>
        <artifactId>buildversion-plugin</artifactId>
        <version>1.0.1-SNAPSHOT</version>
        <executions>
          <execution>
            <goals><goal>set-properties</goal></goals>
          </execution>
        </executions>
      </plugin>
  ...
```

By default, the plugin runs on Maven's `initialize` phase. Any plugin running after that phase will see the following properties:

* `build-tag`: Closest repository tag (in commit history following "first parents"). NOTE: Only tags starting with `v` are considered, and the `v` is stripped. Example: `1.2.0-SNAPSHOT` (tag on git: `v1.2.0-SNAPSHOT`).
* `build-tag-delta`: Number of commits since the closest tag until HEAD. Example: `2`
* `build-commit`: Full hash of current commit (HEAD). Example: `c154712b8cea9da812c52f269578a458911f24cc`
* `build-commit-abbrev`: Abbreviated hash of current commit (HEAD). Example: `c154712`
* `build-version`: Full descriptive version of current build. Includes closest tag, tag delta, and abbreviated commit hash. Example: `1.2.0-SNAPSHOT-2-c154712`
* `build-tstamp`: A date and time stamp of the current commit (HEAD). The pattern is configurable. Example: `20120407001823`.


*Note:* `buildversion-plugin` is currently hosted at the `oss.sonatype.org` maven
repository, and may depend on artifacts on the Clojars repository. You'd need to
add these repos to your `settings.xml` or your project `pom.xml`. Example:

```xml
  <pluginRepositories>
    <pluginRepository>
      <id>sonatype-snapshots</id>
      <url>http://oss.sonatype.org/content/repositories/releases</url>
    </pluginRepository>
    <pluginRepository>
      <id>clojars.org</id>
      <url>http://clojars.org/repo</url>
    </pluginRepository>
  </pluginRepositories>
```


## Configuration Parameters

<table>
  <tr>
    <th>Property</th>
    <th>Default</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>tstampFormat</td>
    <td>yyyyMMddHHmmss</td>
    <td>Specify a custom format for the `build-tstamp` property. Use the pattern syntax defined by Java's <a href="http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a></td>
  </tr>
  <tr>
    <td>customProperties</td>
    <td>-</td>
    <td>Optional. A Clojure snippet of code you may specify in order to modify or create new properties. The code must evaluate to a Map. The name of the keys in the Map become Maven properties on the project. For convenience, for each `build-*` property you have a local variable with the same name already defined for you. See below for an example.</td>
  </tr>
</table>


Example:

```xml
  <plugin>
    <groupId>com.code54.mojo</groupId>
    <artifactId>buildversion-plugin</artifactId>
    <version>1.0.1-SNAPSHOT</version>
    <executions>
      <execution>
        <goals><goal>set-properties</goal></goals>
        <configuration>
          <!-- use only the day for the timestamp -->
          <tstampFormat>yyyy-MM-dd</tstampFormat>

          <!-- Define a new project property 'build-tag-lowercase', based on 'build-tag'
               Note how 'build-tag' is available to the script as a local variable. -->
          <customProperties>
            { :build-tag-lowercase (clojure.string/lower-case build-tag) }
          </customProperties>
        </configuration>
      </execution>
    </executions>
  </plugin>
```

# About git-describe

Before writing this plugin, I used to rely on a simple script which called `git
describe` to obtain a descriptive version number including most recent tag and
commits since such a tag.

Unfortunately, the logic behind `git describe` searches for the closest tag back
in history *following all parent commits on merges*. This means it may select
tags you originally put *on another branch*. So, if you are working on a
development branch and merge back a fix made on a release branch, calling `git
describe` on the development branch may shield a description that includes a tag
you placed on the release branch.

Until `git describe` accepts a `--first-parent` argument to prevent this
problem, this plugin implements its own logic, which basically relies on `git
log --first-parent` to traverse history on the current "line of development".

Reference:

 * [Another explanation](http://www.xerxesb.com/2010/git-describe-and-the-tale-of-the-wrong-commits/) of this same issue with `git describe`.
 * [GIT mailing list discussion](http://kerneltrap.org/mailarchive/git/2010/9/21/40071/thread) about `git describe`'s logic and lack of `--first-parent`.
 * Here's a [working patch to add `--first-parent` to `git describe`](https://github.com/git/git/tree/mrb/describe-first-parent)

## Future

Some ideas:

 * Find a way to inject the project version from git tags into the project's
  pom. The goal is to stop maintaining the project version inside
  `pom.xml`: just leave the pom version fixed at 0.0.0 and let the plugin infer it from
  git. We tried the obvious: calling project.setVersion(), but some Maven phases
  still "see" the version that is in the `pom.xml` file. Need to research
  further.
 * Expose more configuration options (like, the pattern to match candidate tags)
 * Add a mechanism for the plugin to generate a properties file (or, better, any
   file from a template)
 * Support for other repos? (SVN, git-svn, mercurial)

## FAQ

 * Why don't you just use `buildnumber-maven-plugin`?
 Because it does not provide information about tagging and number of commits
 since tag. See "About git-describe" above for details.

 * Why is it implemented in Clojure?
 Because I like the language and wanted to test its Java interoperability. I
 thought a Maven plugin was a good test-bed given that it's very Java-centric,
 including custom annotations, custom classloaders, and a DI container
 (Plexus) for Java objects.

## Acknowledgments

Thanks to Hugo Duncan for his work on
[clojure-maven](https://github.com/pallet/clojure-maven) and accepting my
[`defmojo`](https://github.com/pallet/clojure-maven/commit/1e41d02d32ec3430925765b2a88e2fce89d96307)
addition. He did the heavy-lifting to integrate Clojure with Maven and Plexus.

## License
Licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
Copyright 2012 Fernando Dobladez & Code54 S.A. (http://code54.com)

