package worker;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import worker.AstMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import exception.*;

public class ExprAnalyser extends PreorderJmmVisitor<Boolean, Map.Entry<String, String>> {
    private final Table table;
    private final List<Report> reports;
    private String scope;
    private AstMethod current;

    public ExprAnalyser(Table table, List<Report> reports) {
        this.table = table;
        this.reports = reports;

        addVisit("BinOp", this::binOp);
        addVisit("RelationalExpression", this::relExpr);
        addVisit("AndExpression", this::andExpr);
        addVisit("NotExpression", this::notExpr);
        addVisit("IfCondition", this::cond);
        addVisit("WhileCondition", this::cond);
        addVisit("IntegerLiteral", this::primitive);
        addVisit("BooleanLiteral", this::primitive);
        addVisit("Array", this::arrInit);
        addVisit("ArrayAccess", this::arrAccess);
        addVisit("Variable", this::variable);
        addVisit("Assignment", this::assign);
        addVisit("ClassDeclaration", this::classDecl);
        addVisit("MainMethod", this::mainMeth);
        addVisit("ClassMethod", this::classMeth);
        addVisit("AccessExpression", this::accExpr);
        addVisit("MethodCall", this::methCall);
        addVisit("Length", this::methCall);
        addVisit("NewObject", this::newObj);
        addVisit("Return", this::ret);
    }


    private Map.Entry<String, String> binOp(JmmNode node, Boolean data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftRet = visit(left, true);
        Map.Entry<String, String> rightRet = visit(right, true);
        Map.Entry<String, String> dataRet = Map.entry("int", "null");
        
        if (!leftRet.getValue().equals("true") && left.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left Member not initialized: " + left));
        }

        if (!rightRet.getValue().equals("true") && right.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right Member not initialized: " + right));
        }
        
        if (!leftRet.getKey().equals("int") && !left.getKind().equals("access")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left Member not an integer: " + left));
        }

        if (!rightRet.getKey().equals("int") && !right.getKind().equals("access")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right Member not an integer: " + right));
        }

        if (dataRet.getKey().equals("int")) return dataRet;
        else return Map.entry("error", "null");        
    }


    private Map.Entry<String, String> relExpr(JmmNode node, Boolean data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftRet = visit(left, true);
        Map.Entry<String, String> rightRet = visit(right, true);
        Map.Entry<String, String> dataRet = Map.entry("boolean", "null");

        if (!leftRet.getValue().equals("true") && left.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left Member not initialized: " + left));
        }
        
        if (!rightRet.getValue().equals("true") && right.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right Member not initialized: " + right));
        }

        if (!leftRet.getKey().equals("int")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left Member not an integer: " + left));
        }

        if (!rightRet.getKey().equals("int")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right Member not an integer: " + right));
        }

        if (dataRet.getKey().equals("boolean")) return dataRet;
        else return Map.entry("error", "null");        
    }


    private Map.Entry<String, String> andExpr(JmmNode node, Boolean data) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Map.Entry<String, String> leftRet = visit(left, true);
        Map.Entry<String, String> rightRet = visit(right, true);
        Map.Entry<String, String> dataRet = Map.entry("boolean", "null");

        if (!leftRet.getValue().equals("true") && left.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left Member not initialized: " + left));
        }

        if (!rightRet.getValue().equals("true") && right.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right Member not initialized: " + right));
        }

        if (!leftRet.getKey().equals("boolean")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Left Member not a boolean: " + left));
        }

        if (!rightRet.getKey().equals("boolean")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Right Member not a boolean: " + right));
        }

