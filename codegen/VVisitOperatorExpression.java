public Object VisitOperatorExpression(ASTOperatorExpression opexpr) {
	TypeClass left;
	TypeClass right;
	AATExpression returntree;
	
	left = (TypeClass)opexpr.left().Accept(this);
	right = (TypeClass)opexpr.right().Accept(this);
	Type lefttype = left.type();
	Type righttype = right.type();

    switch (opexpr.operator()) {
    case ASTOperatorExpression.PLUS:
    case ASTOperatorExpression.MINUS:
    case ASTOperatorExpression.MULTIPLY:
    case ASTOperatorExpression.DIVIDE:
		if((lefttype != IntegerType.instance()) || (righttype != IntegerType.instance())) {
			CompError.message(opexpr.line(),"Integer type can be used in operator: PLUS, MINUS, MULTIPLY or DIVIDE.");
		}
		returntree = bt.operatorExpression(left.tree(),right.tree(),opexpr.operator());
		return new TypeClass(IntegerType.instance(),returntree);
		break;
		
     case ASTOperatorExpression.AND:
     case ASTOperatorExpression.OR:
		if((lefttype != BooleanType.instance()) || (righttype != BooleanType.instance())) {
			CompError.message(opexpr.line(),"Only Boolean type can be used in operator: AND or OR!");
		}
		returntree = bt.operatorExpression(left.tree(),right.tree(),opexpr.operator());
		return new TypeClass(IntegerType.instance(),returntree);
		break;
		
     case ASTOperatorExpression.EQUAL:
     case ASTOperatorExpression.GREATER_THAN:
     case ASTOperatorExpression.LESS_THAN:
     case ASTOperatorExpression.GREATER_THAN_EQUAL:
     case ASTOperatorExpression.LESS_THAN_EQUAL:
     case ASTOperatorExpression.NOT_EQUAL:
		if((lefttype != IntegerType.instance()) || (righttype != IntegerType.instance())) {
			CompError.message(opexpr.line(),"Only Boolean type can be used in operator: AND or OR!");
		}
		returntree = bt.operatorExpression(left.tree(),right.tree(),opexpr.operator());
		return new TypeClass(IntegerType.instance(),returntree);
		break;
	}
}
