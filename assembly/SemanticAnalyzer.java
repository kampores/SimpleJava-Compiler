import java.util.Vector;

public class SemanticAnalyzer implements ASTVisitor {
	
	private AATBuildTree bt;
	private VariableEnvironment varEnv;
	private FunctionEnvironment funcEnv;
	private TypeEnvironment typeEnv;
	
	private String currentClassName;
	
	private int currentOffset;
	private String currentFunctionEnd;
	
	 private Type functionReturn;
	 private Label functionEnd;
	public SemanticAnalyzer() {
		varEnv = new VariableEnvironment();
		funcEnv = new FunctionEnvironment();
		funcEnv.addBuiltinFunctions();
		typeEnv = new TypeEnvironment();
		currentClassName = "";
		bt = new AATBuildTree();
		currentOffset = 0;
        functionReturn = null;
        functionEnd = null;
	}
	
	private void beginClassScope(String name) {
		currentClassName = name;
	}
	
	private void endClassScope(String name) {
		currentClassName = "";
	}
	

	
	public Object VisitArrayVariable(ASTArrayVariable array) {
		
		// check the type is array type.
		TypeClass base = (TypeClass)(array.base().Accept(this));
		Type basetype = base.type();
		
		if(!(basetype instanceof ArrayType))
		{
			CompError.message(array.line(),"array type is not array");
			return new TypeClass(IntegerType.instance(), null);
		}
		TypeClass index = (TypeClass)(array.index().Accept(this));
		Type indextype = index.type();
		if(indextype != IntegerType.instance()){
			CompError.message(array.line(),"array type is not integer");
			return BooleanType.instance();
		} else {
			return new TypeClass(((ArrayType)basetype).type(), bt.arrayVariable(base.value(), index.value(), MachineDependent.WORDSIZE));
		}
	}

	public Object VisitAssignmentStatement(ASTAssignmentStatement assign) {
		TypeClass variable = (TypeClass)(assign.variable().Accept(this));
		TypeClass value = (TypeClass)(assign.value().Accept(this));
		if(variable.type() != value.type()) {
			CompError.message(assign.line(),"Type of var is not the same as exp");
		}
		return bt.assignmentStatement(variable.value(), value.value());
	}

	public Object VisitBaseVariable(ASTBaseVariable base) {
		VariableEntry baseEntry = varEnv.find(base.name());
		if(baseEntry == null) {
			CompError.message(base.line(), "Variable "+ base.name() + " is not defined in this scope");
			return new TypeClass(IntegerType.instance(), null);
		} else {
			return new TypeClass(baseEntry.type(), bt.baseVariable(baseEntry.offset()));
		}
	}

	public Object VisitBooleanLiteral(ASTBooleanLiteral boolliteral) {
		if(boolliteral.value() == true)
			return new TypeClass(BooleanType.instance(), bt.constantExpression(1));
		else
			return new TypeClass(BooleanType.instance(), bt.constantExpression(0));
	}
	
	
	public Object VisitClass(ASTClass astclass) {	
		/*if(typeEnv.find(astclass.name()) != null) {
			CompError.message(astclass.line(), "Class "+ astclass.name() + " is already defined in this scope");
			return null;
		}
		ClassType type = new ClassType(new VariableEnvironment());
		currentOffset = 0;
		typeEnv.insert(astclass.name(), type);
		beginClassScope(astclass.name());
		if (astclass.variabledefs() != null){
			astclass.variabledefs().Accept(this);
		}
		endClassScope(astclass.name());
		return bt.emptyStatement();  */

		 currentOffset = 0;
        VariableEntry baseEntry = varEnv.find(astclass.name());
        if (baseEntry != null) {
            CompError.message(astclass.line(), "Class " + astclass.name() + " is already defined");
            return null;
        }

        astclass.variabledefs().Accept(this);

        Type tc;
        VariableEnvironment instanceVars = new VariableEnvironment();

        String varStepTypeName;
        Type varBaseType;
        Type varStepType;

        for (int i = 0; i < astclass.variabledefs().size(); i++) {
            varStepTypeName = astclass.variabledefs().elementAt(i).type();
            varBaseType = typeEnv.find(varStepTypeName);
            varStepType = varBaseType;

            for (int j = 0; j < astclass.variabledefs().elementAt(i).arraydimension(); j++) {
                varStepTypeName = varStepTypeName + "[]";
                varStepType = typeEnv.find(varStepTypeName);
                if (varStepType == null) {
                    varStepType = new ArrayType(varBaseType);
                    typeEnv.insert(varStepTypeName, varStepType);
                }
                varBaseType = varStepType;
            }

            VariableEntry varEntry = new VariableEntry(varStepType, currentOffset);
            currentOffset += MachineDependent.WORDSIZE;
            instanceVars.insert(astclass.variabledefs().elementAt(i).name(), varEntry);
        }
        tc = new ClassType(instanceVars);
        typeEnv.insert(astclass.name(), tc);
        return null;
	}

