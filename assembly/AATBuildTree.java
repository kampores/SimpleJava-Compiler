import java.util.Vector;

public class AATBuildTree {
  
    public AATStatement functionDefinition(AATStatement body, int framesize, Label start,  
					   Label end) {
	
						//   <Label for start of function>
						//  <Code to set up activation record>
						//  <function body>
						//  <label for end of function>
						//  <code to clean up activation record>
						//  <return>
		
		AATStatement storeSP = new AATMove(
			new AATMemory(
				new AATOperator(
					new AATRegister(Register.SP()),
					new AATConstant(framesize),
					AATOperator.MINUS)),
			new AATRegister(Register.SP()));
		
		AATStatement storeFP = new AATMove(
			new AATMemory(
				new AATOperator(
					new AATRegister(Register.SP()),
					new AATOperator(
						new AATConstant(framesize), 
						new AATOperator(
							new AATConstant(1), 
							new AATConstant(MachineDependent.WORDSIZE), 
							AATOperator.MULTIPLY), 
						AATOperator.PLUS),
					AATOperator.MINUS)),
			new AATRegister(Register.FP()));
		
		AATStatement storeRA = new AATMove(
			new AATMemory(
				new AATOperator(
					new AATRegister(Register.SP()),
					new AATOperator(
						new AATConstant(framesize), 
						new AATOperator(
							new AATConstant(2), 
							new AATConstant(MachineDependent.WORDSIZE), 
							AATOperator.MULTIPLY), 
						AATOperator.PLUS),
					AATOperator.MINUS)),
			new AATRegister(Register.ReturnAddr()));
		
		AATStatement updateFP = new AATMove(
			new AATRegister(Register.FP()),
			new AATRegister(Register.SP()));
		
		AATStatement updateSP = new AATMove(
			new AATRegister(Register.SP()),
			new AATOperator(
				new AATRegister(Register.FP()),
				new AATOperator(
					new AATConstant(framesize),
					new AATOperator(
						new AATConstant(3),
						new AATConstant(MachineDependent.WORDSIZE),
						AATOperator.MULTIPLY),
					AATOperator.PLUS),
				AATOperator.MINUS));
		
		AATStatement restoreRA = new AATMove(
			new AATRegister(Register.ReturnAddr()),
			new AATMemory(
				new AATOperator(
					new AATRegister(Register.SP()),
					new AATOperator(
						new AATConstant(1),
						new AATConstant(MachineDependent.WORDSIZE),
						AATOperator.MULTIPLY),
					AATOperator.PLUS)));
		
		AATStatement restoreFP = new AATMove(
			new AATRegister(Register.FP()),
			new AATMemory(
				new AATOperator(
					new AATRegister(Register.SP()),
					new AATOperator(
						new AATConstant(2),
						new AATConstant(MachineDependent.WORDSIZE),
						AATOperator.MULTIPLY),
					AATOperator.PLUS)));
		
		AATStatement restoreSP = new AATMove(
			new AATRegister(Register.SP()),
			new AATMemory(
				new AATOperator(
					new AATRegister(Register.SP()),
					new AATOperator(
						new AATConstant(3),
						new AATConstant(MachineDependent.WORDSIZE),
						AATOperator.MULTIPLY),
					AATOperator.PLUS)));
		
		AATStatement setup = 
			new AATSequential(storeSP, 
			new AATSequential(storeFP, 
			new AATSequential(storeRA, 
			new AATSequential(updateFP, updateSP))));
		AATStatement cleanup = 
			new AATSequential(restoreRA, 
			new AATSequential(restoreFP, restoreSP));
		
		AATStatement tree = 
			new AATSequential(new AATLabel(start), 
			new AATSequential(setup, 
			new AATSequential(body, 
			new AATSequential(new AATLabel(end), 
			new AATSequential(cleanup, new AATReturn())))));

		return tree;
	}
    
