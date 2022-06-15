package worker;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import org.specs.comp.ollir.*;
import java.util.*;

public class Generator {
    private ClassUnit unit;
    private String code;
    private int cond;
    private int stack_counter;
    private int max_counter;

    public Generator(ClassUnit unit) {
        this.unit = unit;
    }

    public String generateClass() {
        StringBuilder builder = new StringBuilder("");

        builder.append(".class ").append(unit.getClassName()).append("\n");

        if (unit.getSuperClass() != null) {
            builder.append(".super ").append(unit.getSuperClass()).append("\n");    
        }
        else {
            builder.append(".super java/lang/Object\n");
        }

        for (Field field : unit.getFields()) {
            builder.append(".field '").append(field.getFieldName()).append("' ").append(this.convertType(field.getFieldType())).append("\n");
        }

        for (Method method : unit.getMethods()) {
            this.stack_counter = 0;
            this.max_counter = 0;
            builder.append(this.generateHeader(method));
            String instruction = this.generateInstructions(method);
            if (!method.isConstructMethod()) {
                builder.append(this.generateLimits(method));
                builder.append(instruction);
            }
        }

        return builder.toString();
    }

    private String generateHeader(Method method) {
        if (method.isConstructMethod()) {
            String superior = "java/lang/Object";
            if (unit.getSuperClass() != null) {
                superior = unit.getSuperClass();
            }

            return "\n.method public <init>()V\naload_0\ninvokespecial " + superior + ".<init>()V\nreturn\n.end method\n";
        }

        StringBuilder builder = new StringBuilder("\n.method public ");
        //append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");
        
        if (method.isStaticMethod()) {
            builder.append("static ");
        }
        if (method.isFinalMethod()) {
            builder.append("final ");
        }

        builder.append(method.getMethodName()).append("(");
        for (Element element : method.getParams()) {
            builder.append(this.convertType(element.getType()));
        }

        builder.append(")").append(this.convertType(method.getReturnType())).append("\n");

        return builder.toString();
    }

    private String generateLimits(Method method) {
        StringBuilder builder = new StringBuilder();

        int counter = method.getVarTable().size();
        if (!method.isStaticMethod()) counter++;
        builder.append(".limit locals ").append(counter).append("\n");
        builder.append(".limit stack ").append(max_counter).append("\n");

        return builder.toString();
    }

    private String generateInstructions(Method method) {
        StringBuilder builder = new StringBuilder();
        method.getVarTable();
        for (Instruction instr : method.getInstructions()) {
            builder.append(generateInstruction(instr, method.getVarTable(), method.getLabels()));
            if (instr instanceof CallInstruction) {
                CallInstruction callInst = (CallInstruction) instr;
                if (callInst.getReturnType().getTypeOfElement() != ElementType.VOID) {
                    builder.append("pop\n");
                    this.decrement(1);
                }
            }
        }

        builder.append("\n.end method\n");
        return builder.toString();
    }