	public Object VisitClasses(ASTClasses classes) { 
		for (int i=0; i<classes.size();i++) 
			classes.elementAt(i).Accept(this);
		return bt.emptyStatement();
	}

	// check the classvariable in variableEnvironment
	public Object VisitClassVariable(ASTClassVariable classvariable) {
		/*Type varType = (Type)classvariable.base().Accept(this);
		if(!(varType instanceof ClassType) && (varType == null)) {
		CompError.message(classvariable.line(), "Not a class variable");
		return new TypeClass(IntegerType.instance(), null);
		}
		VariableEntry classVarEntry = ((ClassType)varType).variables().find(classvariable.variable());
		if(classVarEntry == null) {
		CompError.message(classvariable.line(), classvariable.variable() + " is not a member of the class variable");
		return new TypeClass(IntegerType.instance(), null);
		}
		else {
		return classVarEntry.type();
		}*/
		TypeClass var = (TypeClass)classvariable.base().Accept(this);
		if((!(var.type() instanceof ClassType)) || (var.type() == null)) {
			CompError.message(classvariable.line(), "Not a class variable");
			return new TypeClass(IntegerType.instance(), null);
		}
		VariableEntry classVarEntry = ((ClassType)(var.type())).variables().find(classvariable.variable());
		if(classVarEntry == null) {
			CompError.message(classvariable.line(), classvariable.variable() + " is not a member of the class variable");
			return new TypeClass(IntegerType.instance(), null);
		}
		else {
			return new TypeClass(classVarEntry.type(), bt.classVariable(var.value(), classVarEntry.offset()));
		}
	}

	public Object VisitDoWhileStatement(ASTDoWhileStatement dowhile) {
		TypeClass test;
		AATStatement dowhilebody;
			
			test = (TypeClass)dowhile.test().Accept(this);
		if(!(test.type() instanceof BooleanType)) {
			CompError.message(dowhile.line(),"While test must be a boolean");
		}
		dowhilebody = (AATStatement)dowhile.body().Accept(this);
		return bt.dowhileStatement(test.value(), dowhilebody);	
		/*	Type test = (Type)dowhile.test().Accept(this);
		if (test != BooleanType.instance()) {
		CompError.message(dowhile.line(),"While test must be a boolean");
		}
		dowhile.body().Accept(this);
		return null;
		*/
	}

	//TODO check this:
	public Object VisitEmptyStatement(ASTEmptyStatement empty) {
		return bt.emptyStatement();
	}
    
	//TODO check this.				 
	public Object VisitForStatement(ASTForStatement forstmt) {
		TypeClass test;
		AATStatement init;
		AATStatement increment;
		AATStatement body;
		
		test = (TypeClass) forstmt.test().Accept(this);
		
		if (test.type() != BooleanType.instance()) {
			CompError.message(forstmt.line(),"ForStatement's test must be a boolean");
		}
		
		if (!(forstmt.initialize() instanceof ASTAssignmentStatement)) {
			CompError.message(forstmt.line(), "ForStatement's init must be assignmentStatement.") ;
		}
		init = (AATStatement)forstmt.initialize().Accept(this);
		
		if (!(forstmt.increment() instanceof ASTAssignmentStatement)) {
			CompError.message(forstmt.line(), "ForStatement's increment must be assignmentStatement.") ;
		}
		increment = (AATStatement)forstmt.increment().Accept(this);
		
		varEnv.beginScope();
		body = (AATStatement)forstmt.body().Accept(this);
		varEnv.endScope();
	
		return bt.forStatement(init, test.value(), increment, body);
	}
	
