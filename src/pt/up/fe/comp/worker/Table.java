package worker;

import exception.NoSuchMethod;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.*;

public class Table implements SymbolTable {
    private final List<String> imports = new ArrayList<>();
    private String className;
    private String superClassName;
    private final Map<Symbol, Boolean> fields = new HashMap<>();
    private final List<AstMethod> methods = new ArrayList<>();
    private AstMethod currentMethod;

    public static Type getType(JmmNode node, String attr) {
        Type res;

        if (node.get(attr).equals("int[]"))
            res = new Type("int", true);
        else if (node.get(attr).equals("int"))
            res = new Type("int", false);
        else
            res = new Type(node.get(attr), false);

        return res;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClassName(String scName) {
        this.superClassName = scName;
    }

    public void addImport(String impStmt) {
        imports.add(impStmt);
    }

    public void addField(Symbol field) {
        fields.put(field, false);
    }

    public Boolean fieldExists(String name) {
        for (Symbol field : this.fields.keySet()) {
            if (field.getName().equals(name))
                return true;
        }

        return false;
    }

    public AstMethod getMethod(String name, List<Type> params, Type ret) throws NoSuchMethod {
        for (AstMethod method : methods)
            if (method.getName().equals(name) && ret.equals(method.getRet()) && params.size() == method.getParams().size())
                if (AstMethod.matchParameters(params, method.getParameterTypes())) 
                    return method;

        throw new NoSuchMethod(name);
    }

    public Map.Entry<Symbol, Boolean> getField (String name) {
        for (Map.Entry<Symbol, Boolean> field : this.fields.entrySet())
            if (field.getKey().getName().equals(name))
                return field;

        return null;
    }

    public boolean initializeField (Symbol symbol) {
        if (this.fields.containsKey(symbol)) {
            this.fields.put(symbol, true);
            return true;
        }
        return false;
    }

    public void addMethod (String name, Type ret) {
        currentMethod = new AstMethod(name, ret);
        methods.add(currentMethod);
    }

    @Override
    public String toString() {
        StringBuilder build = new StringBuilder("Symbol Table\n");
        build.append("Imports\n");
        for (String impStmt : imports)
            build.append("\t").append(impStmt).append("\n");

        build.append("Class: ").append(className).append(" Extends: ").append(superClassName).append("\n");

        build.append("Local Variables\n");
        for (Map.Entry<Symbol, Boolean> field : fields.entrySet())
            build.append("\t").append(field.getKey()).append(" Initialized: ").append(field.getValue()).append("\n");

        build.append("Methods").append("\n");
        for (AstMethod method : this.methods) {
            build.append(method).append("\n");
        }

        return build.toString();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(this.fields.keySet());
    }

    @Override
    public List<String> getMethods() {
        List<String> methods = new ArrayList<>();
        for (AstMethod method : this.methods) {
            methods.add(method.getName());
        }

        return methods;
    }

    public AstMethod getCurrentMethod() {
        return currentMethod;
    }

    @Override 
    public Type getReturnType(String method) {
        List<Type> params = new ArrayList<>();
        String[] parts = method.split("::");
        method = parts[0];

        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String[] parts2 = parts[i].split(":");
                params.add(new Type(parts2[0], parts2[1].equals("true")));
            }

        }
        else {
            for (AstMethod meth : this.methods) 
                if (meth.getName().equals(method))
                    return meth.getRet();
        }

        for (AstMethod meth : this.methods) {
            if (meth.getName().equals(method)) {
                List<Symbol> current = meth.getParams();
                boolean found = true;
                if (current.size() != params.size()) continue;
                for (int i = 0; i < params.size(); i++) {
                    if (!current.get(i).getType().equals(params.get(i))) {
                        found = false;
                        break;
                    }
                }

                if (found) return meth.getRet();
            }
        }
        
        return null;
    }

    @Override
    public List<Symbol> getParameters (String method) {
        for (AstMethod meth : this.methods)
            if (meth.getName().equals(method))
                return meth.getParams();

        return null;
    }

    @Override
    public List<Symbol> getLocalVariables (String method) {
        return new ArrayList<>();
    }
    
}