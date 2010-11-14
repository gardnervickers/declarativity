require 'rubygems'
require 'bud'

require 'lib/kvs_metered'
require 'lib/multicast'

# some combinations are simple:

# was TKV
class SingleSiteKVS < Bud
  include BudKVS
  # not necessary except to import 'tickler'
  include BestEffortMulticast
end

# was RKV
class BestEffortReplicatedKVS < Bud
  include ReplicatedKVS
  include BestEffortMulticast
end

module ReplicatedMeteredGlue
  include Anise
  annotator :declare
  # we have mixed in KVSMetering and ReplicatedKVS,
  # both of which implement indirection.  we need to 
  # compose these!
  
  # it's annoying to write this and unthinkable to imagine
  # writing up all the possible combinations of mostly
  # orthogonal components.  I'd like to see this as a rewrite,
  # though different rewrites correspond to different 
  # strategies for join order, materialization etc.
  def state
    super
    table :cs_rep, ['ident'], ['payload']
    table :cs_meter, ['ident'], ['payload']
    scratch :rmg_can_store, ['ident'], ['payload']
  end
  
  declare
  def indir
    can_store <= rmg_can_store.map {|c| c }
  end

  declare 
  def rmg_indir
    cs_rep <= rep_can_store.map {|c| c }
    cs_meter <= meter_can_store.map {|c| c }
    rmg_can_store <= join([cs_rep, cs_meter], [cs_rep.ident, cs_meter.ident]).map do |r, m|
      r 
    end
  end
end

class MeteredBestEffortReplicatedKVS < Bud
  include ReplicatedKVS
  include KVSMetering
  include BestEffortMulticast
  include ReplicatedMeteredGlue
end
