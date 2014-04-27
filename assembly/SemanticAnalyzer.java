import java.util.Vector;

public class SemanticAnalyzer implements ASTVisitor {
	
	AATBuildTree bt;
	private VariableEnvironment varEnv;
	private FunctionEnvironment funcEnv;
	private TypeEnvironment typeEnv;
	
	private String currentClassName;
	
	private int currentOffset;
	private String currentFunctionEnd;
	
	public SemanticAnalyzer() {
		varEnv = new VariableEnvironment();
		funcEnv = new FunctionEnvironment();
		funcEnv.addBuiltinFunctions();
		typeEnv = new TypeEnvironment();
		currentClassName = "";
		bt = new AATBuildTree();
		currentOffset = 0;
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
		}
		TypeClass index = (TypeClass)(array.index().Accept(this));
		Type indextype = index.type();
		if(indextype != IntegerType.instance()){
			CompError.message(array.line(),"array type is not integer");
			return BooleanType.instance();
		} else {
			return new TypeClass((ArrayType)basetype, bt.arrayVariable(base.value(), index.value(), MachineDependent.WORDSIZE));
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
		if(typeEnv.find(astclass.name()) != null) {
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
		return bt.emptyStatement();
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
		return IntegerType.instance();
		}
		VariableEntry classVarEntry = ((ClassType)varType).variables().find(classvariable.variable());
		if(classVarEntry == null) {
		CompError.message(classvariable.line(), classvariable.variable() + " is not a member of the class variable");
		return IntegerType.instance();
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
			return IntegerType.instance();
		}
		for(int i=0; i < formal.arraydimension(); i++){
			typeName += "[]";
			if(typeEnv.find(typeName) == null) {
				type = new ArrayType(type);
				if(typeEnv.find(typeName) == null)
					typeEnv.insert(typeName, type);
			}
		}
		
		varEnv.insert(formal.name(), new VariableEntry(type, currentOffset));
		currentOffset += MachineDependent.WORDSIZE;
		
		return type;
	}
    
	public Object VisitFormals(ASTFormals formals) { 
		Vector types = new Vector();
		for (int i=0; i<formals.size(); i++) {
			types.addElement(formals.elementAt(i).Accept(this));
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
				
		//return IntegerType.instance();
		return new TypeClass(IntegerType.instance(), bt.constantExpression(literal.value()));
	}

	public Object VisitNewArrayExpression(ASTNewArrayExpression newarray) {
		//check the type has been declared
		String typeName = "";
		Type type = typeEnv.find(typeName);
		if(type == null ){
			CompError.message(newarray.line(),"type not declared");
			return IntegerType.instance();
		}
		int i;
		for(i=0; i<newarray.arraydimension();i++){
			typeName = typeName+"[]";
			type = new ArrayType(type);
			if(typeEnv.find(typeName) == null)
				typeEnv.insert(typeName, type);
		}
		TypeClass elements = (TypeClass)(newarray.elements().Accept(this));
		if(elements.type() != IntegerType.instance()) {
			CompError.message(newarray.line(), "Array elements must be an integer");
			return IntegerType.instance();
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
			return IntegerType.instance();
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
		Vector formals = (Vector)(prototype.formals().Accept(this));
		funcEnv.insert(prototype.name(), new FunctionEntry(result, formals));
		return bt.emptyStatement();
	}

	public Object VisitFunction(ASTFunction function) {
		Type result = typeEnv.find(function.type());
		if(result == null) {
			CompError.message(function.line(),"Type "+function.type()+" is not definded");
		}
		
		varEnv.beginScope();
		currentOffset = MachineDependent.WORDSIZE;
		Vector formals = (Vector)(function.formals().Accept(this));
		
		currentOffset = 0;
		currentFunctionEnd = function.name() + "$end";
		AATStatement body = (AATStatement)(function.body().Accept(this));
		varEnv.endScope();
				
		funcEnv.insert(function.name(), new FunctionEntry(result, formals));
		
		String label = function.name();
		if(label.equals("main"))
			label = label + "1";
		
		return bt.functionDefinition(body, 
			function.formals().size() * MachineDependent.WORDSIZE, 
				Label.AbsLabel(label),
				Label.AbsLabel(currentFunctionEnd));
	}
  
	public Object VisitReturnStatement(ASTReturnStatement ret) {
		TypeClass returnStatement;
		Label functionend;
				
		if(ret.value() != null){
			TypeClass value = (TypeClass)(ret.value().Accept(this));
			return bt.returnStatement(value.value(), Label.AbsLabel(currentFunctionEnd));
		}
		CompError.message(ret.line(),"ReturnStatement returns null");
		return bt.emptyStatement();
		/** if (ret.value() != null){
			Type type = ret.value().Accept(this);
			return type;
			}
			CompError.message(ret.line(),"ReturnStatement returns null");	
			return null;*/	
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
			return new TypeClass(BooleanType.instance(), operand.value());
		}
		//!<exp>
		//-<exp>

		//int a; int a[];   analyzeExpression: return type+AATExpression.
		public Object VisitInstanceVariableDef(ASTInstanceVariableDef variabledef) {
			String typeName = variabledef.type();
			Type varType = varEnv.find(typeName).type();
			if(varType == null) {
				CompError.message(variabledef.line(),"Type " + typeName + " not found");
			}
			for(int i = 0;i < variabledef.arraydimension();++i) {
				typeName += "[]";
				varType = new ArrayType(varType);
				if(typeEnv.find(typeName) == null)
					typeEnv.insert(typeName, varType);
			}
			ClassType classtype = (ClassType)(typeEnv.find(currentClassName));
			VariableEnvironment env = classtype.variables();
			env.insert(variabledef.name(), new VariableEntry(varType, currentOffset));
			currentOffset -= MachineDependent.WORDSIZE;
			return bt.emptyStatement();
				
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
		}

		// analyzeExpression. retrun type+AATExpression.
		public Object VisitInstanceVariableDefs(ASTInstanceVariableDefs variabledefs) { 
				
			int i;
			for (i=0; i<variabledefs.size(); i++) {
				variabledefs.elementAt(i).Accept(this);
			}
			return bt.emptyStatement();
		}

			
		public Object VisitVariableExpression(ASTVariableExpression variableexpression) { 
			return variableexpression.variable().Accept(this);
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
			if(varType == null) {
				CompError.message(variabledef.line(),"Type " + typeName + " not found");
			}
			for(int i = 0;i < variabledef.arraydimension();++i) {
				typeName += "[]";
				varType = new ArrayType(varType);
				if(typeEnv.find(typeName) == null)
					typeEnv.insert(typeName, varType);
			}
			if(variabledef.init() != null) {
				init = (TypeClass)(variabledef.init().Accept(this));
				if(varType != init.type()) {
					CompError.message(variabledef.line(),"Exp type and var type incompetible");
				}
			}
			varEnv.insert(variabledef.name(), new VariableEntry(varType, currentOffset));
			if(init != null)
				res = bt.assignmentStatement(bt.baseVariable(currentOffset), init.value());
			currentOffset -= MachineDependent.WORDSIZE;
			return res;
		}	
	}
