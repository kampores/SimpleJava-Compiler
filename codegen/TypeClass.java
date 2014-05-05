class TypeClass {
	private Type type_;
	private AATExpression value_;
	
	public TypeClass(Type type,AATExpression value) {
		type_ = type;
		value_ = value;
	}
	public Type type() {
		return type_;
	}
	
	public AATExpression value() {
		return value_;
	}
	
	public void setvalue(AATExpression value) {
		value_ = value;
	}
	public void settype(Type type){
		type_ = type;
	}
	
}