    public AATStatement ifStatement(AATExpression test, AATStatement ifbody, AATStatement elsebody) {
		Label iftrue = new Label("iftrue");
		Label ifend = new Label("ifend");
		
		AATStatement tree = 
			new AATSequential(new AATConditionalJump(test, iftrue),
			new AATSequential(elsebody,
			new AATSequential(new AATJump(iftrue),
			new AATSequential(new AATLabel(iftrue),
			new AATSequential(ifbody, new AATLabel(ifend))))));
			
		return tree;
    }
    
    public AATExpression allocate(AATExpression size) {
		//TODO AATCallExpression(allocateLabel,)
		//for NewArrayExpression, NewClassExpression
		Vector actuals = new Vector(1);
		actuals.addElement(size);
		return new AATCallExpression(Label.AbsLabel("allocate"), actuals);
  
    }

    public AATStatement whileStatement(AATExpression test, AATStatement whilebody) {
		Label whiletest = new Label("whiletest");
		Label whilestart = new Label("whilestart");
		
		AATStatement tree = 
			new AATSequential(new AATJump(whiletest),
			new AATSequential(new AATLabel(whilestart),
			new AATSequential(whilebody,
			new AATSequential(new AATLabel(whiletest), new AATConditionalJump(test, whilestart)))));
		
		return tree;
		
    }

    public AATStatement dowhileStatement(AATExpression test, AATStatement dowhilebody) {
		Label dowhilestart = new Label("dowwhilestart");
		
		AATStatement tree = 
			new AATSequential(new AATLabel(dowhilestart),
			new AATSequential(dowhilebody, new AATConditionalJump(test, dowhilestart)));

		return tree;
    }
	
    public AATStatement forStatement(AATStatement init, AATExpression test, 
				     AATStatement increment, AATStatement body) {
		Label fortest = new Label("fortest");
		Label forstart = new Label("forstart");
	
		AATSequential tree = 
			new AATSequential(init,
			new AATSequential(new AATJump(fortest),
			new AATSequential(new AATLabel(forstart),
			new AATSequential(body,
			new AATSequential(increment,
			new AATSequential(new AATLabel(fortest), new AATConditionalJump(test, forstart)))))));
	
		return tree;
    }
    
    public AATStatement emptyStatement() {
		return new AATEmpty();
    }
  
    public AATStatement callStatement(Vector actuals, Label name) {
		return new AATCallStatement(name, actuals);
    }
    
    public AATStatement assignmentStatement(AATExpression lhs,
					    AATExpression rhs) {
    	return new AATMove(lhs,rhs);								
    }
    
    public AATStatement sequentialStatement(AATStatement first,
					    AATStatement second) {
		return new AATSequential(first, second);
    }
    
    public AATExpression baseVariable(int offset) {
		return new AATMemory(
			new AATOperator(
				new AATRegister(Register.FP()),
				new AATConstant(offset),
				AATOperator.MINUS));
    }

    public AATExpression arrayVariable(AATExpression base, AATExpression index, int elementSize) {
		return new AATMemory(
			new AATOperator(
				base,
				new AATOperator(
					index,
					new AATConstant(elementSize),
					AATOperator.MULTIPLY),
				AATOperator.MINUS));
    }
    
	//TODO how to deal with S.A[3]  in slides 07-69
    public AATExpression classVariable(AATExpression base, int offset) {
		return new AATMemory(
			new AATOperator(
				base,
				new AATConstant(offset),
				AATOperator.MINUS));
	
    }
  
    public AATExpression constantExpression(int value) {
		return new AATConstant(value);
    }
  
    public AATExpression operatorExpression(AATExpression left,
					    AATExpression right,
					    int operator) {
		return new AATOperator(
			left,
			right,
			operator);
    }
  
    public AATExpression callExpression(Vector actuals, Label name) {
		return new AATCallExpression(name, actuals);
    }
    
    public AATStatement returnStatement(AATExpression value, Label functionend) {
	//first use AATMove to copy the value to register. then AATJump to functionend.
	// copy the value to be returned to the result register, and then jump to the end of the function.
		return new AATSequential(
			new AATMove(new AATRegister(Register.Result()), value),
			new AATJump(functionend));
    }
}








