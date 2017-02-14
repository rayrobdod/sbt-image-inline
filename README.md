# sbt-inline-image
[![Build Status](https://travis-ci.org/rayrobdod/sbt-image-inline.svg?branch=master)](https://travis-ci.org/rayrobdod/sbt-image-inline)

A sbt-web pipeline stage to take links to small images in html, svg and css
documents and convert those references into data uris.

## task and setting keys

The pipeline stage is `inlineImages`.

As there are two types of files to include, this task does not use
`includeFilter in inlineImages` and `excludeFilter in inlineImages` settings.
Instead, there is `documentsToInline in inlineImages` and `documentsToInline in
inlineImages` which describe the two types of inputs.


By default, only files smaller than 2 KiB and with the extensions png, jpeg, jpg
or gif are inlined. To change this, change the `imagesToInline in inlineImages`
setting. The setting is a sequence of FileFilter to MimeType pairs.

```scala
// To inline small BMP images in addition to other values
imagesToInline in inlineImages += (((includeFilter in imagesToInline).value && "*.bmp") -> "image/bmp")

// To inline all PNG images and only those PNG images
imagesToInline in inlineImages := Seq(ExistsFileFilter && "*.png" -> "image/png")

// To inline the default types of images, but of a size up up to 4 KiB
includeFilter in imagesToInline := ExistsFileFilter && new SimpleFileFilter({f => Files.size(f.toPath) < (1024 * 4)}),
```

The default includes instructions to modify css, svg and html documents.
Extending this probably includes a new implementation of
`com.rayrobdod.sbtImageInline.Transformation`
as each format is fairly different.


This stage does not filter out the images inlined, as it doesn't know what else
is using those images.

If one is certain that the inlined images do not need to be staged, one can use
the [sbt-filter](https://github.com/rgcottrell/sbt-filter) stage *after* this
stage in the pipeline, and append to sbt-filter's include filter with something like:

```scala
includeFilter in filter := (includeFilter in filter).value || (imagesToInline in inlineImages).value.foldLeft[sbt.FileFilter](sbt.NothingFilter){_ || _._1}
```