    private String generateInstruction(Instruction instr, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> methodLab) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : methodLab.entrySet()) {
            if (entry.getValue().equals(instr))
                builder.append(entry.getKey()).append(":\n");
        }

        switch(instr.getInstType()) {
            case ASSIGN:
                return builder.append(generateAssign((AssignInstruction) instr, varTable)).toString();
            case NOPER:
                return builder.append(generateSingleOp((SingleOpInstruction) instr, varTable)).toString();
            case BINARYOPER:
                return builder.append(generateBinOp((BinaryOpInstruction) instr, varTable)).toString();
            case UNARYOPER:
                return "Deal with '!' in correct form";
            case CALL:
                return builder.append(generateCall((CallInstruction) instr, varTable)).toString();
            case BRANCH:
                return builder.append(generateBranch((CondBranchInstruction) instr, varTable)).toString();
            case GOTO:
                return builder.append(generateGoto((GotoInstruction) instr, varTable)).toString();
            case PUTFIELD:
                return builder.append(generatePut((PutFieldInstruction) instr, varTable)).toString();
            case GETFIELD:
                return builder.append(generateGet((GetFieldInstruction) instr, varTable)).toString();
            case RETURN:
                return builder.append(generateReturn((ReturnInstruction) instr, varTable)).toString();
            default:
                return "Error in Instruction";
        }

    }

    private String generateAssign(AssignInstruction inst, HashMap<String, Descriptor> varTable) {
        String builder = "";
        Operand op = (Operand) inst.getDest();
        if (op instanceof ArrayOperand) {
            ArrayOperand aop = (ArrayOperand) op;
            builder += String.format("aload%s\n", this.getVirtual(aop.getName(), varTable));
            this.increment(1);
            builder += loadElement(aop.getIndexOperands().get(0), varTable);
        }
        builder += generateInstruction(inst.getRhs(), varTable, new HashMap<String, Instruction>());
        if (!(op.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && inst.getRhs() instanceof CallInstruction))
            builder += this.storeElement(op, varTable);
            
        return builder;
    }

    private String generateSingleOp (SingleOpInstruction inst, HashMap<String, Descriptor> varTable) {
        return loadElement(inst.getSingleOperand(), varTable);
    }

    private String generateBinOp (BinaryOpInstruction inst, HashMap<String, Descriptor> varTable) {
        switch(inst.getOperation().getOpType()) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return this.generateInt(inst, varTable);

            case LTH:
            case GTE:
            case ANDB:
            case NOTB:
            case EQ:
                return this.generateBool(inst, varTable);

            default:
                return "Error in BinOp\n";
        }
    }

    private String generateInt (BinaryOpInstruction inst, HashMap<String, Descriptor> varTable) {
        String leftOp = loadElement(inst.getLeftOperand(), varTable);
        String rightOp = loadElement(inst.getRightOperand(), varTable);
        String op;

        switch(inst.getOperation().getOpType()) {
            case ADD:
                op = "iadd\n";
                break;
            case SUB:
                op = "isub\n";
                break;
            case MUL:
                op = "imul\n";
                break;
            case DIV:
                op = "idiv\n";
                break;
            default:
                return "Error in intOp";
        }

        this.decrement(1);
        return leftOp + rightOp + op;
    }

    private String generateBool (BinaryOpInstruction inst, HashMap<String, Descriptor> varTable) {
        OperationType opt = inst.getOperation().getOpType();
        StringBuilder builder = new StringBuilder();

        switch(inst.getOperation().getOpType()) {
            case LTH:
            case LTE: {
                String leftOp = loadElement(inst.getLeftOperand(), varTable);
                String rightOp = loadElement(inst.getRightOperand(), varTable);

                builder.append(leftOp).append(rightOp).append(this.generateRelational(opt, this.getTrueLabel())).append("iconst_1\n").append("goto ").append(this.getEndIfLabel()).append("\n").append(this.getTrueLabel()).append(":\n").append("iconst_0\n").append(this.getEndIfLabel()).append(":\n");
                this.decrement(1);
                break;
            }
            
            case EQ:
            case ANDB: {
                String ifeq = "ifeq " + this.getTrueLabel() + "\n";

                builder.append(loadElement(inst.getLeftOperand(), varTable)).append(ifeq);
                this.decrement(1);

                builder.append(loadElement(inst.getRightOperand(), varTable)).append(ifeq);
                this.decrement(1);

                builder.append("iconst_1\n").append("goto ").append(this.getEndIfLabel()).append("\n").append(this.getTrueLabel()).append(":\n").append("iconst_0\n").append(this.getEndIfLabel()).append(":\n");
                this.increment(1);
                break;
            }

            case NOTB: {
                String op = loadElement(inst.getLeftOperand(), varTable);

                builder.append(op).append("ifne ").append(this.getTrueLabel()).append("\n").append("iconst_1\n").append("goto ").append(this.getEndIfLabel()).append("\n").append(this.getTrueLabel()).append(":\n").append("iconst_0\n").append(this.getEndIfLabel()).append(":\n");

                break;
            }

            default:
                return "Error in boolOp";
        }

        this.cond++;
        return builder.toString();
    }

    private String generateRelational(OperationType opt, String trueLabel) {
        switch(opt) {
            case LTH:
                return String.format("if_icmpge %s\n", trueLabel);
            case GTE:
                return String.format("if_icmplt %s\n", trueLabel);
            default:
                return "Error in relOp";
        }
    }

    private String generateBranch (CondBranchInstruction inst, HashMap<String, Descriptor> varTable) {
        StringBuilder builder = new StringBuilder();
        var cond = inst.getCondition();
        if (cond instanceof OpInstruction){
            OpInstruction opInts = (OpInstruction) cond;
            switch(opInts.getOperation().getOpType()) {
                case NOTB:
                    builder.append(this.loadElement(inst.getOperands().get(0), varTable)).append("ifeq ").append(inst.getLabel()).append("\n");
                    this.decrement(1);
                    break;

                default:
                    return "Error in CondBranch";
            }
        }
        else {
            return "Error in CondBranch";
        }

        return builder.toString();
    }

    private String generateGoto (GotoInstruction inst, HashMap<String, Descriptor> varTable) {
        return String.format("goto %s\n", inst.getLabel());
    }

    private String generateCall(CallInstruction inst, HashMap<String, Descriptor> varTable) {
        String builder = "";
        CallType callType = inst.getInvocationType();
        switch(callType) {
            case invokevirtual:
            case invokespecial:
                builder += this.generateInvoke(inst, varTable, callType, ((ClassType) inst.getFirstArg().getType()).getName());
                break;
            case invokestatic:
                builder += this.generateInvoke(inst, varTable, callType, ((Operand) inst.getFirstArg()).getName());
                break;
            case arraylength:
                builder += this.loadElement(inst.getFirstArg(), varTable);
                builder += "arraylength\n";
                break;
            case NEW:
                builder += this.generateNew(inst, varTable);
                break;
            default:
                return "Error in Call";
        }   

        return builder;
    }

    private String generateInvoke(CallInstruction inst, HashMap<String, Descriptor> varTable, CallType type, String name) {
        String builder = "";
        String functionLit = ((LiteralElement) inst.getSecondArg()).getLiteral();
        String params = "";
        if (!functionLit.equals("\"<init>\""))
            builder += this.loadElement(inst.getFirstArg(), varTable);

        int nParams = 0;
        for (Element element : inst.getListOfOperands()) {
            builder += this.loadElement(element, varTable);
            params += this.convertType(element.getType());
            nParams++;
        }

        if (!inst.getInvocationType().equals(CallType.invokestatic)) {
            nParams++;
        }
        this.decrement(nParams);
        if (inst.getReturnType().getTypeOfElement() != ElementType.VOID)
            this.increment(1);

        builder += type.name() + " " + this.getObjectClassName(name) + "." + functionLit.replace("\"", "") + "(" + params + ")" + this.convertType(inst.getReturnType()) + "\n";
        if (functionLit.equals("\"<init>\"") && !name.equals("this"))
            builder += this.storeElement((Operand) inst.getFirstArg(), varTable);

        return builder;
    }

    private String generateNew(CallInstruction inst, HashMap<String, Descriptor> varTable) {
        Element e = inst.getFirstArg();
        String builder = "";

        if (e.getType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
            builder += this.loadElement(inst.getListOfOperands().get(0), varTable);
            builder += "newarray int\n";
        }
        else if (e.getType().getTypeOfElement().equals(ElementType.OBJECTREF)) {
            this.increment(2);
            builder += "new " + this.getObjectClassName(((Operand) e).getName()) + "\ndup\n";
        }

        return builder;
    }

    private String generatePut (PutFieldInstruction inst, HashMap<String, Descriptor> varTable) {
        String builder = "";
        Operand obj = (Operand) inst.getFirstOperand();
        Operand var = (Operand) inst.getSecondOperand();
        Element value = inst.getThirdOperand();

        builder += this.loadElement(obj, varTable);
        builder += this.loadElement(value, varTable);
        this.decrement(2);

        return builder + "putfield " + unit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) + "\n";
    }

    private String generateGet (GetFieldInstruction inst, HashMap<String, Descriptor> varTable) {
        String code = "";
        Operand obj = (Operand) inst.getFirstOperand();
        Operand var = (Operand) inst.getSecondOperand();
        code += this.loadElement(obj, varTable);

        return code + "getfield " + unit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) + "\n";
    }

    private String generateReturn (ReturnInstruction inst, HashMap<String, Descriptor> varTable) {
        if (!inst.hasReturnValue()) return "return";
        String builder = "";

        switch (inst.getOperand().getType().getTypeOfElement()) {
            case VOID:
                builder = "return";
                break;
            case INT32:
            case BOOLEAN:
                builder = loadElement(inst.getOperand(), varTable);
                this.decrement(1);
                builder += "ireturn";
                break;
            case ARRAYREF:
            case OBJECTREF:
                builder = loadElement(inst.getOperand(), varTable);
                this.decrement(1);
                builder += "areturn";
                break;
            default:
                break;
        }

        return builder;
    }

    private String convertType(Type type) {
        ElementType eType = type.getTypeOfElement();
        String builder = "";

        if (eType == ElementType.ARRAYREF) {
            eType = ((ArrayType) type).getTypeOfElements();
            builder += "[";
        }

        switch (eType) {
            case INT32:
                return builder + "I";
            case BOOLEAN:
                return builder + "Z";
            case STRING:
                return builder + "Ljava/lang/String;";
            case OBJECTREF:
                String name = ((ClassType) type).getName();
                return builder + "L" + this.getObjectClassName(name) + ";";
            case CLASS:
                return "CLASS";
            case VOID:
                return "V";
            default:
                return "Error converting ElementType";
        }
    }

    private String getObjectClassName (String name) {
        List<String> imports = unit.getImports();

        for (int i = 0; i < imports.size(); i++)
            if (imports.get(i).endsWith("." + name))
                return imports.get(i).replaceAll("\\.", "/");

        return name;
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String num = ((LiteralElement) element).getLiteral();
            this.increment(1);
            return this.selectConstType(num) + "\n";
        }

        else if (element instanceof ArrayOperand) {
            ArrayOperand op = (ArrayOperand) element;

            String builder = String.format("aload%s\n", this.getVirtual(op.getName(), varTable));
            this.increment(1);
            builder += loadElement(op.getIndexOperands().get(0), varTable);

            this.decrement(1);
            return builder + "iaload\n";
        }

        else if (element instanceof Operand) {
            Operand op = (Operand) element;
            switch (op.getType().getTypeOfElement()) {
                case THIS:
                    this.increment(1);
                    return "aload_0\n";
                case INT32:
                case BOOLEAN:
                    this.increment(1);
                    return String.format("iload%s\n", this.getVirtual(op.getName(), varTable));
                case OBJECTREF:
                case ARRAYREF:
                    this.increment(1);
                    return String.format("aload%s\n", this.getVirtual(op.getName(), varTable));
                case CLASS:
                    return "";
                default:
                    return "Error in loadElement\n";
            }
        }

        else return "Error in loadElements\n";
    }

    private String storeElement(Operand op, HashMap<String, Descriptor> varTable) {
        if (op instanceof ArrayOperand) {
            this.decrement(3);
            return "iastore\n";
        }    

        switch (op.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                this.decrement(1);
                return String.format("istore%s\n", this.getVirtual(op.getName(), varTable));
            case OBJECTREF:
            case ARRAYREF:
                this.decrement(1);
                return String.format("astore%s\n", this.getVirtual(op.getName(), varTable));
            default:
                return "Error in storeElements";
        }
    }

    private String getVirtual (String name, HashMap<String, Descriptor> varTable) {
        int virtual = varTable.get(name).getVirtualReg();
        if (virtual > 3) return " " + virtual;
        else return "_" + virtual;
    }

    private String getTrueLabel() {
        return "myTrue" + this.cond;
    }

    private String getEndIfLabel() {
        return "myEndIf" + this.cond;
    }

    private String selectConstType (String literal) {
        return Integer.parseInt(literal) < - 1 || Integer.parseInt(literal) > 5 ?
                    Integer.parseInt(literal) < -128 || Integer.parseInt(literal) > 127 ?
                        Integer.parseInt(literal) < -32768 || Integer.parseInt(literal) > 32767 ?
                            "ldc " + literal :
                        "sipush " + literal :
                    "bipush " + literal :
                "iconst_" + literal;
    }

    private void increment(int add) {
        this.stack_counter += add;
        if (this.stack_counter > this.max_counter) this.max_counter = stack_counter;
    }

    private void decrement(int sub) {
        this.stack_counter -= sub;
    }

}