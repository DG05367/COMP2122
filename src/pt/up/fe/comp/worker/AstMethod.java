package worker;

import exception.WrongArgumentType;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class AstMethod {
    private String name;
    private Type ret;
    private final List<Map.Entry<Symbol, String>> params = new ArrayList<>();
    private final Map<Symbol, Boolean> localVars = new HashMap<>();

    public AstMethod(String name, Type ret) {
        this.name = name;
        this.ret = ret;
    }

    public List<Type> getParameterTypes() {
        List<Type> args = new ArrayList<>();

        for (Map.Entry<Symbol, String> param : this.params)
            args.add(param.getKey().getType());

        return args;
    }

    public void addLocalVars(Symbol var) {
        localVars.put(var, false);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getRet() {
        return ret;
    }

    public void setRet(Type ret) {
        this.ret = ret;
    }

    public void addParam(Symbol param) {
        this.params.add(Map.entry(param, "param"));
    }

    public boolean fieldExists(String field) {
        for (Symbol localVar : this.localVars.keySet())
            if (localVar.getName().equals(field))
                return true;

        return false;
    }

    public Map.Entry<Symbol, Boolean> getField(String name) {
        for (Map.Entry<Symbol, Boolean> field : this.localVars.entrySet())
            if (field.getKey().getName().equals(name))
                return field;
                
        for (Map.Entry<Symbol, String> param : this.params)
            if (param.getKey().getName().equals(name))
                return Map.entry(param.getKey(), true);

        return null;
    }

    public boolean initField(Symbol symbol) {
        if (this.localVars.containsKey(symbol)) {
            this.localVars.put(symbol, true);
            return true;
        }

        return false;
    }

    public List<Symbol> getParams() {
        List<Symbol> args = new ArrayList<>();
        for (Map.Entry<Symbol, String> param : this.params)
            args.add(param.getKey());

        return args;
    }

    public List<Symbol> getLocalVars() {
        return new ArrayList<>(this.localVars.keySet());
    }

    @Override
    public String toString() {
        StringBuilder build = new StringBuilder();
        build.append("Name: ").append(this.name).append(" Return: ").append(this.ret).append("\n");

        build.append("Parameters\n");
        for (Map.Entry<Symbol, String> param : this.params)
            build.append("\t").append(param.getKey()).append("\n");

        build.append("Local Variables\n");
        for (Map.Entry<Symbol, Boolean> localVar : this.localVars.entrySet())
            build.append("\t").append(localVar.getKey()).append(" Initialized: ").append(localVar.getValue()).append("\n");
        
        return build.toString();
    }

    public static boolean matchParameters(List<Type> t1, List<Type> t2) {
        for (int i = 0; i < t1.size(); i++)
            if (!t1.get(i).equals(t2.get(i)))
                return false;

        return true;
    }

    public static List<Type> parseParameters(String params) {
        if (params.equals("")) return new ArrayList<>();

        String[] types = params.split(",");
        List<Type> t = new ArrayList<>();

        for (String i : types) {
            String[] aux = i.split(" ");
            if (aux[0].equals("int[]"))
                t.add(new Type("int", true));
            else
                t.add(new Type(aux[0], aux.length == 2));
        }

        return t;
    }

    public String isParameter(Symbol symbol) {
        for (int i = 1; i < this.params.size() + 1; i++)
            if (params.get(i - 1).getKey() == symbol)
                return "$" + 1;

        return null;
    }

    public List<String> parametersToOllir() {
        List<String> ollir = new ArrayList<>();

        for (Map.Entry<Symbol, String> param : this.params)
            ollir.add(OllirTemplates.variable(param.getKey()));

        return ollir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AstMethod meth = (AstMethod) o;
        return Objects.equals(name, meth.name) && Objects.equals(ret, meth.ret) && Objects.equals(params, meth.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ret, params);
    }
    
}
