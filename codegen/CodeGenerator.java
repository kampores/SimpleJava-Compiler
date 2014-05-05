import java.io.*;

class CodeGenerator implements AATVisitor { 
    
    public CodeGenerator(String output_filename) {
	try {
	    output = new PrintWriter(new FileOutputStream(output_filename));
	} catch (IOException e) {
	    System.out.println("Could not open file "+output_filename+" for writing.");
	}
	/*  Feel free to add code here, if you want to */
	EmitSetupCode();
    }
  
    public Object VisitCallExpression(AATCallExpression expression) { 
		// Textbook p236
		int n = expression.actuals.size();
		for(int i=n;i>0;i--) {
			emit("sw "+Register.ACC()+","+"-"+(MachineDependent.WORDSIZE*i-MachineDependent.WORDSIZE)+"("+Register.SP()+")");
		}
		emit("addi "+Register.SP()+","+Register.SP()+","+"-"+MachineDependent.WORDSIZE*n);
		emit("jal "+expression.label().toString());
		emit("addi "+Register.SP()+","+Register.SP()+","+MachineDependent.WORDSIZE*n);
		emit("addi "+Register.ACC()+","+Register.Result()+","+Register.Zero());
		
    }
  
    //This is on textbook.
    public Object VisitMemory(AATMemory expression) { 
		expression.mem().Accept(this);
		emit("lw "+ Register.ACC() + ", 0(" + Register.ACC() + ")");
		return null;
    }
    
    
    public Object VisitOperator(AATOperator expression) { 
		//textbook 235
		expression.left.Accept(this);
		emit("sw "+Register.ACC()+", 0("+Register.ESP()+")");
		emit("addi "+Register.ESP()+","+Register.ESP()+","+0-MachineDependent.WORDSIZE);
	    expression.right.Accept(this);
		emit("lw "+Register.Temp1()+","+MachineDependent.WORDSIZE+"("+Register.ESP()+")");
		emit("addi "+Register.ESP()+","+Register.ESP()+","+MachineDependent.WORDSIZE);
		switch (opexpr.operator()) {
		    case ASTOperatorExpression.PLUS:
				emit("add"+Register.ACC()+","+Register.Temp1()+","+Register.ACC());
				break;
			case ASTOperatorExpression.MINUS:
				emit("sub"+Register.ACC()+","+Register.Temp1()+","+Register.ACC());
				break;
			case ASTOperatorExpression.MULTIPLY:
				//Register Low = new Register("LO");
				emit("mult"+Register.ACC()+","+Register.Temp1());
			    break;
			case ASTOperatorExpression.DIVIDE:
			    emit("div"+Register.ACC()+","+Register.Temp1());
				break;
			case ASTOperatorExpression.OR:
				emit("add "+Register.ACC()+","+Register.Temp1()+","+Register.ACC());
				emit("slt "+Register.ACC()+",0"+","+Register.ACC());  //if 0 < x+y, than return 1,else return 0
				break;
			case ASTOperatorExpression.AND:
				emit("add "+Register.ACC()+","+Register.Temp1()+","+Register.ACC());
				emit("slt "+Register.ACC()+",1"+","+Register.ACC());    //if 1 < x+y, than return 1, else return 0.
				break;
			case ASTOperatorExpression.EQUAL:
				Label truelab = new Label("truelab");
				Label endlab = new Label("endlab");
				emit("beq "+Register.ACC()+","+Register.Temp1()+","+ truelab.toString());
				emit("addi "+Register.ACC()+","+Register.ACC()+",0");  //truelab 
				emit("j "+endlab.toString());   //endLab
				emit(truelab.toString()+ ":");
				emit("addi "+Register.ACC()+","+Register.ACC()+",1");
				emit(endlab.toString()+":");
				break;
			case ASTOperatorExpression.NOT_EQUAL:
			//bne rs, rt, <target>
				Label notture = new Label("nottrue");
				Label endlab = new Label("endlab");
				emit("bne "+Register.ACC()+","+Register.Temp1()+","+notequal.toString());
				emit("addi "+Register.ACC()+","+Register.ACC()+",1");
				emit("j "+endlab.toString());   
				emit(nottrue.toString()+ ":");
				emit("addi "+Register.ACC()+","+Register.ACC()+",0");
				emit(endlab.toString()+":");
				break;
			case ASTOperatorExpression.LESS_THAN:
				emit("slt "+Register.ACC()+","+Register.Temp1()+","+Register.ACC());
				break;
			case ASTOperatorExpression.LESS_THAN_EQUAL:  //x <= y same as x-1 < y
				emit("addi "+Register.Temp1()+","+Register.Temp1()+",-1");  //x-1 x(lhs) is put in Temp1()
				emit("slt "+Register.ACC()+","+Register.Temp1()+","+Register.ACC());
				break;
			case ASTOperatorExpression.GREATER_THAN:
				emit("slt"+Register.ACC()+Register.ACC()+Register.Temp1());  //just opsite to LESS_THAN
			case ASTOperatorExpression.GREATER_THAN_EQUAL: //x >= y same as x > y-1
				emit("addi "+Register.Temp1()+","+Register.Temp1()+",-1");  //x(lhs) is put in Temp1()
				emit("slt "+Register.ACC()+","+Register.ACC()+","+Register.Temp1());   //just opposite to LESS_THAN_EQUAL
				break;
			case ASTOperatorExpression.NOT:   //implement not x as (1-x)
				emit("sub"+Register.ACC()+","+"1"+","+Register.ACC());
				break;
    }

	//Textbook p234 is "$r1"
    public Object VisitRegister(AATRegister expression) { 
		emit("addi "+Register.ACC()+","+expression.register()+"0");
    }
	
    public Object VisitCallExpression(AATCallExpression expression) { 
		// Textbook p236
		int n = expression.actuals.size();
		for(int i=n;i>0;i--) {
			emit("sw "+Register.ACC()+","+"-"+(MachineDependent.WORDSIZE*i-MachineDependent.WORDSIZE)+"("+Register.SP()+")");
		}
		emit("addi "+Register.SP()+","+Register.SP()+","+"-"+MachineDependent.WORDSIZE*n);
		emit("jal "+expression.label().toString());
		emit("addi "+Register.SP()+","+Register.SP()+","+MachineDependent.WORDSIZE*n);
		emit("addi "+Register.ACC()+","+Register.Result()+","+Register.Zero());
		
    }
	
	//Textbook p233
    public Object VisitCallStatement(AATCallStatement statement) {
		int n = statement.actuals.size();
		for(int i=n;i>0;i--) {
			emit("sw "+Register.ACC()+","+"-"+(MachineDependent.WORDSIZE*i-MachineDependent.WORDSIZE)+"("+Register.SP()+")");
		}
		emit("addi "+Register.SP()+","+Register.SP()+","+"-"+MachineDependent.WORDSIZE*n);
		emit("jal "+expression.label().toString());
		emit("addi "+Register.SP()+","+Register.SP()+","+MachineDependent.WORDSIZE*n);
    }

    public Object VisitConditionalJump(AATConditionalJump statement) {
		statement().test().Accept(this);
		emit("bgtz "+Register.ACC()+statement.label().toString());  
    }
    
    public Object VisitEmpty(AATEmpty statement) {
		return null;
    }
    public Object VisitJump(AATJump statement) {
	emit("j " + statement.label().toString());
	return null;
    }
    public Object VisitLabel(AATLabel statement) {
	emit(statement.label().toString() + ":");
	return null;
    }
    public Object VisitMove(AATMove statement) {
		if(statement.lhs() instanceof AATRegister) {
			statement.rhs().Accept(this);
			emit("addi "+((AATRegister)statement.lhs()).register() + ","+Register.ACC() + ",0");
		} else {
			((AATMemory) statement.lhs()).mem().Accept(this);
			emit("sw "+Register.ACC()+","+"0("+Register.ESP()+")");
			emit("addi "+Register.ESP()+","+Register.ESP()+", "+(0-MachineDependent.WORDSIZE));
			statement.rhs().Accept(this);
			emit("lw "+Register.Tmp1()+","+MachineDependent.WORDSIZE+"("+Register.ESP()+")");
			emit("sw"+Register.ACC()+", 0("+Register.Tmp1()+")");
		}
    }
    public Object VisitReturn(AATReturn statement) {
	emit("jr " + Register.ReturnAddr());
	return null;
    }

    public Object VisitHalt(AATHalt halt) {
	/* Don't need to implement halt -- you can leave 
	   this as it is, if you like */
	return null;
    }
	
    public Object VisitSequential(AATSequential statement) {
		statement.left().Accept(this);
		statement.right().Accept(this);
    }
    
    public Object VisitConstant(AATConstant expression) {
		emit("addi "+Register.ACC()+","+Register.Zero()+","+expression.value());
		return null;
    }
    
    private void emit(String assem) {
	assem = assem.trim();
	if (assem.charAt(assem.length()-1) == ':') 
      output.println(assem);
	else
	    output.println("\t" + assem);
    }
    
    public void GenerateLibrary() {
	emit("Print:");
	emit("lw $a0, 4(" + Register.SP() + ")");
	emit("li $v0, 1");
	emit("syscall");
	emit("li $v0,4");
	emit("la $a0, sp");
	emit("syscall");
	emit("jr $ra");
	emit("Println:");
	emit("li $v0,4");
	emit("la $a0, cr");
	emit("syscall");
	emit("jr $ra");
	emit("Read:");
	emit("li $v0,5");
	emit("syscall");
	emit("jr $ra");
	emit("allocate:");
	emit("la " + Register.Tmp1() + ", HEAPPTR");
	emit("lw " + Register.Result() + ",0(" + Register.Tmp1() + ")");
	emit("lw " + Register.Tmp2() + ", 4(" + Register.SP() + ")");
	emit("sub " + Register.Tmp2() + "," + Register.Result() + "," + Register.Tmp2());
	emit("sw " + Register.Tmp2() + ",0(" + Register.Tmp1() + ")");
	emit("jr $ra");
	emit(".data");
	emit("cr:");
	emit(".asciiz \"\\n\"");
	emit("sp:");
	emit(".asciiz \" \"");
        emit("HEAPPTR:");
	emit(".word 0");
	output.flush();
    }
    
    private void EmitSetupCode() {
	emit(".globl main");
	emit("main:");
	emit("addi " + Register.ESP() + "," + Register.SP() + ",0");
	emit("addi " + Register.SP() + "," + Register.SP() + "," + 
	     - MachineDependent.WORDSIZE * STACKSIZE);
	emit("addi " + Register.Tmp1() + "," + Register.SP() + ",0");
	emit("addi " + Register.Tmp1() + "," + Register.Tmp1() + "," + 
	     - MachineDependent.WORDSIZE * STACKSIZE);
	emit("la " + Register.Tmp2() + ", HEAPPTR");
	emit("sw " + Register.Tmp1() + ",0(" + Register.Tmp2() + ")");
        emit("sw " + Register.ReturnAddr() + "," + MachineDependent.WORDSIZE  + "("+ Register.SP() + ")"); 
 	emit("jal main1");
	emit("li $v0, 10");
        emit("syscall");
    }
    
    private final int STACKSIZE = 1000;
    private PrintWriter output;
    /* Feel Free to add more instance variables, if you like */
}

