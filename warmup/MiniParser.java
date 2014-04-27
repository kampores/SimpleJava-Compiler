import java.util.regex.Pattern;

/**Use testStatement method to check if input string is valid or not. */
public class MiniParser {
	private static final String s = null;
	public static boolean testIdentifer(String s){
		String regex = "(?!(while|do|true|false))[a-z]+";
		return Pattern.matches(regex, s);
	}

	/* an<expression> is either an identifier token, the keyword true,false or (<expression>). such as z=((z)); */
	public static boolean testExpression(String s){
		String regex = "(?!(while|do))[a-z]+";
		if(Pattern.matches(regex, s)) {
			return true;
		}
		else if((s.startsWith("(")) && (s.endsWith(")"))){
			String newS = s.substring(1, s.length()-1);
			return testExpression(newS);
		}
		return false;
	}

	public static boolean testAssignmentStatement(String str){
		String s = str.replaceAll("\\s","");
		//		System.out.println("----"+s+"----");
		if(!(s.endsWith(";"))) {
			return false;
		}
		if(!(s.contains("="))) {
			return false;
		}
		int partition = s.indexOf("=");

		String s1 = s.substring(0, partition);
//		System.out.println("s1 is "+s1);
		String s2 = s.substring(partition+1, s.length()-1);
//		System.out.println("substring of Assignment is"+s2);
		return (testIdentifer(s1) && testExpression(s2));
	}

	public static boolean testDoWhileStatement(String str){
		if(str.isEmpty()) {
			return false;
		}
		String s1 = str.replaceAll("\t", " ").replaceAll(" +", " ").replaceAll("\n", " ");
		//delete spaces before "do"
		int i=0;
		while(i < s1.length() && (s1.charAt(i) == (' ')|| s1.charAt(i) == '\t' || s1.charAt(i) == '\n')) {
			i++;		
		}
		String s = s1.substring(i);
		//		System.out.println("do-while is "+s);

		if(!(s.startsWith("do")) || (!(s.contains("while")))) {
			return false;
		}
		// find out if there is more than one pair of do-while

		int lastwhile = s.lastIndexOf("while");

		String substring = s.substring(3,lastwhile-1);
		int lastcomma = s.lastIndexOf(";");
		//		System.out.println(s.substring((lastwhile)+6,lastcomma));
		//		System.out.println(substring);
		//		System.out.println("test if substring is assignment "+testAssignmentStatement(substring));
		//		System.out.println("----------------");
		if(lastcomma < lastwhile) {
			return false;
		}
		return (testExpression(s.substring((lastwhile)+6,lastcomma)) && (testStatement(substring)));

	}

	public static boolean testStatement(String s) {
		return (testDoWhileStatement(s) || testAssignmentStatement(s) || testWhileStatement(s) || testBlockStatement(s));
	}

	public static boolean testWhileStatement(String str){
		String s1 = str.replaceAll("\t", " ").replaceAll("\n", " ").replace(" +", " ");
		//delete spaces before "while"
		int i=0;
		while(i < s1.length() && (s1.charAt(i) == (' ')|| s1.charAt(i) == '\t' || s1.charAt(i) == '\n')) {
			i++;		
		}
		String s = s1.substring(i);
		if(!s.startsWith("while")) return false;

		int whilestart = s.indexOf("while");
		if((whilestart+5 < s.length()) &&(s.charAt(whilestart+5) !=' ')){

			return false;
		}

		for(int j = whilestart+6;j<str.length(); j++) {
			//			System.out.println(s.substring(whilestart+6,j));
			if(testExpression(s.substring(whilestart+6,j))) {
				//				System.out.println(s.substring(whilestart+6,j));
				return testStatement(s.substring(j));
			}
		}

		return false;
	}

	public static boolean testBlockStatement(String st) {
		String str = st.replaceAll("\\s","");
		//delete spaces before "{"
		int i=0;
		while(i < str.length() && (str.charAt(i) == (' ')|| str.charAt(i) == '\t' || str.charAt(i) == '\n')) {
			i++;		
		}
		String s = str.substring(i);
		if(!s.startsWith("{") || (!s.endsWith("}"))) {
			return false;
		}

		String substring = s.substring(1,s.length()-1);  //cut two braces.
		// test any ended with ";" or "}" is valid statement.
		return (testOneOrMoreStatements(substring));
	}

