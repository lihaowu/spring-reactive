local score=redis.call('hget','user:'..KEYS[1],'score')
local day=redis.call('hget','user:'..KEYS[1],'lastSigninDay')
if(day==KEYS[2])
    then
    return '0'
else
    redis.call('hset','user:'..KEYS[1],'score', score+1,'lastSigninDay',KEYS[2])
    return '1'
end