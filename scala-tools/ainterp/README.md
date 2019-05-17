# Abstract Interpreter

## Building

  1. Install everything, see build-static.sh in the main soucis
  directory:

```bash
sh build-static.sh
```

  2. Then run make inside soucis/scala-tools/ainterp:

```bash
make
```

  This installs a local package containing the interpreter for use by
  other tools build using sbt. See
  ```scala-tools/walahello/build.sbt``` on example of how to include
  it in a sbt project.

## Running

  1. You can use the script ```ainterp``` to start the interpreter:

```bash
./ainterp -D../tests -S"Test.foo.*" "Test.foo(II)I"
```

  Check ```./ainterp --help``` for information on its arguments. Some
  small test cases are in ```scala-tools/tests``` .
