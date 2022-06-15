package worker;

import exception.NoSuchMethod;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;
import worker.OllirTemplates;

import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.Soundbank;

import org.specs.comp.ollir.Ollir;

public class VisitorOllir extends PreorderJmmVisitor<List<Object>, List<Object>> {
    private final Table table;
    private AstMethod current;
    private final List<Report> reports;
    private String scope;
    private final Set<JmmNode> visited = new HashSet<>();
    private int temp_seq = 1;
    private int if_seq = 1;
    private int while_seq = 1;

    public VisitorOllir(Table table, List<Report> reports) {
        super();

        this.table = table;
        this.reports = reports;

        addVisit("ClassDeclaration", this::classToOllir);
        addVisit("MainMethod", this::mainToOllir);
        addVisit("ClassMethod", this::methodToOllir);
        addVisit("Assignment", this::assignToOllir);
        addVisit("IntegerLiteral", this::primitiveToOllir);
        addVisit("BooleanLiteral", this::primitiveToOllir);
        addVisit("BinOp", this::binToOllir);
        addVisit("Variable", this::varToOllir);
        addVisit("Return", this::retToOllir);
        addVisit("VarDeclaration", this::declarationToOllir);
        addVisit("RelationalExpression", this::binToOllir);
        addVisit("AndExpression", this::andToOllir);
        addVisit("NotExpression", this::notToOllir);
        addVisit("IfElse", this::switchToOllir);
        addVisit("IfStmt", this::ifToOllir);
        addVisit("ElseStmt", this::elseToOllir);
        addVisit("IfCondition", this::condToOllir);
        addVisit("WhileStmt", this::whileToOllir);
        addVisit("WhileCondition", this::condToOllir);
        addVisit("AccessExpression", this::accessToOllir);
        addVisit("MethodCall", this::callToOllir);
        addVisit("Length", this::callToOllir);
        addVisit("Array", this::initToOllir);
        addVisit("NewObject", this::objToOllir);
        addVisit("ArrayAccess", this::arrayToOllir);
        setDefaultVisit(this::defaultVisit);
    }

    private List<Object> classToOllir(JmmNode node, List<Object> data) {
        scope = "CLASS";

        List<String> fields = new ArrayList<>();
        List<String> body = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        List<String> imports = this.table.getImports();

        for (int i = 0; i < imports.size(); i++) {
            builder.append(String.format("import %s;\n", imports.get(i)));
        }
        builder.append("\n");

        for (JmmNode child : node.getChildren()) {
            String ollirChild = (String) visit(child, Collections.singletonList("CLASS")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT")) {
                if (child.getKind().equals("VarDeclaration")) {
                    fields.add(ollirChild);
                }
                else {
                    body.add(ollirChild);
                }
            }
        }

        builder.append(OllirTemplates.classTemplate(table.getClassName(), table.getSuper()));
        builder.append(String.join("\n", fields)).append("\n\n");
        builder.append(OllirTemplates.constructor(table.getClassName())).append("\n\n");
        builder.append(String.join("\n\n", body));
        builder.append("\n}");
        
        return Collections.singletonList(builder.toString());
    }

    private List<Object> mainToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        scope = "METHOD";

        try {
            current = table.getMethod("main", Collections.singletonList(new Type("String", true)), new Type("void", false));
        } catch (Exception e) {
            current = null;
            e.printStackTrace();
        }

