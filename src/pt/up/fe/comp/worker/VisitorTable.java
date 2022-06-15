package worker;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.utilities.StringLines;
import java.util.*;
import java.util.stream.Collectors;

public class VisitorTable extends PreorderJmmVisitor<String, String> {
    private final Table table;
    private String scope;
    private final List<Report> reports;

    public VisitorTable(Table table, List<Report> reports) {
        super();
        this.table = table;
        this.reports = reports;

        addVisit("ImportDeclaration", this::importDecl);
        addVisit("ExtraImport", this::extra);
        addVisit("ClassDeclaration", this::classDecl);
        addVisit("VarDeclaration", this::varDecl);
        addVisit("Param", this::param);
        addVisit("ClassMethod", this::classMet);
        addVisit("MainMethod", this::mainMet);

        setDefaultVisit(this::defaultVisit);
    }

    private String importDecl(JmmNode node, String space) {
        table.addImport(node.get("value"));
        return space + "Import";
    }

    private String extra(JmmNode node, String space) {
        List<String> imports = table.getImports();
        String last = imports.get(imports.size() - 1);
        String next = last + "." + node.get("value");
        imports.set(imports.size() - 1, next);

        return space + "Extra_Import";
    }

    private String classDecl(JmmNode node, String space) {
        table.setClassName(node.get("name"));
        try {
            table.setSuperClassName(node.get("extends"));
        } catch(NullPointerException ignored) { }

        scope = "Class";
        return space + "Class";
    }

    private String varDecl(JmmNode node, String space) {
       Symbol field = new Symbol(Table.getType(node, "type"), node.get("id"));
       
       if (scope.equals("Class")) {
           if (table.fieldExists(field.getName())) {
               this.reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Variable already declared: " + field.getName()));

               return space + "Error";
           }

           table.addField(field);
       }
       else {
            if (table.getCurrentMethod().fieldExists(field.getName())) {
                this.reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Variable already declared: " + field.getName()));

                return space + "Error";
            }

            table.getCurrentMethod().addLocalVars(field);
       }

       return space + "Variable";
    }

    private String param(JmmNode node, String space) {
        if (scope.equals("Method")) {
            Symbol field = new Symbol(Table.getType(node, "type"), node.get("value"));
            table.getCurrentMethod().addParam(field);

            String type = field.getType().getName() + ((field.getType().isArray()) ? "[]" : "");
            node.getJmmParent().put("params", node.getJmmParent().get("params") + type + ",");
        }
        else if (scope.equals("Main")) {
            Symbol field = new Symbol(new Type("String", true), node.get("value"));
            table.getCurrentMethod().addParam(field);

            String type = field.getType().getName() + ((field.getType().isArray()) ? "[]" : "");
            node.getJmmParent().put("params", node.getJmmParent().get("params") + type + ",");
        }

        return space + "Parameter";
    }

    private String classMet(JmmNode node, String space) {
        scope = "Method";
        table.addMethod(node.get("name"), Table.getType(node, "return"));

        node.put("params", "");

        return node.toString();
    }

    private String mainMet(JmmNode node, String space) {
        scope = "Main";
        table.addMethod("main", new Type("void", false));
        node.put("params", "");

        return node.toString();
    }

    private String defaultVisit(JmmNode node, String space) {
        String content = space + node.getKind();
        String attrs = node.getAttributes().stream().map(a -> a + "=" + node.get(a)).collect(Collectors.joining(", ", "[", "]"));

        content += ((attrs.length() > 2) ? attrs : "");
        return content;
    }

    private static String reduce(String result, List<String> childResult) {
        var content = new StringBuilder();

        content.append(result).append("\n");

        for (var childRes : childResult) {
            var childContent = StringLines.getLines(childRes).stream().map(line -> " " + line + "\n").collect(Collectors.joining());

            content.append(childContent);
        }

        return content.toString();
    }
    
}
