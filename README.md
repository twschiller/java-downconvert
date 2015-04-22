An Eclipse plugin / tool for downconverting Java _source_ files to Java 1.4.

**If you are looking to run Java 1.5 and 1.6 classes on a Java 1.4 JVM, take a look at the [Retrotranslator project](http://retrotranslator.sourceforge.net/).**

This tool is undergoing active development; in particular, the UI / configuration is basically non-existent.

The plugin is currently targeted at translating source files to Java 1.4 so that they can be consumed by older program analysis tools (e.g., [ESC/Java2](http://secure.ucd.ie/products/opensource/ESCJava2/)), and supports the following features:

  * Translating "Enhanced" for-loops (foreach loops) into while loops
  * Removing generics, and inserting the appropriate casts
  * Removing annotations (see the section below for details)

## Unsupported Features ##

This project is driven the target projects used in my research. As such, features will typically only be added as I need them. Currently, the following features are not supported:

  * Removing enums `*`
  * Covariant return types `*`
  * Autoboxing `*`
  * Correcting parameter types for overrides of generic methods (e.g., `compareTo`) `*`
  * Java 1.5 concurrency classes

`*` Will likely be supported in the future

## Running the Tool ##

The Eclipse plugin adds a "Java Deconverter" menu to the Eclipse toolbar, with commands to perform the individual transformations, or to perform all the transformations at once (i.e., Convert All). In general, you must perform the transformations in the order described in the "How it Works" section.

## Inserting JML Specifications ##

The [Java Modeling Language (JML)](http://www.cs.ucf.edu/~leavens/JML/) is a formal language for writing class and method specifications.

The tool can insert JML specifications for the generics that have been removed. For example:

```
List<String> x = new ArrayList<String>();
```

becomes

```
List/*<String>*/ x = new ArrayList/*<String>*/();
//@set x.elementType = \type(String);
```

## Annotations ##

Some Java annotations, e.g., `@Override` are only used for compiler checks, while others can effect the program at compile-time. Since the tool is performing a source-to-source translation, the latter type of annotations are not supported.

## How it works ##

Eclipse's compiler is used to create typed abstract syntax tree (ASTs). The downconverter plugin then traverses the ASTs using a series of `ASTVisitor`s:

  1. Converting enhanced loops to while / for loops
  1. Inserting casts in positions that will require them when generics are removed
  1. Inserting mirror generic declarations as comments
  1. Inserting JML annotations as comments
  1. Stripping generics from method calls
  1. Stripping generics from type and method declarations

