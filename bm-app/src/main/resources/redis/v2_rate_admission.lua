-- 只访问KEYS[1]，确保Redis Cluster路由键就是实际操作键。
local t = redis.call('TIME')
local window = redis.call('HGET', KEYS[1], 'window')
local n
if window ~= t[1] then
  redis.call('HSET', KEYS[1], 'window', t[1], 'count', 1)
  redis.call('EXPIRE', KEYS[1], 2)
  n = 1
else
  n = redis.call('HINCRBY', KEYS[1], 'count', 1)
end
if n > tonumber(ARGV[1]) then return 0 end
return 1
