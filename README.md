# STARS MTCQ Integration

A drop-in replacement for the default CMFTB logic used in [STARS](https://github.com/tudo-aqua/stars). 
Metric Temporal Conjunctive Queries (MTCQs) are interpreted over a background ontology.
This needs to be given by the user in one of the usual OWL file formats.

## Building

Execute `./gradlew build`.

## Tests

To verify the implementation, some test cases are offered.
Execute them by `./gradlew test`.

## Usage

For a complete example on using the library, refer to `src/kotlin/tools/aqua/stars/logic/mtcq/MTCQTest.kt`. 

The general steps are:

### 1. Initialize the evaluator

Create an MTCQEvaluator instance and load your ontology file.
```kotlin
val mtcqEval = MTCQEvaluator<...>()
mtcqEval.loadOntology(File("/path/to/mtcqOntology.rdf"))
``` 

### 2. Define DL-convertible classes

Specify the classes that the evaluator can interpret, e.g., Vehicle or Lane.
These must not be present in the ontology and are added on the fly.

```kotlin
val dlConvertibleClasses = setOf(Lane::class, Vehicle::class)
```

Alternatively, you can annotate the data model classes directly using `@DLConvertible`. 
In this case, you are not required to pass a set of `dlConvertibleClasses`.
Any marked class is automatically translated into OWL, and hence the data model becomes available in the ontology.

### 3. Create an MTCQ

Define the MTCQ using the `mtcq(...)` function.
Provide your DL query and the evaluator instance.

```kotling
val testMtcqPred = predicate(Vehicle::class) { ctx, _ ->
    mtcq(ctx, "Your MTCQ string goes here", mtcqEval, dlConvertibleClasses = dlConvertibleClasses)
}
```

Done! You can now use the `testMtcqPred` like any predicate in STARS.
If the MTCQ is non-Boolean, it is checked whether at least one answer is returned to evaluate the predicate.