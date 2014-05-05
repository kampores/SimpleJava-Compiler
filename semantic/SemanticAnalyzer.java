import java.util.Vector;

public class SemanticAnalyzer implements ASTVisitor {
	private VariableEnvironment varEnv;
	private FunctionEnvironment funcEnv;
	private TypeEnvironment typeEnv;
	
	private String currentClassName;
	
	public SemanticAnalyzer() {
		varEnv = new VariableEnvironment();
		funcEvn = new FunctionEnvironment();
		funcEnv.addBuiltinFunctions();
		typeEnv = new TypeEnvironment();
		currentClassName = "";
	}
	
	private void beginClassScope(String name) {
		currentClassName = name;
	}
	
	private void endClassScope(String name) {
		currentClassName = "";
	}
	
	public Object VisitArrayVariable(ASTArrayVariable array) {
		// check the type is array type.
		Type baseType = (Type) array.base().Accept(this);
		if(! baseType instanceof ArrayType)
		{
			ComError.message(array.line(),"array type is not array");
		}
		Type indexType = (Type)(array.index().Accept(this));
		if(indexType != IntegerType.instance()){
			ComError.message(array.line(),"array type is not integer");
			return BooleanType.instance();
		} else {
			return (ArrayType)(baseType).type();
		}
	}

	public Object VisitAssignmentStatement(ASTAssignmentStatement assign) {
		Type variableType = (Type) assign.variable().Accept(this);
		Type valueType = (Type) assign.value().Accept(this);
		if(variableType != valueType) {
			ComError.message(assign.lint(),"Type of var is not the same as exp");
		}
		return null;
	}

	public Object VisitBaseVariable(ASTBaseVariable base) {
		VariableEntry baseEntry = variableEnv.find(base.name());
		if(baseEntry == null) {
			Comerror.message(base.line(), "Variable "+base.name() + " is not defined in this scope");
			return IntegerType.instance();
		}else {
			return baseEntry.type();
		}
	}

	public Object VisitBooleanLiteral(ASTBooleanLiteral boolliteral) {
		return BooleanType.instance();
	}
	
	public Object VisitClass(ASTClass classs) {	
		if(typeEnv.find(classs.name()) != null) {
			Comerror.message(classs.line(), "Class "+ classs.name() + " is already defined in this scope");
			return null;
		}
		ClassType type = new ClassType(new VariableEnvironment());
		typeEnv.insert(classs.name(), type);
			beginClassScope(classs.name());
		if (classs.variabledefs() != null){
			classs.variabledefs().Accept(this);
		}
		endClassScope(classs.name());
		return null;
	}

	public Object VisitClasses(ASTClasses classes) { 
		for (int i=0; i<classes.size();i++) 
			classes.elementAt(i).Accept(this);
		return null;
	}

	// check the classvariable in variableEnvironment
	public Object VisitClassVariable(ASTClassVariable classvariable) {
		Type varType = (Type)classvariable.base().Accept(this);
		if(!(varType instanceof ClassType)) {
			Comerror.message(classvariable.line(), "Not a vaild class variable");
			return IntegerType.instance();
		}
		VariableEntry classVarEntry = ((ClassType)varType).variables().find(classvariable.variable());
		if(classVarEntry == null) {
			Comerror.message(classvariable.line(), classvariable.variable() + " is not a member of the class variable");
			return IntegerType.instance();
		}
		else {
			return classVarEntry.type();
		}
	}

	public Object VisitDoWhileStatement(ASTDoWhileStatement dowhile) {
		Type test = (Type)dowhile.test().Accept(this);
		if (!(test instanceof BooleanType)) {
			CompError.message(dowhile.line(),"While test must be a boolean");
		}
		dowhile.body().Accept(this);
		return null;
	}

	public Object VisitEmptyStatement(ASTEmptyStatement empty) {
		return null;
	}
    
	public Object VisitForStatement(ASTForStatement forstmt) {
		Type test = (Type) forstmt.test().Accept(this);
		if (test != BooleanType.instance()) {
			CompError.message(forstmt.line(),"ForStatement's test must be a boolean");
		}
		
		if (!(forstmt.initialize() instanceof ASTAssignmentStatement)) {
			CompError.message(forstmt.line(), "ForStatement's init must be assignmentStatement.") ;
		}
		forstmt.initialize().Accept(this);
		
		if (!(forstmt.increment() instanceof ASTAssignmentStatement)) {
			CompError.message(forstmt.line(), "ForStatement's increment must be assignmentStatement.") ;
		}
		forstmt.increment().Accept(this);
		
		varEnv.beginScope();
		forstmt.body().Accept(this);
		varEnv.endScope();
		return null;			
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
		
		return typeEnv.find(typeName);
	}
    
