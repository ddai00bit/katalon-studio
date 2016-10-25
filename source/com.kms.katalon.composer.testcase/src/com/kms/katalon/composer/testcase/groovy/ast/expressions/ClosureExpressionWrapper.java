package com.kms.katalon.composer.testcase.groovy.ast.expressions;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import com.kms.katalon.composer.testcase.groovy.ast.ASTHasBlock;
import com.kms.katalon.composer.testcase.groovy.ast.ASTNodeWrapHelper;
import com.kms.katalon.composer.testcase.groovy.ast.ASTNodeWrapper;
import com.kms.katalon.composer.testcase.groovy.ast.ClassNodeWrapper;
import com.kms.katalon.composer.testcase.groovy.ast.ParameterWrapper;
import com.kms.katalon.composer.testcase.groovy.ast.statements.BlockStatementWrapper;

public class ClosureExpressionWrapper extends ExpressionWrapper implements ASTHasBlock {
    private static final String UNKNOWN = "<unknown>";

    private ParameterWrapper[] parameters;

    private BlockStatementWrapper code;

    public ClosureExpressionWrapper(ParameterWrapper[] parameters, ASTNodeWrapper parentNodeWrapper) {
        super(parentNodeWrapper);
        this.parameters = parameters;
        this.code = new BlockStatementWrapper(this);
        this.type = ClassNodeWrapper.getClassWrapper(ClassHelper.CLOSURE_TYPE, this);
    }

    public ClosureExpressionWrapper(ClosureExpression closureExpression, ASTNodeWrapper parentNodeWrapper) {
        super(closureExpression, parentNodeWrapper);
        parameters = new ParameterWrapper[closureExpression.getParameters().length];
        for (int i = 0; i < closureExpression.getParameters().length; i++) {
            parameters[i] = new ParameterWrapper(closureExpression.getParameters()[i], this);
        }
        initCodeBlock(closureExpression);
    }

    private void initCodeBlock(ClosureExpression closureExpression) {
        Statement statementCode = closureExpression.getCode();
        if (statementCode instanceof BlockStatement) {
            this.code = new BlockStatementWrapper((BlockStatement) statementCode, this);
            return;
        }
        this.code = new BlockStatementWrapper(this);
        code.addStatement(ASTNodeWrapHelper.getStatementNodeWrapperFromStatement(statementCode, this));
    }

    public ClosureExpressionWrapper(ClosureExpressionWrapper closureExpressionWrapper, ASTNodeWrapper parentNodeWrapper) {
        super(closureExpressionWrapper, parentNodeWrapper);
        parameters = new ParameterWrapper[closureExpressionWrapper.getParameters().length];
        for (int i = 0; i < closureExpressionWrapper.getParameters().length; i++) {
            parameters[i] = new ParameterWrapper(closureExpressionWrapper.getParameters()[i], this);
        }
        this.code = new BlockStatementWrapper(closureExpressionWrapper.getBlock(), this);
    }

    @Override
    public String getText() {
        String paramText = getParametersText(parameters);
        if (paramText.length() > 0) {
            return "{ " + paramText + " -> ... }";
        } else {
            return "{ -> ... }";
        }
    }

    public ParameterWrapper[] getParameters() {
        return parameters;
    }

    @Override
    public boolean hasAstChildren() {
        return true;
    }

    @Override
    public List<? extends ASTNodeWrapper> getAstChildren() {
        List<ASTNodeWrapper> astNodeWrappers = new ArrayList<ASTNodeWrapper>();
        for (ParameterWrapper parameter : parameters) {
            astNodeWrappers.add(parameter);
        }
        astNodeWrappers.add(code);
        return astNodeWrappers;
    }

    @Override
    public BlockStatementWrapper getBlock() {
        return code;
    }

    @Override
    public ClosureExpressionWrapper clone() {
        return new ClosureExpressionWrapper(this, getParent());
    }

    private static String getParameterText(ParameterWrapper node) {
        if (node == null)
            return UNKNOWN;

        String name = node.getName() == null ? UNKNOWN : node.getName();
        String type = node.getType() == null || node.getType().getName() == null ? UNKNOWN : node.getType().getName();
        if (node.getInitialExpression() != null) {
            return type + " " + name + " = " + node.getInitialExpression().getText();
        }
        return type + " " + name;
    }

    private static String getParametersText(ParameterWrapper[] parameters) {
        if (parameters == null)
            return "";
        if (parameters.length == 0)
            return "";
        StringBuilder result = new StringBuilder();
        int max = parameters.length;
        for (int x = 0; x < max; x++) {
            result.append(getParameterText(parameters[x]));
            if (x < (max - 1)) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    public void setParameters(ParameterWrapper[] array) {
        this.parameters = array;
    }

    public void setBlock(BlockStatementWrapper blockStatementWrapper) {
        this.code = blockStatementWrapper;
    }

    @Override
    public boolean updateInputFrom(ASTNodeWrapper input) {
        if (!(input instanceof ClosureExpressionWrapper)) {
            return false;
        }
        ClosureExpressionWrapper closureExpressionWrapper = (ClosureExpressionWrapper) input;
        setParameters(closureExpressionWrapper.getParameters());
        setBlock(closureExpressionWrapper.getBlock());
        return true;
    }
}