	// test if there is one or more simpleLogic statements. 
	public static boolean testOneOrMoreStatements(String str){
		if(str=="") {
			return true;
		}
		for(int i =0; i<str.length(); i++) {
			//the first valid statement,then recursively check the rest of string is valid statement.
			if(testStatement(str.substring(0,i)) == true) {
				//				System.out.println("testOneOrMoreStatement "+str.substring(0,i));
				return testOneOrMoreStatements(str.substring(i));
			}
		}
		return testStatement(str);
	}


	public static void main(String[] args) {

		//Input a file and then build a string for testing. 	
//		BufferedReader in = null;
//		try {
//			in = new BufferedReader(new FileReader(args[0]));
//		} catch (FileNotFoundException e1) {
//			System.out.println(e1.toString());
//		}
//
//		StringBuilder sb = new StringBuilder();
//		String s = "";
//		try {
//			while((s = in.readLine()) != null) {
//				sb.append(s);
//			}
//		} catch (IOException e) {
//			System.out.println(e.toString());
//		}
//		s = sb.toString();
		
		
		String s = " {{x = true; } }";
		if((testStatement(s)) == true) {
			System.out.println("Valid program. ");
		}
		else 
			System.out.println("Invalid program.");

		//			Boolean a = testStatement(java.io.FileInputStream(args[0]));

		//		String s = "a=((true));";
		//		System.out.println(testAssignmentStatement(s));
		//		String s1 = "do  do x=x+1; while (x);  while (true);";
		//		System.out.println(s1.replaceAll(" +", " "));
		//		System.out.println(testDoWhileStatement(s1));
		//		String s = "x=true;";
		//		System.out.println("????"+SimpleLogic.testAssignmentStatement(s));

	}
}


//public static boolean testBlockStatement(String s){
//	if(!s.startsWith("{") || (!s.endsWith("}"))) {
//		return false;
//	}
//	String substring = s.substring(1,s.length()-1);  //cut two braces.
//	
//	if((!substring.contains("do")) && (!substring.contains("while"))) {
//		//if not contain do or while, then just zero or more assignment statement.
//		return testStatement(substring);
////		String firststatement = substring.substring(0,substring.indexOf(";")+1);
////		String reststatement = substring.substring(substring.indexOf(";")+1);
////		return (testAssignmentStatement(firststatement) && testManyStatements(reststatement)); 
//	} 
//	
//	/** If there is do-while or while statement. First check if has valid do-while statement, 
//	 * and the rest are valid assignment/while statements. */
//	if(substring.contains("do") && (substring.contains("while"))) {
//		int doindex = substring.indexOf("do");
//		int whileindex = substring.indexOf("while");
//		
//		return (testOneOrMoreAssignmentStatements(substring.substring(0, doindex)) && testDoWhileStatement(substring.substring(doindex,whileindex)) && testOneOrMoreAssignmentStatements(substring.substring(whileindex)));
//		
//	}
//	
//	//If there is no do-while, but while statement.
//	else if(!substring.contains("do") && (substring.contains("while"))) {
//		int whileindex = substring.indexOf("while");
//		int endOfWhileStatement = 20; // TODO how to get the end of while.
//	if(whileindex == 0) {
//		// while is the first statement. then check if while is valid statement and the rest if valid statements.
//		return testWhileStatement(substring.substring(0,endOfWhileStatement+1)) && testOneOrMoreAssignmentStatements(substring.substring(endOfWhileStatement));
//	}
//	// if while is not the first statement.
//	return (testStatement(substring.substring(0,whileindex+1)) || testOneOrMoreAssignmentStatements(substring.substring(0,whileindex+1))) &&
//			testWhileStatement(substring.substring(whileindex,endOfWhileStatement+1)) && (testStatement(substring.substring(endOfWhileStatement+1)) || testOneOrMoreAssignmentStatements(substring.substring(endOfWhileStatement+1)));
//	}
//	
//
//	return false;
//	
//}
//
