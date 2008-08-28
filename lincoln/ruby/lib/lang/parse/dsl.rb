#!/usr/bin/ruby

# convert SQL-DDL-style "create table" statements to ruby ObjectTable classes with equivalent definitions.
# ie ruby dsl.rb < create.sql > schema.rb

require "rubygems"
require "treetop"
require 'tree_walker.rb'
require 'ddl.rb'

$current = Hash.new
$tables = Hash.new
$types = Hash.new
$keys = Hash.new

class Visit < TreeWalker::Handler
	def semantic(text,obj)
		$current[self.token] = text
	end
end

class VTable < Visit
	def semantic(text,obj)
		$tables[text] = Array.new
		$types[text] = Array.new
		$keys[text] = Array.new
		$position = 0
		super(text,obj)
	end
end

class VCol < Visit
	def semantic(text,obj)
		#print "current of "+$current["tablename"]+"\n"
		$tables[$current["tablename"]] << text.delete('+')
		$position += 1
		super(text,obj)
	end 
end

class VKey < Visit
  def semantic(text,obj)
    $keys[$current["tablename"]] << $position-1
    super(text,obj)
  end
end

class VType < Visit
	def semantic(text,obj)
		super(text,obj)
		$types[$current["tablename"]] << text
	end
end

class VNum < Visit
	def semantic(text,obj)
		super(text,obj)
		$keys[$current["tablename"]] << text
	end
		
end


prog = ''
while line = STDIN.gets
	prog = prog + line
end


parser = DdlParser.new
tree = parser.parse(prog)
if !tree
      puts 'failure'
     raise RuntimeError.new(parser.failure_reason)
end

sky = TreeWalker.new(tree)

v = Visit.new

sky.add_handler("tablename",VTable.new,1)
sky.add_handler("key_colname",VCol.new,1)
sky.add_handler("key_modifier",VKey.new,1)
sky.add_handler("dtype",VType.new,1)


sky.add_handler("num",VNum.new,1)

sky.walk("n")

print "require 'lib/types/table/object_table'\n"
print "require 'lib/lang/parse/compiler_mixins'\n"
# print "require 'lib/lang/plan/predicate'\n"
# print "require 'lib/lang/plan/selection_term'\n"
# print "require 'lib/lang/plan/program'\n"
if ARGV.include? "compiler" then
  print "class CompilerCatalogTable < ObjectTable\n"
  # print "  def register(obj)\n"
  # print "    defined?(@@classes) ? @@classes[obj.class.hash] = obj.class : @@classes = {obj.class.hash => obj.class}\n"
  # print "  end\n"
  print "  @@classes = Hash.new"
  print "  def CompilerCatalogTable.classes\n"
  print "    @@classes.keys\n"
  print "  end\n"
  print "end\n\n"
end

$tables.sort.each do |table, arr|
  tableCap = table[0..0].capitalize + table[1..table.length]
  mixin = tableCap+"TableMixin"
	print "class "+tableCap+"Table < "
	print (ARGV.include? "compiler") ? "CompilerCatalogTable\n" : "ObjectTable\n"
  print "include "+mixin+" if defined? "+mixin+"\n"
	if ($keys[table].size > 0) then
		print "  @@PRIMARY_KEY = Key.new("+$keys[table].join(",")+")\n"
	else
		# print "  @@PRIMARY_KEY = Key.new("+(0..arr.size-1).to_a.join(",")+")\n"
		print "  @@PRIMARY_KEY = Key.new\n"
	end

	print "  class Field\n"
	(0..arr.size-1).each do |i|
		print "    "+arr[i].upcase+"="+i.to_s+"\n"
	end
	print "  end\n"
	print "  @@SCHEMA = ["+$types[table].join(",")+"]\n"

  print "  @@classes[self] = 1" if ARGV.include? "compiler"

	print "\n  def initialize\n"
        print "    super(TableName.new(GLOBALSCOPE, \""+table+"\"), @@PRIMARY_KEY,  TypeList.new(@@SCHEMA))\n"
        print "    if defined? "+mixin+" and "+mixin+".methods.include? 'initialize_mixin'\n       then initialize_mixin \n    end\n"
  # print "    programKey = Key.new(Field::" + arr[0].upcase+")\n"
  # print "    index = HashIndex.new(self, programKey, Index::Type::SECONDARY)\n"
  # print "    @secondary[programKey] = index\n"
	print "  end\n"

  print "\n  def field(name)\n"
  print "\n    eval('Field::'+name)\n"
  print "\n  end"
  
  print "\n  def scope\n"
  print "\n    GLOBALSCOPE\n"
  print "\n  end"
  
  print "\n  def pkey\n"
  print "\n    @@PRIMARY_KEY\n"
  print "\n  end"

  print "\n  def schema\n"
  print "\n    @@SCHEMA\n"
  print "\n  end"
  
	print "\n  def schema_of\n"
	(0..arr.size-1).each do |i|
		print "    "+ arr[i]+" = Variable.new(\""+arr[i]+"\","+$types[table][i]+")\n"
		print "    "+arr[i]+".position="+i.to_s+"\n"
	end
	print "    return Schema.new(\""+tableCap+"\",["+arr.join(",")+"])\n"
	print "  end\n"
	print "end\n\n"
end