	//add(int a, X b)
	public Object VisitFormal(ASTFormal formal) {
		if(formal.name() == null){
			CompError.message(formal.line()," formal's name is null");
		}
		String typeName = formal.type();
		Type type = typeEnv.find(typeName);
		if(type == null) {
			CompError.message(formal.line(),"Type not declared");
			return new TypeClass(IntegerType.instance(), null);
		}
		for(int i=0; i < formal.arraydimension(); i++){
			typeName += "[]";
			if(typeEnv.find(typeName) == null) {
				typeEnv.insert(typeName, new ArrayType(type));
			}
			type = typeEnv.find(typeName);
		}
		
		varEnv.insert(formal.name(), new VariableEntry(type, currentOffset));
		currentOffset += MachineDependent.WORDSIZE;
		
		return type;
	}
    
	public Object VisitFormals(ASTFormals formals) { 
		Vector types = new Vector();
		for (int i=0; i<formals.size(); i++) {
			types.addElement((Type)formals.elementAt(i).Accept(this));
			varEnv.insert(formals.elementAt(i).name(), new VariableEntry((Type)formals.elementAt(i).Accept(this), -(i+1)*MachineDependent.WORDSIZE));
		}
		return types;
	}
    
	public Object VisitFunctionDefinitions(ASTFunctionDefinitions functiondefs) {
		AATStatement res = bt.emptyStatement();
		for (int i=0; i < functiondefs.size(); i++) {
			// since functionDefinition is superclass of ASTPrototype and ASTFunction.
			res = bt.sequentialStatement(res, (AATStatement)(functiondefs.elementAt(i).Accept(this)));
		}
		return res;
	}

	public Object VisitFunctionCallExpression(ASTFunctionCallExpression functioncall) { 
		//Function Calls:must be declared before they are called,the nums and types of parameter must match the declaration. type of a function call is also determined by its declaraion.
		FunctionEntry functionEntry = funcEnv.find(functioncall.name());
		if(functionEntry == null){
			CompError.message(functioncall.line(),"Function "+functioncall.name()+" is not declared");
			return new TypeClass(IntegerType.instance(), null);
		} else {
			Vector actuals = new Vector();
			for (int i=0; i<functioncall.size(); i++) {
				TypeClass in = (TypeClass)(functioncall.elementAt(i).Accept(this));
				Type argumentType = (Type)(functionEntry.formals().elementAt(i));
				if(in.type() != argumentType) {
					CompError.message(functioncall.line(), "Wrong argument type");
				}
				else {
					actuals.addElement(in.value());
				}
			}
			String label = functioncall.name();
			if(label.equals("main"))
				label = label + "1";
			return new TypeClass(functionEntry.result(), bt.callExpression(actuals, Label.AbsLabel(label)));
		}
	}
    

	public Object VisitFunctionCallStatement(ASTFunctionCallStatement functioncall) {
		//books p188
		FunctionEntry functionEntry = funcEnv.find(functioncall.name());
		if(functionEntry == null){
			CompError.message(functioncall.line(),"Function "+functioncall.name()+" is not declared");
			return null;
		} else {
			Vector actuals = new Vector();
			for (int i=0; i<functioncall.size(); i++) {
				TypeClass in = (TypeClass)(functioncall.elementAt(i).Accept(this));
				Type argumentType = (Type)(functionEntry.formals().elementAt(i));
				if(in.type() != argumentType) {
					CompError.message(functioncall.line(), "Wrong argument type");
				}
				else {
					actuals.addElement(in.value());
				}
			}
			String label = functioncall.name();
			if(label.equals("main"))
				label = label + "1";
			return bt.callStatement(actuals, Label.AbsLabel(label));
		}
	}
	//This is right, from textbook.
	public Object VisitIfStatement(ASTIfStatement ifstmt) {
		TypeClass test;
		AATStatement thenstm;
		AATStatement elsestm;
				
				
		test = (TypeClass) ifstmt.test().Accept(this);
		thenstm = (AATStatement)ifstmt.thenstatement().Accept(this);
		if(ifstmt.elsestatement() != null) {
			elsestm = (AATStatement)ifstmt.elsestatement().Accept(this);
		}else {
			elsestm = null;
		}
				
		if(test.type() != BooleanType.instance()) {
			CompError.message(ifstmt.line(), "If test must be a boolean");
		}
		return bt.ifStatement(test.value(), thenstm, elsestm);
	}