        StringBuilder builder = new StringBuilder(".method public ");
        builder.append(OllirTemplates.method("main", current.parametersToOllir(), OllirTemplates.type(current.getRet()), true));
        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));
        builder.append("\nret.V;");
        builder.append("\n}");

        return Collections.singletonList(builder.toString());
    }

    private List<Object> methodToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        scope = "METHOD";

        List<Type> params = AstMethod.parseParameters(node.get("params"));

        try {
            current = table.getMethod(node.get("name"), params, Table.getType(node, "return"));
        } catch (Exception e) {
            current = null;
            e.printStackTrace();
        }

        StringBuilder builder = new StringBuilder(".method ");
        builder.append(OllirTemplates.method(current.getName(), current.parametersToOllir(), OllirTemplates.type(current.getRet())));
        List<String> body = new ArrayList <>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
            if (ollirChild != null && ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));
        builder.append("\n}");

        return Collections.singletonList(builder.toString());
    }

    private List<Object> assignToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        Map.Entry<Symbol, Boolean> var;
        boolean field = false;

        if ((var = current.getField(node.get("var"))) == null) {
            var = table.getField(node.get("var"));
            field = true;
        }

        String name = !field ? current.isParameter(var.getKey()) : null;
        String ollirVariable, ollirType;

        StringBuilder builder = new StringBuilder();

        ollirVariable = OllirTemplates.variable(var.getKey(), name);
        ollirType = OllirTemplates.type(var.getKey().getType());
        List<Object> visitRes;

        if (node.getChildren().size() > 1) {
            String target = (String) visit(node.getChildren().get(0)).get(0);
            String[] parts = target.split("\n");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    builder.append(parts[i]).append("\n");
                }
            }
            visitRes = visit(node.getChildren().get(1), Arrays.asList(field ? "FIELD" : "ASSIGNMENT", var.getKey(), "ARRAY_ACCESS"));
            String res = (String) visitRes.get(0);
            String[] parts2 = res.split("\n");
        
            if (parts2.length > 1) {
                for (int i = 0; i < parts2.length - 1; i++) {
                   builder.append(parts2[i]).append("\n"); 
                }

                if (!field) {
                    String temp = "temporary" + temp_seq++ + ".i32";
                    builder.append(String.format("%s :=.i32 %s;\n", temp, parts2[parts2.length - 1]));

                    builder.append(String.format("%s :=%s %s;\n", OllirTemplates.arrayaccess(var.getKey(), name, parts[parts.length - 1]), OllirTemplates.type(new Type(var.getKey().getType().getName(), false)), temp));
                    //builder.append(String.format("%s :=%s %s;\n", OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, parts[parts.length - 1]), OllirTemplates.type(new Type(var.getKey().getType().getName(), false)), temp));
                }
                else {
                    String temp = "temporary" + temp_seq++;
                    builder.append(String.format("%s :=%s %s;\n", temp + ollirType, ollirType, OllirTemplates.getfield(var.getKey())));
                
                    builder.append(String.format("%s :=%s %s;\n", OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, parts[parts.length - 1]), OllirTemplates.type(new Type(var.getKey().getType().getName(), false)), parts2[parts2.length - 1]));
                }
            }
            else {
                if (!field) {
                    String temp = "temporary" + temp_seq++ + ".i32";
                    builder.append(String.format("%s :=.i32 %s;\n", temp, res));

                    builder.append(String.format("%s :=%s %s;\n", OllirTemplates.arrayaccess(var.getKey(), name, parts[parts.length - 1]), OllirTemplates.type(new Type(var.getKey().getType().getName(), false)), temp));
                    //builder.append(String.format("%s :=%s %s;\n", OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, parts[parts.length - 1]), OllirTemplates.type(new Type(var.getKey().getType().getName(), false)), temp));
                }
                else {
                    String temp = "temporary" + temp_seq++;
                    builder.append(String.format("%s :=%s %s;\n", temp + ollirType, ollirType, OllirTemplates.getfield(var.getKey())));
                
                    builder.append(String.format("%s :=%s %s;\n", OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, parts[parts.length - 1]), OllirTemplates.type(new Type(var.getKey().getType().getName(), false)), res));
                }
            }
        }
        else {
            visitRes = visit(node.getChildren().get(0), Arrays.asList(field ? "FIELD" : "ASSIGNMENT", var.getKey(), "SIMPLE"));
            String res = (String) visitRes.get(0);
            String[] parts2 = res.split("\n");
            if (parts2.length > 1) {
                for (int i = 0; i < parts2.length - 1; i++) {
                   builder.append(parts2[i]).append("\n"); 
                }

                if (!field) {
                    builder.append(String.format("%s :=%s %s;", ollirVariable, ollirType, parts2[parts2.length - 1]));
                }
                else {
                    if (visitRes.size() > 1 && (visitRes.get(1).equals("ARRAY_INIT") || visitRes.get(1).equals("OBJECT_INIT"))) {
                        String temp = "temporary" + temp_seq++ + ollirType;
                        builder.append(String.format("%s :=%s %s;\n", temp, ollirType, parts2[parts2.length - 1]));
                        builder.append(OllirTemplates.putfield(ollirVariable, temp));
                    }
                    else {
                        builder.append(OllirTemplates.putfield(ollirVariable, parts2[parts2.length - 1]));
                    }
                    builder.append(";");
                }
            }
            else {
                if (!field) {
                    builder.append(String.format("%s :=%s %s;", ollirVariable, ollirType, res));
                }
                else {
                    if (visitRes.size() > 1 && (visitRes.get(1).equals("ARRAY_INIT") || visitRes.get(1).equals("OBJECT_INIT"))) {
                        String temp = "temporary" + temp_seq++ + ollirType;
                        builder.append(String.format("%s :=%s %s;\n", temp, ollirType, res));
                        builder.append(OllirTemplates.putfield(ollirVariable, temp));
                    }
                    else {
                        builder.append(OllirTemplates.putfield(ollirVariable, res));
                    }
                    builder.append(";");
                }
            }
        }

        if (visitRes.size() > 1 && visitRes.get(1).equals("OBJECT_INIT")) {
            builder.append("\n").append(OllirTemplates.objectinstance(var.getKey()));
        }

        return Collections.singletonList(builder.toString());
    }

    private List<Object> primitiveToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String value, type;

        switch (node.getKind()) {
            case "IntegerLiteral":
                value = node.get("value") + ".i32";
                type = ".i32";
                break;

            case "BooleanLiteral":
                value = (node.get("value").equals("true") ? "1" : "0") + ".bool";
                type = ".bool";
                break;

            default:
                value = "";
                type = "";
                break;
        }

        if (data.get(0).equals("RETURN")) {
            String temp = "temporary" + temp_seq++ + type;
            value = String.format("%s :=%s %s;\n%s", temp, type, value, temp);
        }
        else if (data.get(0).equals("CONDITION") && type.equals(".bool")) {
            value = String.format("%s ==.bool 1.bool\n", value);
        }

        return Collections.singletonList(value);
    }

    private List<Object> binToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);
        String leftRet = (String) visit(left, Collections.singletonList("BINARY")).get(0);
        String rightRet = (String) visit(right, Collections.singletonList("BINARY")).get(0);
        String[] leftStmt = leftRet.split("\n");
        String[] rightStmt = rightRet.split("\n");

        StringBuilder builder = new StringBuilder();
        String leftSide = binOps(leftStmt, builder, new Type("int", false));
        String rightSide = binOps(rightStmt, builder, new Type("int", false));

        leftSide = leftSide.split(":=")[0];
        rightSide = rightSide.split(":=")[0];

        if (data == null) {
            return Arrays.asList("DEFAULT_VISIT");
        }
        if (data.get(0).equals("RETURN") || data.get(0).equals("FIELD")) {
            Symbol var = new Symbol(new Type("int", false), "temporary" + temp_seq++);
            builder.append(String.format("%s :=.i32 %s %s.i32 %s;\n",OllirTemplates.variable(var), leftSide, node.get("op"), rightSide));
            builder.append(OllirTemplates.variable(var));
        }
        else {
            builder.append(String.format("%s %s.i32 %s", leftSide, node.get("op"), rightSide));
        }

        return Collections.singletonList(builder.toString());
    }

    private List<Object> varToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        Map.Entry<Symbol, Boolean> field = null;
        boolean classField = false;

        if (scope.equals("CLASS")) {
            classField = true;
            field = table.getField(node.get("name"));
        }
        else if (scope.equals("METHOD") && current != null) {
            field = current.getField(node.get("name"));
            if (field == null) {
                classField = true;
                field = table.getField(node.get("name"));
            }
        }

        StringBuilder builder = null;

        if (data.get(0).equals("ACCESS")) {
            builder = (StringBuilder) data.get(1);
        }

        if (field != null) {
            String name = current.isParameter(field.getKey());
            if (classField && !scope.equals("CLASS")) {
                StringBuilder ollir = new StringBuilder();
                Symbol var = new Symbol(field.getKey().getType(), "temporary" + temp_seq++);
                if (data.get(0).equals("CONDITION")) {
                    ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(var), OllirTemplates.type(var.getType()), OllirTemplates.getfield(field.getKey())));
                    ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(var)));

                    return Arrays.asList(ollir.toString(), var, name);
                }
                else {
                    Objects.requireNonNullElse(builder, ollir).append(String.format("%s :=%s %s;\n", OllirTemplates.variable(var), OllirTemplates.type(var.getType()), OllirTemplates.getfield(field.getKey())));
                    ollir.append(OllirTemplates.variable(var));

                    return Arrays.asList(ollir.toString(), var, name);
                }
            }
            else {
                if (data.get(0).equals("CONDITION")) {
                    return Arrays.asList(String.format("%s ==.bool 1.boll", OllirTemplates.variable(field.getKey(), name)), field.getKey(), name);
                }

                return Arrays.asList(OllirTemplates.variable(field.getKey(), name), field.getKey(), name);
            }
        }

        return Arrays.asList("ACCESS", node.get("name"));
    }

    private List<Object> retToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();

        List<Object> visit = visit(node.getChildren().get(0), Arrays.asList("RETURN"));

        String res = (String) visit.get(0);
        String[] parts = res.split("\n");
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                builder.append(parts[i]).append("\n");
            }
            builder.append(OllirTemplates.ret(current.getRet(), parts[parts.length - 1]));
        }
        else {
            builder.append(OllirTemplates.ret(current.getRet(), res));
        }

        return Collections.singletonList(builder.toString());
    }

    private List<Object> declarationToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        if ("CLASS".equals(data.get(0))) {
            Map.Entry<Symbol, Boolean> var = table.getField(node.get("id"));
            return Arrays.asList(OllirTemplates.field(var.getKey()));
        }

        return Arrays.asList("");
    }

    private List<Object> andToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);
        String leftRet = (String) visit(left, Collections.singletonList("BINARY")).get(0);
        String rightRet = (String) visit(right, Collections.singletonList("BINARY")).get(0);
        String[] leftStmt = leftRet.split("\n");
        String[] rightStmt = rightRet.split("\n");

        StringBuilder builder = new StringBuilder();

        String leftSide = binOps(leftStmt, builder, new Type("int", false));
        String rightSide = binOps(rightStmt, builder, new Type("int", false));

        if (data.get(0).equals("RETURN") || data.get(0).equals("FIELD")) {
            Symbol var = new Symbol(new Type("boolean", false), "temporary" + temp_seq++);
            if (node.get("op").equals("and"))
                builder.append(String.format("%s :=.bool %s.i32 %s;\n",OllirTemplates.variable(var), leftSide, "&&", rightSide));
            else
                builder.append(String.format("%s :=.bool %s.i32 %s;\n",OllirTemplates.variable(var), leftSide, node.get("op"), rightSide));
            builder.append(OllirTemplates.variable(var));
        }
        else {
            if (node.get("op").equals("and"))
                builder.append(String.format("%s %s.i32 %s", leftSide, "&&", rightSide));
            else 
                builder.append(String.format("%s %s.i32 %s", leftSide, node.get("op"), rightSide));
        }

        return Collections.singletonList(builder.toString());
    }

    private List<Object> notToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();
        String expr = (String) visit(node.getChildren().get(0), Collections.singletonList("NOT")).get(0);
        String[] parts = expr.split("\n");
        String last = parts[parts.length - 1];

        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++)
                builder.append(parts[i]).append("\n");
        }
        Symbol var = new Symbol(new Type("boolean", false), "temporary" + temp_seq++);
        String[] exprParts;

        if ((exprParts = last.split("<")).length == 2) {
            if (data.get(0).equals("RETURN")) {
                builder.append(String.format("%s :=.bool %s<=%s;\n", OllirTemplates.variable(var), exprParts[0], exprParts[1]));
                builder.append(String.format("%s", OllirTemplates.variable(var)));
            } 
            else {
                builder.append(String.format("%s>=%s", exprParts[0], exprParts[1]));
            }
        }
        else if ((exprParts = last.split(">=")).length == 2) {
            if (data.get(0).equals("RETURN")) {
                builder.append(String.format("%s :=.bool %s<%s;\n", OllirTemplates.variable(var), exprParts[0], exprParts[1]));
                builder.append(String.format("%s", OllirTemplates.variable(var)));
            } 
            else {
                builder.append(String.format("%s<%s", exprParts[0], exprParts[1]));
            }
        }
        else if (expr.equals("0.bool")) {
            builder.append("1.bool");
        }
        else if (expr.equals("1.bool")) {
            builder.append("0.bool");
        }
        else {
            if (data.get(0).equals("RETURN")) {
                Symbol variable = new Symbol(new Type("boolean", false), "temporary" + temp_seq++);
                builder.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(variable), last));
                builder.append(String.format("%s :=.bool %s |.bool %s;\n", OllirTemplates.variable(var), OllirTemplates.variable(variable), OllirTemplates.variable(variable)));
                builder.append(String.format("%s", OllirTemplates.variable(var)));
            }
            else {
                builder.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(var), last));
                builder.append(String.format("%s !.bool %s", OllirTemplates.variable(var), OllirTemplates.variable(var)));
            }
        }
        return Collections.singletonList(builder.toString());
    } 

    private List<Object> switchToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();
        String ifelse = (String) visit(node.getChildren().get(0), Collections.singletonList("IFELSE")).get(0);
        String[] parts = ifelse.split("\n");

        for (int i = 0; i < parts.length; i++) {
            builder.append(parts[i]).append("\n");
        }

        return Collections.singletonList(builder.toString());
    }

    private List<Object> ifToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();
        int count = if_seq++;
        String ifCond = (String) visit(node.getChildren().get(0), Collections.singletonList("CONDITION")).get(0);
        String[] ifCondParts = ifCond.split("\n");

        if (ifCondParts.length > 1) {
            for (int i = 0; i < ifCondParts.length - 1; i++)
                builder.append(ifCondParts[i]).append("\n");
        }
        if (ifCondParts[ifCondParts.length - 1].contains("==.bool 1.bool")) {
            String cond = ifCondParts[ifCondParts.length - 1].split(" ==.bool ")[0];
            builder.append(String.format("if (%s !.bool %s) goto else%d;\n", cond, cond, count));
        }
        else {
            Symbol aux = new Symbol(new Type("boolean", false), "temporary" + temp_seq++);
            builder.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(aux), ifCondParts[ifCondParts.length - 1]));
            builder.append(String.format("if (%s !.bool %s) goto else%d;\n", OllirTemplates.variable(aux), OllirTemplates.variable(aux), count));
        }

        List<String> body = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++) {
            body.add((String) visit(node.getChildren().get(i), Collections.singletonList("IF")).get(0));
        }
        builder.append(String.join("\n", body)).append("\n");
        builder.append(String.format("goto endif%d;\n", count));
        builder.append(visit(node.getJmmParent().getChildren().get(1), Arrays.asList("ELSE", count)).get(0));
        builder.append("\n");
        builder.append(String.format("endif%d:\n", count));

        return Collections.singletonList(builder.toString());
    }

    private List<Object> elseToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("else%s:\n", Integer.toString((int) data.get(1))));
        List<String> body = new ArrayList<>();

        for (int i = 0; i < node.getChildren().size(); i++) {
            body.add((String) visit(node.getChildren().get(i), Collections.singletonList("ELSE")).get(0));
        }

        builder.append(String.join("\n", body));
        return Collections.singletonList(builder.toString());
    }

    private List<Object> condToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        return visit(node.getChildren().get(0), Collections.singletonList("CONDITION"));
    }

    private List<Object> whileToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();
        int count = while_seq++;
        builder.append(String.format("loop%d:\n", count));
        String cond = (String) visit(node.getChildren().get(0), Collections.singletonList("WHILE")).get(0);
        String[] condParts = cond.split("\n");

        if (condParts.length > 1){
            for (int i = 0; i < condParts.length - 1; i++)
                builder.append(condParts[i]).append("\n");
        }

        if (condParts[condParts.length - 1].contains("==.bool 1.bool")) {
            String condAux = condParts[condParts.length - 1].split(" ==.bool ")[0];
            builder.append(String.format("if (%s !.bool %s) goto endloop%d;\n", condAux, condAux, count));
        }
        else {
            Symbol aux = new Symbol(new Type("boolean", false), "temporary" + temp_seq++);
            builder.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(aux), condParts[condParts.length - 1]));
            builder.append(String.format("if (%s !.bool %s) goto endloop%d;\n", OllirTemplates.variable(aux), OllirTemplates.variable(aux), count));
        }

        List<String> body = new ArrayList<>();
        for (int i = 1; i < node.getChildren().size(); i++)
            body.add((String) visit(node.getChildren().get(i), Arrays.asList("IF")).get(0));
          
        builder.append(String.join("\n", body)).append("\n");
        builder.append(String.format("goto loop%d;\n", count));
        builder.append(String.format("endloop%d:", count));

        return Collections.singletonList(builder.toString());
    }

    private List<Object> accessToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        JmmNode target = node.getChildren().get(0);
        JmmNode meth = node.getChildren().get(1);
        StringBuilder builder = new StringBuilder();
        List<Object> targetRet = visit(target, Arrays.asList("ACCESS", builder));
        List<Object> methRet = visit(meth, Arrays.asList("ACCESS", builder));
        Symbol assign = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;
        String expr = null;
        Type type = (data.get(0).equals("BINARY") || (data.size() > 2 && data.get(2).equals("ARRAY_ACCESS"))) ? new Type("int", false) : null;

        if (targetRet.get(0).equals("ACCESS")) {
            if (!targetRet.get(1).equals("this")) {
                String targetVar = (String) targetRet.get(1);
                if (assign != null) {
                    if (data.get(2).equals("ARRAY_ACCESS")) {
                        expr = OllirTemplates.invokestatic(targetVar, (String) methRet.get(1), new Type(assign.getType().getName(), false), (String) methRet.get(2));
                        type = new Type(assign.getType().getName(), false);
                    }
                    else {
                        expr = OllirTemplates.invokestatic(targetVar, (String) methRet.get(1), assign.getType(), (String) methRet.get(2));
                        type = assign.getType();
                    }
                }
                else {
                    type = (type == null) ? new Type("void", false) : type;
                    expr = OllirTemplates.invokestatic(targetVar, (String) methRet.get(1), type, (String) methRet.get(2));
                }
            }
            else {
                if (methRet.get(0).equals("method")) {
                    if (assign != null) {
                        expr = OllirTemplates.invokespecial((String) methRet.get(1), assign.getType(), (String) methRet.get(2));
                        type = assign.getType();
                    }
                    else {
                        type = (type == null) ? new Type("void", false) : type;
                        expr = OllirTemplates.invokespecial((String) methRet.get(1), type, (String) methRet.get(2));
                    }
                }
                else {
                    AstMethod call = (AstMethod) methRet.get(1);
                    expr = OllirTemplates.invokevirtual(call.getName(), call.getRet(), (String) methRet.get(2));
                    type = call.getRet();
                }
            }
        }
        else if (meth.getKind().equals("ArrayAccess")) {
            Symbol array = (Symbol) targetRet.get(1);
            String index = (String) methRet.get(0);
            String[] parts = index.split("\n");

            if (parts.length > 1)
                for (int i = 0; i < parts.length - 1; i++)
                    builder.append(parts[i]).append("\n");

            expr = OllirTemplates.arrayaccess(array, (String) targetRet.get(2), parts[parts.length - 1]);
            type = new Type(array.getType().getName(), false);
        }
        else {
            if (targetRet.get(1).equals("OBJECT_INIT")) {
                Type type2 = new Type((String) targetRet.get(2), false);
                Symbol aux = new Symbol(type, "temporary" + temp_seq++);
                builder.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(aux), OllirTemplates.type(type2), targetRet.get(0)));
                builder.append(OllirTemplates.objectinstance(aux)).append("\n");

                if (methRet.get(0).equals("method")) {
                    if (assign != null) {
                        expr = OllirTemplates.invokespecial(OllirTemplates.variable(aux), (String) methRet.get(1), assign.getType(), (String) methRet.get(2));
                        type = assign.getType();
                    }
                    else {
                        type = (type == null) ? new Type("void", false) : type;
                        expr = OllirTemplates.invokespecial(OllirTemplates.variable(aux), (String) methRet.get(1), type, (String) methRet.get(2));
                    }
                }
                else {
                    AstMethod call = (AstMethod) methRet.get(1);
                    expr = OllirTemplates.invokevirtual(OllirTemplates.variable(aux), call.getName(), call.getRet(), (String) methRet.get(2));
                    type = call.getRet();
                }
            }
            else {
                if (methRet.get(0).equals("method")) {
                    if (assign != null) {
                        expr = OllirTemplates.invokevirtual(OllirTemplates.variable((Symbol) targetRet.get(1)), (String) methRet.get(1), assign.getType(), (String) methRet.get(2));
                        type = assign.getType();
                    }
                    else {
                        type = (type == null) ? new Type("void", false) : type;
                        expr = OllirTemplates.invokevirtual(OllirTemplates.variable((Symbol) targetRet.get(1)), (String) methRet.get(1), type, (String) methRet.get(2));
                    }
                }
                else if (!methRet.get(0).equals("length")){
                    Symbol targetVar = (Symbol) targetRet.get(1);
                    AstMethod call = (AstMethod) methRet.get(1);
                    expr = OllirTemplates.invokevirtual(OllirTemplates.variable(targetVar), call.getName(), call.getRet(), (String) methRet.get(2));
                    type = call.getRet();
                }
                else {
                    expr = OllirTemplates.arraylength(OllirTemplates.variable((Symbol) targetRet.get(1), (String) targetRet.get(2)));
                    type = new Type("int", false);
                }
            }
        }

        if ((data.get(0).equals("CONDITION") || data.get(0).equals("BINARY") || data.get(0).equals("FIELD") || data.get(0).equals("PARAM") || data.get(0).equals("RETURN")) && type != null && expr != null) {
            Symbol aux = new Symbol(type, "temporary" + temp_seq++);
            builder.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(aux), OllirTemplates.type(type), expr));
            if (data.get(0).equals("CONDITION")) 
                builder.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(aux)));
            else if (data.get(0).equals("BINARY") || data.get(0).equals("FIELD") || data.get(0).equals("PARAM") || data.get(0).equals("RETURN"))
                builder.append(String.format("%s", OllirTemplates.variable(aux)));
        }
        else
            builder.append(expr);

        if (data.get(0).equals("METHOD") || data.get(0).equals("IF") || data.get(0).equals("ELSE") || data.get(0).equals("WHILE"))
            builder.append(";");

        return Arrays.asList(builder.toString(), type);
    }

    private List<Object> callToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        if (node.getKind().equals("Length")) return Arrays.asList("length");

        StringBuilder builder = (StringBuilder) data.get(1);
        List<JmmNode> children = node.getChildren();
        Map.Entry<List<Type>, String> params = getParams(children, builder);
        String meth = node.get("value");

        if (params.getKey().size() > 0)
            for (Type param : params.getKey())
                meth += "::" + param.getName() + ":" + (param.isArray() ? "true" : "false");

        Type type = table.getReturnType(meth);

        try {
            AstMethod method = table.getMethod(node.get("value"), params.getKey(), type);
            return Arrays.asList("class_method", method, params.getValue());
        } catch (Exception e) {
            return Arrays.asList("method", node.get("value"), params.getValue());
        }
    }

    private List<Object> initToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder builder = new StringBuilder();
        String size = (String) visit(node.getChildren().get(0), Collections.singletonList("RETURN")).get(0);
        String[] sizeParts = size.split("\n");

        if (sizeParts.length > 1) 
            for (int i = 0; i < sizeParts.length - 1; i++)
                builder.append(sizeParts[i]).append("\n");

        builder.append(OllirTemplates.arrayinit(sizeParts[sizeParts.length - 1]));
        return Arrays.asList(builder.toString(), "ARRAY_INIT");
    }

    private List<Object> objToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String ret = OllirTemplates.objectinit(node.get("value"));
        if (data.get(0).equals("METHOD")) 
            ret += ";";

        return Arrays.asList(ret, "OBJECT_INIT", node.get("value"));
    }

    private List<Object> arrayToOllir(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String visit = (String) visit(node.getChildren().get(0), Arrays.asList("RETURN")).get(0);

        return Arrays.asList(visit);
    }

    private Map.Entry<List<Type>, String> getParams(List<JmmNode> children, StringBuilder builder) {
        List<Type> params = new ArrayList<>();
        List<String> ollir = new ArrayList<>();

        for (JmmNode child : children) {
            Type type;
            String var, res;
            String[] stmts;
            switch (child.getKind()) {
                case "IntegerLiteral":
                    type = new Type("int", false);
                    ollir.add(String.format("%s%s", child.get("value"), OllirTemplates.type(type)));
                    params.add(type);
                    break;
                
                case "BooleanLiteral":
                    type = new Type("boolean", false);
                    ollir.add(String.format("%s%s", child.get("bool"), OllirTemplates.type(type)));
                    params.add(type);
                    break;

                case "Variable":
                    List<Object> variable = visit(child, Arrays.asList("PARAM"));

                    stmts = ((String) variable.get(0)).split("\n");
                    if (stmts.length > 1)
                        for (int i = 0; i < stmts.length - 1; i++)
                            builder.append(stmts[i]).append("\n");

                        params.add(((Symbol) variable.get(1)).getType());
                    ollir.add(stmts[stmts.length - 1]);
                    break;

                case "AccessExpression":
                    List<Object> expr = visit(child, Arrays.asList("PARAM"));
                    stmts = ((String) expr.get(0)).split("\n");
                    if (stmts.length > 1)
                        for (int i = 0; i < stmts.length - 1; i++)
                            builder.append(stmts[i]).append("\n");

                    builder.append(String.format("%s%s :=%s %s;\n",  "temporary" + temp_seq, OllirTemplates.type((Type) expr.get(1)), OllirTemplates.type((Type) expr.get(1)), stmts[stmts.length - 1]));
                    ollir.add("temporary" + temp_seq++ + OllirTemplates.type((Type) expr.get(1)));
                    params.add((Type) expr.get(1));
                    break;

                case "RelationalExpression":
                case "AndExpression":
                case "NotExpression":
                    var = (String) visit(child, Arrays.asList("PARAM")).get(0);
                    stmts = var.split("\n");
                    res = binOps(stmts, builder, new Type("boolean", false));
                    params.add(new Type("boolean", false));
                    ollir.add(res);
                    break;

                case "BinOp":
                    var = (String) visit(child, Arrays.asList("PARAM")).get(0);
                    stmts = var.split("\n");
                    res = binOps(stmts, builder, new Type("int", false));
                    params.add(new Type("int", false));
                    ollir.add(res);
                    break;

                default:
                    break;
            }
        }

        return Map.entry(params, String.join(", ", ollir));
    }

    private static List<Object> reduce (List<Object> nodeRes, List<List<Object>> childrenRes) {
        var content = new StringBuilder();

        if (!nodeRes.get(0).equals("DEFAULT_VISIT"))
            content.append(nodeRes.get(0));

        for (var childRes : childrenRes)
            if (!childRes.get(0).equals("DEFAULT_VISIT"))
                content.append(String.join("\n", StringLines.getLines((String) childRes.get(0))));

        if (nodeRes.size() > 1) {
            List<Object> res = new ArrayList<>();
            res.add(nodeRes.get(0));
            res.addAll(nodeRes.subList(1, nodeRes.size()));

            return res;
        }
        else return Arrays.asList(content.toString());
    }

    private String binOps(String[] stmts, StringBuilder builder, Type type) {
        String last;

        if (stmts.length > 1) {
            for (int i = 0; i < stmts.length - 1; i++)
                builder.append(stmts[i]).append("\n");

            String stmt = stmts[stmts.length - 1];
            if (stmt.split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern pattern = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher matcher = pattern.matcher(stmt);
                matcher.find();
                builder.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_seq))), OllirTemplates.assignmentType(matcher.group(1)), stmt));
                last = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_seq++)));
            }
            else last = stmts[0];
        }
        else {
            if (stmts[0].split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern pattern = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher matcher = pattern.matcher(stmts[0]);
                matcher.find();
                builder.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_seq))), OllirTemplates.assignmentType(matcher.group(1)), stmts[0]));
                last = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_seq++)));
            }
            else last = stmts[0];
        }
        return last;
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        /*StringBuilder builder = new StringBuilder();

        if (node.getChildren().size() > 0) {
            int size = node.getChildren().size() - 1;
            String childRes = (String) visit(node.getChildren().get(size), data).get(0);
            if (!childRes.equals("DEFAULT_VISIT")) 
                builder.append(childRes);
        }

        return Collections.singletonList(builder.toString());*/
        return Collections.singletonList("");
    }
}
