require 'lib/types/operator/join'
require 'lib/types/table/table'

class ScanJoin < Join 
	def initialize(predicate, input)
		super(predicate, input)
		@table = Table::table(predicate.name)
	end
	
	def to_s
		return "NEST LOOP JOIN: PREDICATE[" + @predicate  + "]"
	end
	
	def evaluate(tuples) #TupleSet
		result = TupleSet.new("tmp", nil)
		innerTuples = @table.tuples
		tuples.each do |outer| 
		  innerTuples.each do |inner|
				inner.schema = @predicate.schema.clone
				
				if validate(outer, inner)
					join = outer.join(inner)
					if (!join.nil?) 
						result << join
					end
				end
			end
		end
		return result
	end

end
