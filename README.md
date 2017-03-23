# Convert PerfProStudio files to ERG (Wahoo Kickr) files.

[![Travis CI Build Status][travis-badge]][travis-link]

  [travis-link]: https://travis-ci.org/plandes/clj-perferg
  [travis-badge]: https://travis-ci.org/plandes/clj-perferg.svg?branch=master

Convert PerfProStudio files to ERG mode files, which is used for trainers like
the WahooKickr.  I use [iMobileIntervals](http://imobileintervals.com), which
takes
[ERG](http://support.trainerroad.com/hc/en-us/articles/201869764-Erg-Mode-Explained) file
forms.  However, my coach exports files
as [PerfPro (.ppsmrx)](http://perfprostudio.com).  This utility converts to an
ERG file and also optionally provides and Excel summary file.


## Obtaining

The latest release binaries are
available [here](https://github.com/plandes/clj-perferg/releases/latest).

### Source

In your `project.clj` file, add:

[![Clojars Project](https://clojars.org/com.zensols.tools/erg/latest-version.svg)](https://clojars.org/com.zensols.tools/erg/)


## Usage

The app needs a `-p` for the `.ppsmrx` PerfPro file and
your [FTP](https://wattbike.com/us/functional-threshold-power).  You can
optionally provide `-e` and use `-h` to get a usage.

```bash
$ pp2erg -p Bike\ Climb\ 3\ \(4x4\)\ +\ \(4x1\)\ min.ppsmrx -f 244 -e
perferg: perfpro: processing Bike Climb 3 (4x4) + (4x1) min.ppsmrx with ftp 244
perferg: perfpro: wrote: Bike Climb 3 (4x4) + (4x1) min.erg
perferg: perfpro: wrote: Bike Climb 3 (4x4) + (4x1) min.xls
```

Usage:
```sql
$ pp2erg --help
usage: pp2erg [options]
Convert a perfpro .ppsmrx file to .erg and optionally .xls
  -l, --level <log level>     INFO  Log level to set in the Log4J2 system.
  -p, --perfpro <file>              The PerfPro .ppsmrx file input file
  -e, --excel                       If provided output an Excel summary file as well
  -f, --ftp <number>                The functional threshold power
  -i, --interpolate <number>  4.5   zone to percent FTP interplation constant
```

## Todo

* Currently the tool doesn't support individual zones--that's next.
* Add option to parse FTP description matches (i.e. *"seated climb 75% FTP"*
  gets an FTP of 75%.


## Documentation

API [documentation](https://plandes.github.io/clj-perferg/codox/index.html).


## Building

To build from source, do the folling:

- Install [Leiningen](http://leiningen.org) (this is just a script)
- Install [GNU make](https://www.gnu.org/software/make/)
- Install [Git](https://git-scm.com)
- Download the source: `git clone https://github.com/clj-perferg && cd clj-perferg`
- Download the make include files:
```bash
mkdir ../clj-zenbuild && wget -O - https://api.github.com/repos/plandes/clj-zenbuild/tarball | tar zxfv - -C ../clj-zenbuild --strip-components 1
```
- Build the distribution binaries: `make dist`

Note that you can also build a single jar file with all the dependencies with: `make uber`


## Changelog

An extensive changelog is available [here](CHANGELOG.md).


## License

Copyright © 2017 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