		public Object VisitFormals(ASTFormals formals) { 
			Vector types = new Vector();
			if(formals != null && formals.size() >0){
			for (int i=0; i<formals.size(); i++) {
				types.addElement(formals.elementAt(i).Accept(this));
			}
			return types;
		}
		}
    
		public Object VisitFunctionDefinitions(ASTFunctionDefinitions functiondefs) { 
			for (int i=0; i < functiondefs.size(); i++)
				// since functionDefinition is superclass of ASTPrototype and ASTFunction.
				functiondefs.elementAt(i).Accept(this);
			return null;
		}

		public Object VisitFunctionCallExpression(ASTFunctionCallExpression functioncall) { 
			//Function Calls:must be declared before they are called,the nums and types of parameter must match the declaration. type of a function call is also determined by its declaraion.
			FunctionEntry functionEntry = funcEnv.find(functioncall.name());
			if(functionEntry == null){
				CompError.message(functioncall.line(),"Function "+functioncall.name()+" is not declared");
				return IntegerType.instance();
			} else {
				for (int i=0; i<functioncall.size(); i++) {
					Type inType = (Type)(functioncall.elementAt(i).Accept(this));
					Type argumentType = (Type)(functionEntry.formals().elementAt(i));
					if(!inType == argumentType) {
						CompError.message(functioncall.line(), "Wrong argument type");
					}
				}
				return functionEntry.result();
			}
		}
    

			public Object VisitFunctionCallStatement(ASTFunctionCallStatement functioncall) {
				//books p188
				FunctionEntry functionEntry = funcEnv.find(functioncall.name());
				if(functionEntry == null){
					CompError.message(functioncall.line(),"Function "+functioncall.name()+" is not declared");
					return null;
				} else {
					if(functioncall.size() == functionEntry.formals().size()) {
						for (int i=0; i<functioncall.size(); i++) {
							Type inType = (Type)(functioncall.elementAt(i).Accept(this));
							Type argumentType = (Type)(functionEntry.formals().elementAt(i));
							if(!inType == argumentType) {
								CompError.message(functioncall.line(), "Wrong argument type");
							}
						}
					} else {
						ComError.message(functioncall.line(), "Function "+functioncall.name()+" needs "
							+functionEntry.formals().size()+" formals");
					}
				}
						return null;
			}
	

			public Object VisitIfStatement(ASTIfStatement ifstmt) {
				Print("If (test/if body/else body)");
				Type test = (Type) ifstmt.test().Accept(this);
				if(test != BooleanType.instance()) {
					CompError.message(ifstmt.line(), "If test must be a boolean");
				}
				ifstmt.thenstatement().Accept(this);
				if(ifstmt.elsestatement() != null) {
					ifstmt.elsestatement().Accept(this);
				}
				return null;
			}

			public Object VisitIntegerLiteral(ASTIntegerLiteral literal) {
				return IntegerType.instance();
			}

			public Object VisitNewArrayExpression(ASTNewArrayExpression newarray) {
				Type test = (Type) ifstmt.test().Accept(this);
				if(test != BooleanType.instance()) {
					CompError.message(ifstmt.line(), "If test must be a boolean");
					return IntegerType.instance();
				}
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
				if(ifstmt.elements().Accept(this) != IntegerType.instance()) {
					CompError.message(ifstmt.line(), "Array elements must be an integer");
					return IntegerType.instance();
				}
				return type;
			}

			public Object VisitNewClassExpression(ASTNewClassExpression newclass) {
				Type type = typeEnv.find(newclass.type()) 
				if(type == null ){
					CompError.message(newclass.line(),"Class not declared");
					return IntegerType.instance();
				}
				return type;
			}

