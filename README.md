# Minerva 2 WPRDF Convertor

Get data at https://covid19map.elixir-luxembourg.org/minerva/ under Info, 'source file'
as a zip file.

Unzip the content in a newly created `data` folder. After that, replace spaces in the filenames
into underscores:

```shell
cd data/submaps
rename 's/\s/_/g' -- *.xml
```

## Download the BridgeDb mapping file

Download `Hs_Derby_Ensembl_91.bridge` (or similar) and put it in this folder.
Check the file name in the `convert.groovy`.

## Converting Minerva SMBL files

Easiest is just to use the `Makefile`:

```shell
make all.ttl
```

### Step by step

Example conversion:

```shell
groovy convert.groovy data/submaps/Apoptosis\ pathway.xml
```

The resulting Turtle can be validated with:

```shell
cat foo.ttl | rapper -i turtle -t -q - . > /dev/null
```

(Install rapper with `apt install raptor2-utils` on Debian/Ubuntu.)