        if (dataRet.getKey().equals("boolean")) return dataRet;
        else return Map.entry("error", "null");        
    }


    private Map.Entry<String, String> notExpr(JmmNode node, Boolean data) {
        JmmNode child = node.getChildren().get(0);

        Map.Entry<String, String> childRet = visit(child, true);
        Map.Entry<String, String> dataRet = Map.entry("boolean", "null");

        if (!childRet.getValue().equals("true") && child.getKind().equals("Variable")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Member not a initialized: " + child));
        }

        if (!childRet.getKey().equals("boolean")) {
            dataRet = Map.entry("error", "null");
            if (data != null)
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Operator '!' cannot be applied to " + childRet.getKey().replace(" ", "")));
        }

        if (dataRet.getKey().equals("boolean")) return dataRet;
        else return Map.entry("error", "null");
    }


    private Map.Entry<String, String> cond(JmmNode node, Boolean data) {
        JmmNode child = node.getChildren().get(0);
        Map.Entry<String, String> childRet = visit(child, true);
        Map.Entry<String, String> dataRet = Map.entry("boolean", "null");

        if (!childRet.getKey().equals("boolean")) {
            dataRet = Map.entry("error", "null");
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Conditional expression not boolean"));
        }

        return dataRet;
    }


    private Map.Entry<String, String> primitive(JmmNode node, Boolean data) {
        String ret;

        switch (node.getKind()) {
            case "IntegerLiteral":
                ret = "int";
                break;
            case "BooleanLiteral":
                ret = "boolean";
                break;
            default:
                ret = "error";
                break;
        }

        return Map.entry(ret, "true");
    }


    private Map.Entry<String, String> arrInit(JmmNode node, Boolean data) {
        JmmNode child = node.getChildren().get(0);
        Map.Entry<String, String> childRet = visit(child, true);
        Map.Entry<String, String> dataRet = Map.entry("int []", "null");

        if (!childRet.getKey().equals("int")) {
            dataRet = Map.entry("error", "null");
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "The size of the array init is not an Integer: " + child));
        }

        return dataRet;
    }


    private Map.Entry<String, String> arrAccess(JmmNode node, Boolean data) {
        JmmNode child = node.getChildren().get(0);
        Map.Entry<String, String> childRet = visit(child, true);

        if (!childRet.getKey().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "The index of the array access is not an Integer: " + child));
            return Map.entry("error", "null");
        }

        return Map.entry("index", childRet.getValue());
    }


    private Map.Entry<String, String> variable(JmmNode node, Boolean data) {
        Map.Entry<Symbol, Boolean> field = null;

        if (scope.equals("Class"))
            field = table.getField(node.get("name"));
        
        else if (scope.equals("Method") && current != null) {
            if (current.getName().equals("main") && node.get("name").equals("this")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Wrong use of this"));
            }
            field = current.getField(node.get("name"));
            if (field == null) 
                field = table.getField(node.get("name"));
        }

        if (field == null && table.getImports().contains(node.get("name")))
            return Map.entry("access", "true");
        else if (field == null && node.get("name").equals("this"))
            return Map.entry("method", "true");
            
        if (field == null)
            return Map.entry("error", "null");
        else
            return Map.entry(field.getKey().getType().getName() + (field.getKey().getType().isArray() ? " []" : ""), field.getValue() ? "true" : "null");
    }


    private Map.Entry<String, String> assign(JmmNode node, Boolean data) {
        List<JmmNode> children = node.getChildren();

        if (children.size() == 1) {
            Map.Entry<String, String> assignment = visit(node.getChildren().get(0), true);

            if (assignment.getKey().equals("error")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Undeclared Variable: " + node.getChildren().get(0)));
                return null;
            }

            Map.Entry<Symbol, Boolean> variable;
            if ((variable = current.getField(node.get("var"))) == null) {
                if (current.getName().equals("main"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Undeclared Variable: " + node.getChildren().get(0)));
                variable = table.getField(node.get("var"));
            }

            if (assignment.getKey().equals("access")) {
                variable.setValue(true);
                return null;
            }

            String[] parts = assignment.getKey().split(" ");
            String ftr = variable.getKey().getType().getName();

            if (ftr.equals(parts[0]) || ftr.equals(table.getSuper()) && parts[0].equals(table.getClassName()) || table.getImports().contains(ftr) && table.getImports().contains(parts[0])) {
                if (!(parts.length == 2 && variable.getKey().getType().isArray()) && !(parts.length == 1 && !variable.getKey().getType().isArray())) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Mismatched types: " + variable.getKey().getType() + " and " + assignment.getKey()));
                    return null;
                }
            }
            else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Mismatched types: " + variable.getKey().getType() + " and " + assignment.getKey()));
                return null;
            }

            if (!current.initField(variable.getKey()))
                table.initializeField(variable.getKey());
        
        }
        else {
            Map.Entry<Symbol, Boolean> array;

            if ((array = current.getField(node.get("var"))) == null)
                array = table.getField(node.get("var"));

            Map.Entry<Symbol, Boolean> variable;
            JmmNode child = children.get(1);
            if (child.getKind().equals("Variable")) {
                if ((variable = current.getField(child.get("name"))) == null)
                    variable = table.getField(child.get("name")); 
            }
            else {
                if ((variable = current.getField(node.get("var"))) == null)
                    variable = table.getField(node.get("var"));
            }

            if (!array.getKey().getType().equals(variable.getKey().getType()))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Mismatched types: " + variable.getKey().getType() + " and " + array.getKey()));
            else {
                if (!current.initField(variable.getKey()))
                    table.initializeField(variable.getKey());
            }
        }

        return null;
    }


    private Map.Entry<String, String> classDecl(JmmNode node, Boolean data) {
        scope = "Class";
        return null;
    }


    private Map.Entry<String, String> mainMeth(JmmNode node, Boolean data) {
        scope = "Method";

        try {
            current = table.getMethod("main", Arrays.asList(new Type("String", true)), new Type("void", false));
        } catch (Exception e) {
            current = null;
            e.printStackTrace();
        }

        return null;
    }


    private Map.Entry<String, String> classMeth(JmmNode node, Boolean data) {
        scope = "Method";

        List<Type> params = AstMethod.parseParameters(node.get("params"));

        try {
            current = table.getMethod(node.get("name"), params, Table.getType(node, "return"));
        } catch (Exception e) {
            current = null;
            e.printStackTrace();
        }

        return null;
    }


    private Map.Entry<String, String> accExpr(JmmNode node, Boolean requested) {
        JmmNode target = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);

        Map.Entry<String, String> targetRet = visit(target, true);
        Map.Entry<String, String> methodRet = visit(method, true);

        if (targetRet.getKey().equals("error")) {
            if (requested != null || !node.getJmmParent().getKind().equals("Assignment")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "No such method"));
            }
            
            return Map.entry("error", "null");
        }

        if (methodRet.getKey().equals("error")) {
            if (targetRet.getKey().equals("access"))
                return Map.entry("access", "null");
            
            else {
                if (requested != null || !node.getJmmParent().getKind().equals("Assignment")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "No such method"));
                }
            
                return Map.entry("error", "null");
            }
        }
        else if (targetRet.getKey().equals("method") || targetRet.getKey().equals(table.getClassName())) {
            return Map.entry(methodRet.getValue(), "null");
        }
        else if (targetRet.getKey().equals("access")) {
            return Map.entry("access", "null");
        }
        else if (targetRet.getKey().equals("int") || targetRet.getKey().equals("boolean")) {
            if (requested != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Target cannot be primitive"));
            return Map.entry("error", "null");
        }
        else if (targetRet.getKey().equals("int []")) {
            if (methodRet.getKey().equals("length"))
                return Map.entry("int", "length");
            if (methodRet.getKey().equals("index"))
                return Map.entry("int", "index");
        }
        else if (target.get("name").equals(table.getClassName())) {
            return Map.entry(methodRet.getValue(), "index");
        }
        else if (table.getImports().contains(target.get("name"))) {
            return Map.entry("access", "null");
        }
        else if(table.getImports().contains(targetRet.getKey())) {
            return Map.entry("access", "null");
        }

        return Map.entry("error", "null");
    }


    private Map.Entry<String, String> methCall(JmmNode node, Boolean data) {
        if (node.getKind().equals("Length"))
            return Map.entry("length", "null");

        List<JmmNode> children = node.getChildren();
        List<Type> params = getParameterTypes(children);

        String method = node.get("value");
        if (params.size() > 0) {
            for (Type param : params)
                method += "::" + param.getName() + ":" + (param.isArray() ? "true" : "false");
        }

        Type retType = table.getReturnType(method);

        try {
            table.getMethod(node.get("value"), params, retType);
        } catch (Exception e) {
            JmmNode parent = node.getJmmParent();
            if (parent.getChildren().size() > 0) {
                JmmNode var = parent.getChildren().get(0);
                if (var.getKind().equals("Variable") && current.getField(var.get("name")) != null) {
                    String imported = current.getField(var.get("name")).getKey().getType().getName();
                    if (this.table.getSuper() == null && !this.table.getImports().contains(imported))
                        return Map.entry("error", "noSuchMethod");
                    else
                        return Map.entry("method", "access");
                }
                else if (var.getKind().equals("Variable")) {
                    if (var.get("name").equals("this")) {
                        if (table.getSuper() != null)
                            return Map.entry("method", "access");
                        else {
                            for (String methodName : table.getMethods()) {
                                if (methodName.equals(node.get("value")))
                                    return Map.entry("method", "access");
                            }
                            return Map.entry("error", "noSuchMethod"); 
                        }
                    }
                    else {
                        if (this.table.getSuper() == null)
                            return Map.entry("error", "noSuchMethod");
                        else
                            return Map.entry("method", "access");
                    }
                }
                else {
                    if (this.table.getSuper() == null)
                        return Map.entry("error", "noSuchMethod");
                    else
                        return Map.entry("method", "access");
                }
            }
            else {
            if (this.table.getSuper() == null)
                return Map.entry("error", "noSuchMethod");
            else
                return Map.entry("method", "access");
            }
        }

        return Map.entry("method", retType.getName() + (retType.isArray() ? " []" : ""));
    }


    private Map.Entry<String, String> newObj(JmmNode node, Boolean data) {
        return Map.entry(node.get("value"), "object");
    }


    private Map.Entry<String, String> ret(JmmNode node, Boolean data) {
        String retType = visit(node.getChildren().get(0), true).getKey();

        if (retType.equals("access")) return null;

        String[] parts = retType.split(" ");
        if (node.getChildren().get(0).getKind().equals("AccessExpression")) {
            JmmNode child = node.getChildren().get(0).getChildren().get(0);
            if (current.getField(child.get("name")) != null) {
                String imported = current.getField(child.get("name")).getKey().getType().getName();
                if (retType.equals("error") && table.getImports().contains(imported)) return null;
            }
        }
        if (parts.length == 2 && current.getRet().isArray()) {
            if (!parts[0].equals(current.getRet().getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Wrong return type"));
            }
            return null;
        }
        else if (parts.length == 1 && !current.getRet().isArray()) {
            if (!parts[0].equals(current.getRet().getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Wrong return type"));
            }
            return null;
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Wrong return type"));
        }

        return null;
    }


    private List<Type> getParameterTypes(List<JmmNode> children) {
        List<Type> params = new ArrayList<>();
        for (JmmNode child : children) {
            switch (child.getKind()) {
                case "IntegerLiteral":
                    params.add(new Type("int", false));
                    break;
                case "BooleanLiteral":
                case "RelationalExpression":
                case "AndExpression":
                case "NotExpression":
                    params.add(new Type("boolean", false));
                    break;
                case "Variable":
                case "AccessExpression":
                case "BinOp":
                    Map.Entry<String, String> var = visit(child, true);
                    String[] type = var.getKey().split(" ");
                    params.add(new Type(type[0], type.length == 2));
                    break;
                default:
                    break;
            }
        }
        return params;
    }
}