	//TODO check 
	public Object VisitIntegerLiteral(ASTIntegerLiteral literal) {
				
		return new TypeClass(IntegerType.instance(), bt.constantExpression(literal.value()));
	}

	public Object VisitNewArrayExpression(ASTNewArrayExpression newarray) {
		//check the type has been declared
		String typeName = newarray.type();
		Type type = typeEnv.find(typeName);
		if(type == null ){
			CompError.message(newarray.line(),"type not declared");
			return new TypeClass(IntegerType.instance(), null);
		}
		int i;
		for(i=0; i<newarray.arraydimension();i++){
			typeName = typeName+"[]";
			if(typeEnv.find(typeName) == null) {
				typeEnv.insert(typeName, new ArrayType(type));
			}
			type = typeEnv.find(typeName);
		}
		TypeClass elements = (TypeClass)(newarray.elements().Accept(this));
		if(elements.type() != IntegerType.instance()) {
			CompError.message(newarray.line(), "Array elements must be an integer");
			return new TypeClass(IntegerType.instance(), null);
		}
		return new TypeClass(type, bt.allocate(bt.operatorExpression(
			elements.value(),
				bt.constantExpression(MachineDependent.WORDSIZE),
					AATOperator.MULTIPLY)));
	}

	public Object VisitNewClassExpression(ASTNewClassExpression newclass) {
		Type type = typeEnv.find(newclass.type());
		if(type == null || (!(type instanceof ClassType))){
			CompError.message(newclass.line(),"Class not declared");
			return new TypeClass(IntegerType.instance(), null);
		}
		return new TypeClass(type, bt.allocate(bt.operatorExpression(
			bt.constantExpression(((ClassType)type).variables().size()),
				bt.constantExpression(MachineDependent.WORDSIZE),
					AATOperator.MULTIPLY)));
	}
			
	//TODO VVisitOperatorExpression.  is this right?
	public Object VisitOperatorExpression(ASTOperatorExpression opexpr) {
		TypeClass left;
		TypeClass right;
		AATExpression returntree;
				
		left = (TypeClass)opexpr.left().Accept(this);
		right = (TypeClass)opexpr.right().Accept(this);
		
		Type lefttype = left.type();
		Type righttype = right.type();

		if((opexpr.operator() >=1) && (opexpr.operator() <=4)) {
			if((lefttype != IntegerType.instance()) || (righttype != IntegerType.instance())) {
				CompError.message(opexpr.line(),"operator requires integer operands");
			}
			returntree = bt.operatorExpression(left.value(), right.value(), opexpr.operator());
					
			return new TypeClass(IntegerType.instance(), returntree);
		}
		else if ((opexpr.operator() >= 5)&&(opexpr.operator() <=6)) {
			if((lefttype != BooleanType.instance()) ||(righttype != BooleanType.instance())){
				CompError.message(opexpr.line(),"operator requires boolean operands");
			}
			returntree = bt.operatorExpression(left.value(), right.value(), opexpr.operator());
			return new TypeClass(BooleanType.instance(), returntree);
					
		}
		else if ((opexpr.operator() >= 7)&&(opexpr.operator() <=12)) {
			if((lefttype != IntegerType.instance()) || (righttype != IntegerType.instance())) {
				CompError.message(opexpr.line(), "operator requires integer operands");
			}
			returntree = bt.operatorExpression(left.value(), right.value(), opexpr.operator());
			return new TypeClass(BooleanType.instance(), returntree);
		}
		else {
			return null;
		}
	}


