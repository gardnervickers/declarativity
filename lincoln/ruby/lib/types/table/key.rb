class Key
  include Comparable
  include Enumerable
  
  def initialize(*attrs)
    @attributes = Array.new
    attrs.each {|a| @attributes << a}
  end
  
  def attributes
    @attributes
  end
  
  def ==(o)
    return false unless o.class == Key 
    return (to_s == o.to_s)
  end
  
  def to_s
    if @attributes.length == 0 then
      return "None"
    end
    value = String.new
    @attributes.each do |a|
      value << a.to_s + ", "
    end
    if value != "" then
      value[value.length-2] = ""
    end
    return value.rstrip
  end
  
  def size
    return @attributes.length
  end
  
  def <<(field)
    @attributes << field
  end
  
  def project(tup)
    if empty then
      return tup
    else
      values = Array.new
      @attributes.each do |a|
        values << tup.value(a)
      end
      proj = Tuple.new(*values)
#      proj.id(tup.id)
      return proj
    end
  end
  
  def empty
    return (@attributes.length == 0)
  end
  
  def each
    @attributes.each {|a| yield a}
  end
  
  def <=>(o)
    if attributes.size < o.attributes.size then
      return -1
    elsif @attributes.size > o.attributes.size then
      return 1
    else
      attributes.each_with_index do |a,i|
        value = (a <=> o.attributes[i])
        if value != 0 then
          return value
        end
      end
      return 0
    end
  end
end