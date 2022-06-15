package pt.up.fe.comp;  

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import worker.*;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

public class Ollir implements JmmOptimization{
    @Override
    public OllirResult toOllir(JmmSemanticsResult res) {
        JmmNode node = res.getRootNode();
        
        VisitorOllir visitor = new VisitorOllir((Table) res.getSymbolTable(), res.getReports());
        System.out.println("Preorder Visitor - Generating OLLIR...");
        int size = node.getChildren().size() - 1;
        String code = (String) visitor.visit(node.getChildren().get(size), Arrays.asList("DEFAULT_VISIT")).get(0);
        System.out.println("OLLIR Generation Successful!");
        
        return new OllirResult(res, code, res.getReports());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult res) {
        return res;
    }

    @Override
    public OllirResult optimize(OllirResult res) {
        return res;
    }
}
