require 'lib/types/table/object_table'
require 'lib/lang/plan/term'
require 'lib/types/table/key'
require 'lib/types/operator/selection_op'
require 'lib/lang/plan/object_from_catalog'
class SelectionTerm < Term 	
  class SelectionTable < ObjectTable 
    include ObjectFromCatalog
    @@PRIMARY_KEY = Key.new(0,1,2)

    class Field 
      PROGRAM = 0
      RULE = 1
      POSITION = 2
      OBJECT = 3
    end
    # Program name, Rule name, Term position, Selection Object
    @@SCHEMA = [String,	String, Integer, SelectionTerm]

    def initialize
      super(TableName.new(GLOBALSCOPE, "selection"), @@PRIMARY_KEY,  TypeList.new(@@SCHEMA))
    end
  end

  def initialize(bool)
    super()
    @predicate = bool
  end

  def to_s
    return predicate.to_s
  end

  def requires
    return predicate.variables
  end

  attr_reader :predicate

  def operator(input)
    return SelectionOp.new(self, input)
  end

  def set(program, rule, position) 
    @@Program.selection.force(Tuple.new(program, rule, position, self))
  end
end
