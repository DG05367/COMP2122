package pt.up.fe.comp;  

import java.util.Collections;  
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;  
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;  
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;  
import pt.up.fe.comp.jmm.parser.JmmParserResult;  
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.*;
  
import worker.*;

public class Analyser implements JmmAnalysis {  

    @Override  
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {    
        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var error = new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Started semantic analysis but the are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(error));
        }

        if (parserResult.getRootNode() == null) {
            var error = new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Started semantic analysis but AST node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(error));
        }
        
        JmmNode node = parserResult.getRootNode();
        
        Table table = new Table();
        List<Report> reports = new ArrayList<>();
        
        System.out.println("Visitor - Filling Symbol Table");
        VisitorTable visitor = new VisitorTable(table, reports);
        visitor.visit(node, "");
        System.out.println("Symbol Table Filled");

        System.out.println("Visitor - Semantic Analysis");
        ExprAnalyser analyser = new ExprAnalyser(table, reports);
        analyser.visit(node, null);
        System.out.println("Semantic Analysis Done");

        return new JmmSemanticsResult(parserResult, table, reports);  
    }  

} 