			public Object VisitOperatorExpression(ASTOperatorExpression opexpr) {
				//TODO how to check the type of expression of of OperatorExpression????
				//+,-,*,/, expression of left() and right() should be int, type of expression is int.
				// &, ||, expression of left() and right() should be boolean, type of expression is boolean.
				// >,>=, < , <=, != , type of left() and right() should be int, type of expression is boolean.
				Type lefttype = (Type)opexpr.left().Accept(this);
				Type righttype = (Type)opexpr.right().Accept(this);

				if((opexpr.operator() >=1) && (opexpr.operator() <=4) {
					if((lefttype != IntegerType.instance()) || (righttype != IntegerType.instance())) {
						CompError.message(opexpr.line(),"wrong type ");
					}
					return IntegerType.instance();
				}
				else if ((opexpr.operator() >= 5)&&(opexpr.operator() <=6)) {
					if((lefttype != BooleanType.instance()) ||(righttype != BooleanType.instance())){
						CompError.message(opexpr.line(),"wrong type ");
					}
					return BooleanType.instance();
				}
				else if ((opexpr.operator() >= 7)&&(opexpr.operator() <=12)) {
					if((lefttype != IntegerType.instance()) || (righttype != IntegerType.instance())) {
						CompError.message(opexpr.line(), "wrong type ");
					}
					return BooleanType.instance();
				}
			}


			public Object VisitProgram(ASTProgram program) { 
				if (program.classes() != null) 
					program.classes().Accept(this);
				if (program.functiondefinitions() != null)
					program.functiondefinitions().Accept(this);
				return null;
			}
  
			public Object VisitPrototype(ASTPrototype prototype) {	
				Type result = typeEnv.find(prototype.type());
				if(result == null) {
					CompError.message(protyotype.line(),"Type "+prototype.type()+" is not definded");
				}
				Vector formals = (Vector)(prototype.formals().Accept(this));
				funcEnv.insert(prototype.name(), new FunctionEntry(result, formals);
				return null;
			}

			public Object VisitFunction(ASTFunction function) {
				Type result = typeEnv.find(function.type());
				if(result == null) {
					CompError.message(function.line(),"Type "+function.type()+" is not definded");
				}
				Vector formals = (Vector)(function.formals().Accept(this));
				
				varEnv.beginScope();
				function.body.Accept(this);
				varEnv.endScope();
				
				funcEnv.insert(function.name(), new FunctionEntry(result, formals);
				return null;
			}
  
			public Object VisitReturnStatement(ASTReturnStatement ret) {
				if (ret.value() != null){
					 Type type = ret.value().Accept(this);
					 return type;
				}
				ComError.message(ret.line(),"ReturnStatement returns null");	
				return null;
			}

			public Object VisitStatements(ASTStatements statements) { 
				int i;
				for (i=0; i<statements.size(); i++)
					statements.elementAt(i).Accept(this);
				return null;
			}

			public Object VisitUnaryOperatorExpression(ASTUnaryOperatorExpression operator) {
				operator.operand().Accept(this);
				Type operatorType = (Type)operator.operand().Accept(this);
				if(operatorType != BooleanType.instance()) {
					ComError.message(ret.line(),"Operand for unary opertion must be boolean");
				}
				return BooleanType.instance();
			}
			//!<exp>
			//-<exp>

			//int a; int a[];
			//int a[][];
			//varType = IntegerType
			//varType = ArrayType(ArrayType(IntegerType));
			//int b[];
			//int a[];
			//new ArrayType(IntegerType);
			//new MyClasss()
			//Singleton pattern
			//"a" == "a"
			public Object VisitInstanceVariableDef(VisitInstanceVariableDef variabledef) {
				String typeName = variabledef.type();
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
				return null;
			}


			public Object VisitInstanceVariableDefs(ASTInstanceVariableDefs variabledefs) { 
				int i;
				for (i=0; i<variabledefs.size(); i++) {
					variabledefs.elementAt(i).Accept(this);
				}
				return null;
			}

			public Object VisitVariableExpression(ASTVariableExpression variableexpression) { 
				return variableexpression.variable().Accept(this);
			}

			public Object VisitWhileStatement(ASTWhileStatement whilestatement) {
				Type test = (Type) whilestatement.test().Accept(this);
				if(test != BooleanType.instance()) {
					CompError.message(whilestatement.line(),"while test must be a boolean");
				}
				whilestatement.body().Accept(this);
				return null;
			}   
			public Object VisitVariableDefStatement(ASTVariableDefStatement variabledef) {
				String typeName = variabledef.type();
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
				if(variabledef.init() != null) {
					Type expType = variabledef.init().Accept(this);
					if(varType != expType) {
						CompError.message(variabledef.line(),"Exp type and var type incompetible");
					}
				}
				varEnv.insert(variabledef.name(), varType);
				return null;
			}
		}	
	}	
}