	public Object VisitProgram(ASTProgram program) { 
		AATStatement res = null;
		if (program.classes() != null) 
			program.classes().Accept(this);
		if (program.functiondefinitions() != null)
			res = (AATStatement)(program.functiondefinitions().Accept(this));
		return res;
	}
	//Textbook 173 instruction+173 Figure7.14.
	public Object VisitPrototype(ASTPrototype prototype) {	
		Type result = typeEnv.find(prototype.type());
		if(result == null) {
			CompError.message(prototype.line(),"Type "+prototype.type()+" is not definded");
		}
		varEnv.beginScope();
		Vector formals = (Vector)(prototype.formals().Accept(this));
		varEnv.endScope();
		funcEnv.insert(prototype.name(), new FunctionEntry(result, formals));
		return bt.emptyStatement();
	}


	public Object VisitFunction(ASTFunction function) {
		//CompError.message(function.line(),"VisitFunction: #ele in varEnv = " + varEnv.size():);
		varEnv.beginScope();
		currentOffset = 0;
	    FunctionEntry func = funcEnv.find(function.name());
	    Label startlabel = Label.AbsLabel(function.name());
	    Label endlabel = Label.AbsLabel(function.name() + "@end");
		
		if(func == null) {
			Type functionType = typeEnv.find(function.type());
			if(functionType == null) {
				CompError.message(function.line(),"Type "+function.type()+" is not definded");
			} else {
                functionReturn = functionType;
                functionEnd = endlabel;
			}
			Vector formals = (Vector)(function.formals().Accept(this));
			funcEnv.insert(function.name(), new FunctionEntry(functionType, formals,startlabel,endlabel));
		} else {
			Type functionType = typeEnv.find(function.type());
            if (functionType != func.result()) {
                CompError.message(function.line(), "Return type of " + function.name() + " is not same as prototype");
			} else {
				functionReturn = functionType;
				functionEnd = endlabel;
			}
			Vector formals = null;
			Vector formalsP = func.formals();
            if (function.formals() != null)
                formals = (Vector) function.formals().Accept(this);
            if (formals.size() != formalsP.size())
                CompError.message(function.line(), "Number of parameter of " + function.name() + " is not same as prototype");
            else {
                for (int i = 0; i < formals.size(); i++) {
                    if (formals.elementAt(i) != formalsP.elementAt(i))
                        CompError.message(function.line(), "Type of #" + (i + 1) + " formal parameter is not same as prototype");
                }
            }
		}
		AATStatement body = (AATStatement)(function.body().Accept(this));
       	varEnv.endScope();
        return bt.functionDefinition(body,
                                     currentOffset + 12,
                                     startlabel,
                                     endlabel); }
      
	
	public Object VisitReturnStatement(ASTReturnStatement ret) {
/*	TypeClass returnStatement;
		Label functionend;
				
		if(ret.value() != null){
			TypeClass value = (TypeClass)(ret.value().Accept(this));
			return bt.returnStatement(value.value(), Label.AbsLabel(currentFunctionEnd));
		}
		CompError.message(ret.line(),"ReturnStatement returns null");
		return bt.emptyStatement();
		} */
		
		Type retType = VoidType.instance();
        AATExpression retValue = null;
        if (ret.value() != null) {
            TypeClass retResult = (TypeClass) ret.value().Accept(this);
            retType = retResult.type();
            retValue = retResult.value();
        }
        if (functionReturn != retType)
            CompError.message(ret.line(), "Return type doesn't match");
        return bt.returnStatement(retValue,
                                  functionEnd);
    }


		public Object VisitStatements(ASTStatements statements) { 
			AATStatement res = bt.emptyStatement();
			int i;
			for (i=0; i<statements.size(); i++) {
				res = bt.sequentialStatement(res, (AATStatement)(statements.elementAt(i).Accept(this)));
			}
			return res;
		}

		public Object VisitUnaryOperatorExpression(ASTUnaryOperatorExpression operator) {
			TypeClass operand = (TypeClass)(operator.operand().Accept(this));
			if(operand.type() != BooleanType.instance()) {
				CompError.message(operator.line(),"Operand for unary opertion must be boolean");
			}
			AATExpression returntree = bt.operatorExpression(bt.constantExpression(0), operand.value(), AATOperator.NOT);
			return new TypeClass(BooleanType.instance(), returntree);
		}
		//!<exp>
		//-<exp>

