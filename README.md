## SUMMARY: 
This programing project was developed within the scope of the Compilers course unit and consists on the development
of a compiler for a subset of the Java programming language, Java-- (Java minus minus). The main parts of the project being the
Semantic analysis and Code generation that are described bellow.


## SEMANTIC ANALYSIS: 
This compiler stage is responsible for validating the contents of the AST. This includes verifying the type of operations, if invokedfunctions or methods exist, their expected arguments, etc.

Type Verification:
- Operations must be between elements of the same type
- Doesn't allow operations between arrays
- Array access is only allowed to arrays
- Only int values to index accessing array
- Only int values to initialize an array
- Assignment between the same type (a_int = b_boolean is not allowed)
- Boolean operation (&&, < ou !) only with booleans
- Conditional expressions only accepts variables, operations or function calls that return boolean
- Raises an error if variable has not been initialized
- Assumes parameters are initialized

Method Verification:
- Check if the method "target" exists and if it contains the corresponding method
- Check if method exists in the declared class, else: error if no extends is declared, if extends assume that the method is from the super class
- If the method is not from the declared class, assume that the method exists and assume the expected types
- Verify that the number of arguments in an invoke is the same as the number of parameters of the declaration
- Verify that the type of the parameters matches the type of the arguments

Fields:
- Checks if the variable is initialized, throwing an error if the variable is used without being initialized;
- The `initialized` flag is saved on the symbol table.


## CODE GENERATION: 
 
Starting with the Java-- code file, we developed a parser for this language, taking into account the furnished grammar.
With the code exempt of lexical and syntactic errors, we perform the generation of the syntax tree while annotating some
nodes and leafs with extra information. With this AST we can create the Symbol Table and perform a Semantic Analysis.
The next step needed is to generate OLLIR code derived from the AST and Symbol table and the final step is to generate
the set of JVM instructions to be accepted by jasmin.


## PROS: (Identify the most positive aspects of your tool)
- We were able to generate the OLLIR Code, even though we had the option not to do it
 
## CONS: (Identify the most negative aspects of your tool)
- There is no Register allocation
- There is no constatn propagation optimization



# Compilers Project

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages 

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage). 

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.SimpleParser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.
