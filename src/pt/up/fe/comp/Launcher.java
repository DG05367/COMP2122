package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;  
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class Launcher {

    public static void main(String[] args) throws IOException {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        var eval = new VisitorEval();
        JmmNode root = parserResult.getRootNode();

        System.out.println(root.toTree());
        //System.out.println("visitor eval: " + eval.visit(root, null));

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Instantiate JmmAnalysis  

        Analyser analyser = new Analyser();  
 
        // Analysis stage  

        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult); 

        System.out.println(analysisResult.getSymbolTable());
  
        // Check if there are parsing errors  

        TestUtils.noErrors(analysisResult.getReports());  

        //Optimization stage

        Ollir ollir = new Ollir();

        OllirResult ollirResult = ollir.toOllir(analysisResult);

        System.out.println(ollirResult.getOllirCode());

        TestUtils.noErrors(ollirResult.getReports());

        // Jasmin stage

        Backend jasmin = new Backend();

        //var ollirResult = new OllirResult(input, Collections.emptyMap());

        JasminResult jasminResult = jasmin.toJasmin(ollirResult);

        System.out.println(jasminResult.getJasminCode());

        TestUtils.noErrors(jasminResult.getReports());

        /*Path path = Paths.get(ollirResult.getSymbolTable().getClassName() + "/");
        if (!Files.exists(path))
            Files.createDirectory(path);

        jasminResult.compile(path.toFile());
        jasminResult.compile(Path.of("test/fixtures/libs/compiled").toFile());*/
        jasminResult.compile();

    }

}
