-- Redis服务端时间，多个实例共享一个固定秒窗口；只存摘要身份。
local t = redis.call('TIME')
local key = KEYS[1] .. ':' .. t[1]
local n = redis.call('INCR', key)
if n == 1 then redis.call('EXPIRE', key, 2) end
if n > tonumber(ARGV[1]) then return 0 end
return 1
