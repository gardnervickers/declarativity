table Term (
	+termid Integer,
	ruleid Integer,
	term_pos Integer,
	term_txt String
);

table MyPredicate (
	+predicateid Integer,
	termid Integer,
	pred_pos Integer,
	pred_txt String
);

table Predicate (
  +program String,
  +rule String,
  +position Integer, 
  event String,
  object String
);

table PrimaryExpression (
	+primaryexpressionid Integer,
	termid Integer,
	p_pos Integer,
	p_txt String,
	type String,
	datatype String
);

table Expression (
	+expressionid Integer,
	termid Integer,
	expr_pos Integer,
	expr_text String
);

table MyFact (
	+factid Integer,
	programid Integer,
	tablename String
);

table Fact (
	program String, 
  tablename TableName,
	tuple Tuple
);

table Table (
  +tableid Integer,
  tablename String
);

table Column (
	+columnid Integer,
	tableid Integer,
	datatype String
);

table MyIndex (
	+indexid Integer,
	tableid Integer,
	indx_pos Integer
);


table Index (
  +tablename TableName,
  +key Key,
  type TableType,
  classname String,
  object String
);

table Program (
	+program Integer,
	owner String,
	object String
);

table Query (
	  program String,
	  rule String,
	  public Integer,
	  delete Integer,
	  event String,
	  input TableName,
	  output TableName,
	  object String
);

table Operator (
  program String,
  rule String,
  +id String,
  selectivity Float
);

table Compiler (
  +name String,
  owner String,
  file String,
  program String  
);

table Assignment (
	+program String,
	+rule String,
  position Integer
);

table Rule (
  +program String,
  +name String,
  is_public String, 
  is_delete String,
  object String
);

table Selection (
  +program String,
  +rule String,
  +position Integer,
  object String
);

table Watch (
  +program String,
  +tablename String,
  +modifier String
);
