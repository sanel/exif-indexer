# exif-indexer

Reads images from S3, extracts EXIF metadata and indexes them in local
H2 storage. Has ability to lookup for particular metadata.

## Installation 

To compile and install, make sure to have
[Leiningen](http://leiningen.org) installed. In source folder do:

```sh
$ lein uberjar
```

This will generate `target/exif-indexer.jar` executable jar file.

## Usage

To index available images use:

```sh
$ java -jar exif-indexer.jar -index
```

To lookup metadata for particular image, use:

```sh
$ java -jar exif-indexer.jar -lookup <image-name>
```

For example, to figure out EXIF metadata for
`0003b8d6-d2d8-4436-a398-eab8d696f0f9.68cccdd4-e431-457d-8812-99ab561bf867.jpg`
use:

```sh
$ java -jar exif-indexer.jar -lookup 0003b8d6-d2d8-4436-a398-eab8d696f0f9.68cccdd4-e431-457d-8812-99ab561bf867.jpg 
```

## License

Copyright © 2016 Sanel Zukan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
