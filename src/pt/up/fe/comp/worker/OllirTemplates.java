package worker;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OllirTemplates {
    public static String classTemplate(String name, String extended) {
        if (extended == null) return classTemplate(name);

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s extends %s", name, extended)).append("{\n");
        return builder.toString();
    }

    public static String classTemplate(String name) {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append("{\n");
        return builder.toString();
    }

    public static String constructor(String name) {
        StringBuilder builder = new StringBuilder();
        builder.append(".construct ").append(name).append("().V").append("{\n");
        builder.append("invokespecial(this, \"<init>\").V;");
        builder.append("\n}");
        return builder.toString();
    }

    public static String method(String name, List<String> params, String ret, boolean isStatic) {
        StringBuilder builder = new StringBuilder();

        if (isStatic) builder.append("static ");

        builder.append(name).append("(");
        builder.append(String.join(", ", params));
        builder.append(")");

        builder.append(ret);
        builder.append("{\n");
        return builder.toString();
    }

    public static String method(String name, List<String> params, String ret) {
        return method(name, params, ret, false);
    }

    public static String type(Type type) {
        StringBuilder builder = new StringBuilder();

        if (type.isArray()) builder.append(".array");

        if ("int".equals(type.getName())) builder.append(".i32");
        else if ("void".equals(type.getName())) builder.append(".V");
        else if ("boolean".equals(type.getName())) builder.append(".bool");
        else builder.append(".").append(type.getName());

        return builder.toString();
    }

    public static String binary(String left, String right, String op, Type type) {
        return String.format("%s %s%s %s", left, op, OllirTemplates.type(type), right);
    }

    public static String variable(Symbol var) {
        StringBuilder builder = new StringBuilder(var.getName());

        builder.append(type(var.getType()));

        return builder.toString();
    }

    public static Symbol escapeVariable(Symbol var) {
        if (var.getName().charAt(0) == '$')
            return new Symbol(var.getType(), "dollar_" + var.getName().substring(1));
        else if (var.getName().charAt(0) == '_')
            return new Symbol(var.getType(), "under_" + var.getName().substring(1));
        else if (var.getName().equals("ret") || var.getName().equals("array"))
            return new Symbol(var.getType(), "escaped_" + var.getName());
    
        return var;
    }

    public static String variable(Symbol var, String param) {
        var = escapeVariable(var);

        if (param == null) return variable(var);
        return param + "." + variable(var);
    }

    public static String arrayaccess(Symbol var, String param, String index) {
        var = escapeVariable(var);

        if (param == null) {
            return String.format("%s[%s]%s", var.getName(), index, type(new Type(var.getType().getName(), false)));
        }
        return String.format("%s.%s[%s]%s", param, var.getName(), index, type(new Type(var.getType().getName(), false)));
    }

    public static String assignmentType(String operator) {
        switch(operator) {
            case "+":
            case "*":
            case "/":
            case "-":
                return ".i32";

            case "&&":
            case "||":
            case "!":
            case ">=":
            case "<":
                return ".bool";

            default:
                return ".error";
        }
    }

    public static String ret(Type ret, String exp) {
        return String.format("ret%s %s;", OllirTemplates.type(ret), exp);
    }

    public static String invokestatic(String target, String method, Type ret, String param) {
        if (param.equals(""))
            return String.format("invokestatic(%s, \"%s\")%s", target, method, type(ret));
        return String.format("invokestatic(%s, \"%s\", %s)%s", target, method, param, type(ret));
    }

    public static String invokevirtual(String var, String method, Type ret, String param) {
        if (param.equals(""))
            return String.format("invokevirtual(%s, \"%s\")%s", var != null ? var : "this", method, type(ret));
        return String.format("invokevirtual(%s, \"%s\", %s)%s", var != null ? var : "this", method, param, type(ret));
    }

    public static String invokevirtual(String method, Type ret, String param) {
        return invokevirtual(null, method, ret, param);
    }

    public static String invokespecial(String var, String method, Type ret, String param) {
        if (param.equals(""))
            return String.format("invokespecial(%s, \"%s\")%s", var != null ? var : "this", method, type(ret));
        return String.format("invokespecial(%s, \"%s\", %s)%s", var != null ? var : "this", method, param, type(ret));
    }

    public static String invokespecial(String method, Type ret, String param) {
        return invokespecial(null, method, ret, param);
    }

    public static String arraylength(String var) {
        return String.format("arraylength(%s).i32", var);
    }

    public static String putfield(String var, String value) {
        return String.format("putfield(this, %s, %s).V", var, value);
    }

    public static String getfield(Symbol var) {
        return String.format("getfield(this, %s)%s", variable(var), type(var.getType()));
    }

    public static String field(Symbol var) {
        return String.format(".field public %s;", variable(var));
    }

    public static String arrayinit(String size) {
        return String.format("new(array, %s).array.i32", size);
    }

    public static String objectinit(String objectClass) {
        return String.format("new(%s).%s", objectClass, objectClass);
    }

    public static String objectinstance(Symbol var) {
        return String.format("invokespecial(%s, \"<init>\").V;", variable(var));
    }
}
