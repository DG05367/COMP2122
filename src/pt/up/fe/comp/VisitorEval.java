package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class VisitorEval extends AJmmVisitor<Object, Integer> {

    public VisitorEval() {

        addVisit("AdditiveExpression", this::addExprVisit);
        addVisit("MultiplicativeExpression", this::mulExprVisit);
        addVisit("Factor", this::factorVisit);
        addVisit("INTEGER", this::integerVisit);

        setDefaultVisit(this::defaultVisit);
    }

    private Integer addExprVisit(JmmNode node, Object dummy) {

        int result = visit(node.getJmmChild(0));

        if (node.getNumChildren() == 3) {

            boolean isAdd = node.getJmmChild(1).getKind().equals("PLUS");
            int nextOperand = visit(node.getJmmChild(2));

            if (isAdd) {
                result = result + nextOperand;
            } else {
                result = result - nextOperand;
            }
        }

        return result;
    }

    private Integer mulExprVisit(JmmNode node, Object dummy) {

        int result = visit(node.getJmmChild(0));

        if (node.getNumChildren() == 3) {

            boolean isMult = node.getJmmChild(1).getKind().equals("TIMES");
            int nextOperand = visit(node.getJmmChild(2));

            if (isMult) {
                result = result * nextOperand;
            } else {
                result = result / nextOperand;
            }
        }

        return result;
    }

    private Integer factorVisit(JmmNode node, Object dummy) {

        switch (node.getNumChildren()) {
        case 1: // just an integer, e.g., 2
            return visit(node.getJmmChild(0));
        case 2: // a negated integer, e.g., -2
            return -1 * visit(node.getJmmChild(1));
        case 3: // and expression surrounded by parenthesis, e.g. (2+3)
            return visit(node.getJmmChild(1));
        default:
            throw new RuntimeException("Wrong no. of nodes in Factor.");
        }
    }

    private Integer integerVisit(JmmNode node, Object dummy) {

        return Integer.parseInt(node.get("image"));
    }

    private Integer defaultVisit(JmmNode node, Object dummy) {

        if (node.getNumChildren() != 2) { // Start has 2 children because <LF> is a Token Node
            throw new RuntimeException("Illegal number of children in node " + node.getKind() + ".");
        }

        return visit(node.getJmmChild(0));
    }
}