		//int a; int a[];   analyzeExpression: return type+AATExpression.
		public Object VisitInstanceVariableDef(ASTInstanceVariableDef variabledef) {
			String typeName = variabledef.type();
			Type varType = typeEnv.find(variabledef.type());
			Type varFinalType;
			if(varType == null) {
				CompError.message(variabledef.line(),"Type " + typeName + " not found");
				return null;
			} else {
				for(int i = 0;i < variabledef.arraydimension();++i) {
					typeName += "[]";
					varFinalType = typeEnv.find(typeName);
					if(varFinalType == null) {
						varFinalType = new ArrayType(varType);
						typeEnv.insert(typeName, varFinalType);
					}
					varType = varFinalType;
				}
			}
				return null;
		}


		/*	ClassType classtype = (ClassType)(typeEnv.find(currentClassName));
			VariableEnvironment env = classtype.variables();
			env.insert(variabledef.name(), new VariableEntry(varType, currentOffset));
			currentOffset -= MachineDependent.WORDSIZE;
			return bt.emptyStatement();*/
			/*String typeName = variabledef.type();
			Type varType = varEnv.find(typeName);
			if(varType == null) {
			CompError.message(variabledef.line(),"Type " + typeName + " not found");
			}
			for(int i = 0;i < variabledef.arraydimenision();++i) {
			typeName += "[]";
			varType = new ArrayType(varType);
			if(typeEnv.find(typeName) == null)
			typeEnv.insert(typeName, varType);
			}
			typeEnv.find(currentClassName).variables().insert(variabledef.name(),varType);
			return null; */

		// analyzeExpression. retrun type+AATExpression.
		public Object VisitInstanceVariableDefs(ASTInstanceVariableDefs variabledefs) { 
				
			int i;
			for (i=0; i<variabledefs.size(); i++) {
				variabledefs.elementAt(i).Accept(this);
			}
			return null;
		}

			
		public Object VisitVariableExpression(ASTVariableExpression variableexpression) { 
			TypeClass varExp = (TypeClass) variableexpression.variable().Accept(this);
			return varExp;
		}

		public Object VisitWhileStatement(ASTWhileStatement whilestatement) {
			TypeClass test = (TypeClass)whilestatement.test().Accept(this);
			if(!(test.type() instanceof BooleanType)) {
				CompError.message(whilestatement.line(),"while test must be a boolean");
			}
			AATStatement whilebody = (AATStatement)whilestatement.body().Accept(this);
			return bt.whileStatement(test.value(), whilebody);	
			/*Type test = (Type) whilestatement.test().Accept(this);
			if(test != BooleanType.instance()) {
			CompError.message(whilestatement.line(),"while test must be a boolean");
			}
			whilestatement.body().Accept(this);
			return null; */
		}   
		public Object VisitVariableDefStatement(ASTVariableDefStatement variabledef) {
			String typeName = variabledef.type();
			Type varType = typeEnv.find(typeName);
			TypeClass init = null;
			AATStatement res = bt.emptyStatement();
			
			VariableEntry baseEntry = varEnv.find(variabledef.name());
	        if (baseEntry != null) {
	            CompError.message(variabledef.line(), "Variable " + variabledef.name() + " is already declared");
	            return bt.emptyStatement();
	        }
			if(varType == null) {
				CompError.message(variabledef.line(),"Type " + typeName + " not found");
				return bt.emptyStatement();
			}
			for(int i = 0;i < variabledef.arraydimension();++i) {
				typeName += "[]";
				if(typeEnv.find(typeName) == null) {
					typeEnv.insert(typeName, new ArrayType(varType));
				}
				varType = typeEnv.find(typeName);
			}
			varEnv.insert(variabledef.name(), new VariableEntry(varType, currentOffset));
			currentOffset += MachineDependent.WORDSIZE;

			if(variabledef.init() != null) {
				init = (TypeClass)(variabledef.init().Accept(this));
				if(varType != init.type()) {
					CompError.message(variabledef.line(),"Exp type and var type incompetible");
					return bt.emptyStatement();
				}
				 return bt.assignmentStatement(bt.baseVariable(currentOffset - MachineDependent.WORDSIZE),init.value());
			}
			return bt.emptyStatement();
		}	

}
